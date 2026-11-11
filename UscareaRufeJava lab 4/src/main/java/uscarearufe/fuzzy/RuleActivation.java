package uscarearufe.fuzzy;

public final class RuleActivation {
    public final FuzzyRule rule;
    public final double strength;

    public RuleActivation(FuzzyRule rule, double strength) {
        this.rule = rule;
        this.strength = strength;
    }
}
