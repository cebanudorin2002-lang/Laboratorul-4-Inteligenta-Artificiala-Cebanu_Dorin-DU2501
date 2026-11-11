package uscarearufe.fuzzy;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Variabilă lingvistică: universul de discurs + termenii (mulțimile fuzzy). */
public final class LinguisticVariable {
    private final String name;
    private final String unit;
    private final double min;
    private final double max;
    private final List<MembershipFunction> terms;

    public LinguisticVariable(String name, String unit, double min, double max, MembershipFunction... terms) {
        this.name = name;
        this.unit = unit;
        this.min = min;
        this.max = max;
        this.terms = Collections.unmodifiableList(Arrays.asList(terms));
    }

    public String getName() { return name; }
    public String getUnit() { return unit; }
    public double getMin() { return min; }
    public double getMax() { return max; }
    public List<MembershipFunction> getTerms() { return terms; }

    public MembershipFunction getTerm(String termName) {
        for (MembershipFunction t : terms)
            if (t.getName().equals(termName)) return t;
        throw new IllegalArgumentException("Termenul „" + termName + "” nu există în variabila „" + name + "”.");
    }

    /** Fuzificare: gradul de apartenență al valorii x la fiecare termen. */
    public Map<String, Double> fuzzify(double x) {
        Map<String, Double> m = new LinkedHashMap<>();
        for (MembershipFunction t : terms)
            m.put(t.getName(), t.evaluate(x));
        return m;
    }
}
