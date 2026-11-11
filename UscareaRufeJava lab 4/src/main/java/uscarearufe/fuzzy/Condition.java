package uscarearufe.fuzzy;

public final class Condition {
    public final LinguisticVariable variable;
    public final String term;

    public Condition(LinguisticVariable variable, String term) {
        this.variable = variable;
        this.term = term;
    }
}
