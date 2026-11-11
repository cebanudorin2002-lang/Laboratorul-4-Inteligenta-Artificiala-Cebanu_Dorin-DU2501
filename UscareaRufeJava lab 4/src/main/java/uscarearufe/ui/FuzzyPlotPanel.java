package uscarearufe.ui;

import uscarearufe.fuzzy.LinguisticVariable;
import uscarearufe.fuzzy.MembershipFunction;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Grafic al funcțiilor de apartenență ale unei variabile (cu valoarea curentă marcată). */
public final class FuzzyPlotPanel extends JPanel {
    private static final Color[] DEFAULT_COLORS = {
            new Color(33, 150, 243), new Color(76, 175, 80), new Color(244, 67, 54)
    };

    private final LinguisticVariable variable;
    private final Color[] colors;
    private final boolean isOutput;

    private Double input;
    private double[] xs, agg;
    private Double crisp;

    public FuzzyPlotPanel(LinguisticVariable variable) {
        this(variable, false, null);
    }

    public FuzzyPlotPanel(LinguisticVariable variable, boolean isOutput, Color[] colors) {
        this.variable = variable;
        this.isOutput = isOutput;
        this.colors = colors != null ? colors : DEFAULT_COLORS;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(300, 200));
        setMinimumSize(new Dimension(200, 150));
    }

    public void setState(Double input, double[] xs, double[] aggregated, Double crisp) {
        this.input = input;
        this.xs = xs;
        this.agg = aggregated;
        this.crisp = crisp;
        repaint();
    }

    public void setState(double input) {
        setState(input, null, null, null);
    }

    private static String F(double v) {
        return String.format(Locale.US, "%.2g", v).replaceAll("\\.0+$", "");
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        final int ml = 42, mr = 14, mt = 40, mb = 34;
        float plotW = Math.max(10, getWidth() - ml - mr);
        float plotH = Math.max(10, getHeight() - mt - mb);
        float plotL = ml, plotT = mt, plotR = ml + plotW, plotB = mt + plotH;

        java.util.function.DoubleFunction<Float> X = x ->
                plotL + (float) ((x - variable.getMin()) / (variable.getMax() - variable.getMin()) * plotW);
        java.util.function.DoubleFunction<Float> Y = mu ->
                plotB - (float) (mu * plotH);

        g.setFont(getFont().deriveFont(Font.BOLD));
        g.setColor(Color.BLACK);
        String title = isOutput
                ? variable.getName() + " [" + variable.getUnit() + "] — agregare și centroid"
                : variable.getName() + " [" + variable.getUnit() + "]";
        g.drawString(title, 6, 16);

        g.setFont(getFont());
        // grid
        g.setColor(new Color(225, 225, 225));
        for (double mu : new double[]{0, 0.25, 0.5, 0.75, 1}) {
            g.drawLine(Math.round(plotL), Math.round(Y.apply(mu)), Math.round(plotR), Math.round(Y.apply(mu)));
            if (mu == 0 || mu == 0.5 || mu == 1) {
                g.setColor(Color.DARK_GRAY);
                g.drawString(F(mu), 6, Math.round(Y.apply(mu)) + 4);
                g.setColor(new Color(225, 225, 225));
            }
        }
        for (int i = 0; i <= 5; i++) {
            double xv = variable.getMin() + (variable.getMax() - variable.getMin()) * i / 5.0;
            g.drawLine(Math.round(X.apply(xv)), Math.round(plotT), Math.round(X.apply(xv)), Math.round(plotB));
            g.setColor(Color.DARK_GRAY);
            g.drawString(F(xv), Math.round(X.apply(xv)) - 10, Math.round(plotB) + 14);
            g.setColor(new Color(225, 225, 225));
        }
        g.setColor(Color.GRAY);
        g.drawRect(Math.round(plotL), Math.round(plotT), Math.round(plotW), Math.round(plotH));

        // aggregated area
        if (isOutput && xs != null && agg != null) {
            List<Integer> px = new ArrayList<>();
            List<Integer> py = new ArrayList<>();
            px.add(Math.round(X.apply(xs[0])));
            py.add(Math.round(Y.apply(0)));
            for (int i = 0; i < xs.length; i++) {
                px.add(Math.round(X.apply(xs[i])));
                py.add(Math.round(Y.apply(agg[i])));
            }
            px.add(Math.round(X.apply(xs[xs.length - 1])));
            py.add(Math.round(Y.apply(0)));
            int[] xArr = px.stream().mapToInt(Integer::intValue).toArray();
            int[] yArr = py.stream().mapToInt(Integer::intValue).toArray();
            g.setColor(new Color(255, 160, 0, 90));
            g.fillPolygon(xArr, yArr, xArr.length);
        }

        // membership curves
        int n = 240;
        List<MembershipFunction> terms = variable.getTerms();
        for (int k = 0; k < terms.size(); k++) {
            Color col = colors[k % colors.length];
            int[] xPts = new int[n + 1];
            int[] yPts = new int[n + 1];
            for (int i = 0; i <= n; i++) {
                double xv = variable.getMin() + (variable.getMax() - variable.getMin()) * i / (double) n;
                xPts[i] = Math.round(X.apply(xv));
                yPts[i] = Math.round(Y.apply(terms.get(k).evaluate(xv)));
            }
            g.setColor(col);
            g.setStroke(new BasicStroke(2.2f));
            g.drawPolyline(xPts, yPts, n + 1);

            float lx = plotL + 6 + k * Math.max(70, plotW / 3.2f);
            g.fillRect(Math.round(lx), 24, 12, 8);
            g.setColor(Color.BLACK);
            g.setFont(getFont());
            g.drawString(terms.get(k).getName(), Math.round(lx) + 15, 32);
        }

        // input marker
        if (!isOutput && input != null) {
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10, new float[]{4, 4}, 0));
            float xi = X.apply(input);
            g.drawLine(Math.round(xi), Math.round(plotT), Math.round(xi), Math.round(plotB));
            for (int k = 0; k < terms.size(); k++) {
                double mu = terms.get(k).evaluate(input);
                if (mu <= 0) continue;
                Color col = colors[k % colors.length];
                g.setColor(col);
                g.fill(new Ellipse2D.Float(xi - 4, Y.apply(mu) - 4, 8, 8));
                g.drawString(String.format(Locale.US, "μ=%.2f", mu), Math.round(xi) + 6, Math.round(Y.apply(mu)) - 8);
            }
            g.setColor(Color.BLACK);
            g.drawString("x = " + F(input), Math.min(Math.round(xi) + 4, Math.round(plotR) - 55), Math.round(plotT) + 12);
        }

        // centroid
        if (isOutput && crisp != null && !Double.isNaN(crisp)) {
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10, new float[]{5, 4}, 0));
            float xc = X.apply(crisp);
            g.drawLine(Math.round(xc), Math.round(plotT), Math.round(xc), Math.round(plotB));
            g.setFont(getFont().deriveFont(Font.BOLD));
            g.drawString(String.format(Locale.US, "centroid = %.2f h", crisp),
                    Math.min(Math.round(xc) + 4, Math.round(plotR) - 105), Math.round(plotT) + 12);
        }
    }
}
