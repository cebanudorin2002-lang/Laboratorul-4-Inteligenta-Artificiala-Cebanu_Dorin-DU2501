package uscarearufe.fuzzy;

import java.util.List;
import java.util.Map;

public final class InferenceResult {
    public final Map<String, Map<String, Double>> fuzzified;
    public final List<RuleActivation> activations;
    public final Map<String, Double> outputClip;
    public final double[] xs;
    public final double[] aggregated;
    public final boolean hasOutput;
    public final double crisp;

    public InferenceResult(Map<String, Map<String, Double>> fuzzified,
                           List<RuleActivation> activations,
                           Map<String, Double> outputClip,
                           double[] xs, double[] aggregated,
                           boolean hasOutput, double crisp) {
        this.fuzzified = fuzzified;
        this.activations = activations;
        this.outputClip = outputClip;
        this.xs = xs;
        this.aggregated = aggregated;
        this.hasOutput = hasOutput;
        this.crisp = crisp;
    }
}
