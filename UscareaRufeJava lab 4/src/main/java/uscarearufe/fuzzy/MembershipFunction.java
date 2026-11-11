package uscarearufe.fuzzy;

/**
 * Funcție de apartenență trapezoidală (a, b, c, d). Cazul triunghiular este b == c.
 * Cazurile „umăr” (a == b sau c == d) sunt tratate corect.
 */
public final class MembershipFunction {
    private final double a, b, c, d;
    private final String name;

    public MembershipFunction(String name, double a, double b, double c, double d) {
        if (!(a <= b && b <= c && c <= d))
            throw new IllegalArgumentException(
                    "Parametri invalizi pentru termenul „" + name + "”: trebuie a ≤ b ≤ c ≤ d.");
        this.name = name;
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    public static MembershipFunction triunghi(String name, double a, double b, double c) {
        return new MembershipFunction(name, a, b, b, c);
    }

    public static MembershipFunction trapez(String name, double a, double b, double c, double d) {
        return new MembershipFunction(name, a, b, c, d);
    }

    public String getName() { return name; }

    /** Gradul de apartenență μ(x) ∈ [0, 1]. */
    public double evaluate(double x) {
        if (x < a || x > d) return 0.0;
        if (x >= b && x <= c) return 1.0;
        if (x < b) return (x - a) / (b - a);
        return (d - x) / (d - c);
    }
}
