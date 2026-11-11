package uscarearufe.fuzzy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Motor de inferență Mamdani:
 * 1) fuzificare, 2) evaluarea regulilor (AND = min, OR = max, × pondere),
 * 3) implicație (tăiere min), 4) agregare (max), 5) defuzificare centroid.
 */
public final class MamdaniEngine {
    private final List<LinguisticVariable> inputs;
    private final List<FuzzyRule> rules;
    private final LinguisticVariable output;
    private final double[] xs;
    private final Map<String, double[]> termCurves = new HashMap<>();

    public MamdaniEngine(List<LinguisticVariable> inputs, List<FuzzyRule> rules,
                         LinguisticVariable output, int resolution) {
        this.inputs = inputs;
        this.rules = rules;
        this.output = output;
        xs = new double[resolution];
        for (int i = 0; i < resolution; i++)
            xs[i] = output.getMin() + (output.getMax() - output.getMin()) * i / (resolution - 1.0);
        for (MembershipFunction t : output.getTerms()) {
            double[] curve = new double[resolution];
            for (int i = 0; i < resolution; i++)
                curve[i] = t.evaluate(xs[i]);
            termCurves.put(t.getName(), curve);
        }
    }

    public InferenceResult infer(Map<String, Double> crisp) {
        Map<String, Map<String, Double>> fuzz = new LinkedHashMap<>();
        for (LinguisticVariable v : inputs) {
            Double x = crisp.get(v.getName());
            if (x == null || Double.isNaN(x) || Double.isInfinite(x))
                throw new IllegalArgumentException("Valoare lipsă sau invalidă pentru „" + v.getName() + "”.");
            double clamped = Math.max(v.getMin(), Math.min(v.getMax(), x));
            fuzz.put(v.getName(), v.fuzzify(clamped));
        }

        List<RuleActivation> activations = new ArrayList<>();
        Map<String, Double> clip = new LinkedHashMap<>();
        for (MembershipFunction t : output.getTerms())
            clip.put(t.getName(), 0.0);

        for (FuzzyRule r : rules) {
            List<Double> mus = new ArrayList<>();
            for (Condition c : r.conditions)
                mus.add(fuzz.get(c.variable.getName()).get(c.term));
            double s = r.operator == LogicOp.AND
                    ? mus.stream().mapToDouble(Double::doubleValue).min().orElse(0)
                    : mus.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            s *= r.weight;
            activations.add(new RuleActivation(r, s));
            if (s > clip.get(r.outputTerm))
                clip.put(r.outputTerm, s);
        }

        double[] agg = new double[xs.length];
        for (MembershipFunction t : output.getTerms()) {
            double c = clip.get(t.getName());
            if (c <= 0) continue;
            double[] curve = termCurves.get(t.getName());
            for (int i = 0; i < agg.length; i++)
                agg[i] = Math.max(agg[i], Math.min(c, curve[i]));
        }

        double num = 0, den = 0;
        for (int i = 0; i < agg.length; i++) {
            num += xs[i] * agg[i];
            den += agg[i];
        }
        boolean has = den > 1e-9;

        return new InferenceResult(
                fuzz,
                Collections.unmodifiableList(activations),
                clip,
                xs, agg,
                has,
                has ? num / den : Double.NaN);
    }
}
