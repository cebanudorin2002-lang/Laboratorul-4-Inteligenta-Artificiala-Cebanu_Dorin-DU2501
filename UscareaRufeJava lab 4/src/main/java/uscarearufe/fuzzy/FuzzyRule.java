package uscarearufe.fuzzy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Regulă: DACĂ (c1 AND/OR c2 ...) ATUNCI ieșire = termen [pondere]. */
public final class FuzzyRule {
    public final int number;
    public final List<Condition> conditions;
    public final LogicOp operator;
    public final LinguisticVariable outputVariable;
    public final String outputTerm;
    public final double weight;

    public FuzzyRule(int number, LogicOp op, LinguisticVariable outVar, String outTerm, double weight,
                     Condition... conditions) {
        this.number = number;
        this.operator = op;
        this.outputVariable = outVar;
        this.outputTerm = outTerm;
        this.weight = weight;
        this.conditions = Collections.unmodifiableList(Arrays.asList(conditions));
    }

    public String getText() {
        String op = operator == LogicOp.AND ? " ȘI " : " SAU ";
        String cond = conditions.stream()
                .map(c -> c.variable.getName().toLowerCase(Locale.ROOT) + " este " + c.term)
                .collect(Collectors.joining(op));
        String w = weight < 1.0 ? String.format(Locale.US, "  [pondere %.2g]", weight) : "";
        return "DACĂ " + cond + " ATUNCI " + outputVariable.getName().toLowerCase(Locale.ROOT)
                + " este " + outputTerm + w;
    }
}
