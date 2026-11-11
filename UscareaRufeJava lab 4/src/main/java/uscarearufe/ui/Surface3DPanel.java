package uscarearufe.ui;

import uscarearufe.fuzzy.LinguisticVariable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Suprafață 3D desenată cu Java2D (algoritmul pictorului), rotabilă cu mouse-ul. */
public final class Surface3DPanel extends JPanel {
    private double[][] z;
    private LinguisticVariable vx, vy;
    private String info = "";
    private Double mx, my, mz;
    private double az = -40 * Math.PI / 180;
    private double el = 30 * Math.PI / 180;
    private Point drag;
    private static final double Z_MAX = 12.0;

    public Surface3DPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(600, 500));

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { drag = e.getPoint(); }

            @Override
            public void mouseReleased(MouseEvent e) { drag = null; }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (drag != null && SwingUtilities.isLeftMouseButton(e)) {
                    az += (e.getX() - drag.x) * 0.01;
                    el = Math.max(5 * Math.PI / 180, Math.min(85 * Math.PI / 180,
                            el + (e.getY() - drag.y) * 0.01));
                    drag = e.getPoint();
                    repaint();
                }
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    public void setData(LinguisticVariable vx, LinguisticVariable vy, double[][] z, String info,
                        Double markerX, Double markerY, Double markerZ) {
        this.vx = vx;
        this.vy = vy;
        this.z = z;
        this.info = info;
        this.mx = markerX;
        this.my = markerY;
        this.mz = markerZ;
        repaint();
    }

    private static Color gradient(double t) {
        t = Math.max(0, Math.min(1, t));
        Color a = new Color(46, 160, 67), b = new Color(245, 205, 50), c = new Color(205, 50, 50);
        Color from = t < 0.5 ? a : b, to = t < 0.5 ? b : c;
        double u = t < 0.5 ? t * 2 : (t - 0.5) * 2;
        return new Color(
                (int) (from.getRed() + (to.getRed() - from.getRed()) * u),
                (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * u),
                (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * u));
    }

    private static final class Proj {
        final Point2D.Float p;
        final double depth;
        Proj(Point2D.Float p, double depth) { this.p = p; this.depth = depth; }
    }

    private Proj project(double nx, double ny, double nz) {
        double xr = nx * Math.cos(az) - ny * Math.sin(az);
        double yr = nx * Math.sin(az) + ny * Math.cos(az);
        double zz = (nz - 0.5) * 1.2;
        double u = xr;
        double v = zz * Math.cos(el) + yr * Math.sin(el);
        double scale = Math.min(getWidth() - 120, getHeight()) * 0.33;
        float sx = (float) (getWidth() / 2.0 - 40 + scale * u);
        float sy = (float) (getHeight() / 2.0 + 15 - scale * v);
        return new Proj(new Point2D.Float(sx, sy), yr);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        if (z == null || vx == null || vy == null) {
            g.setColor(Color.DARK_GRAY);
            g.drawString("Apăsați «Construiește suprafața» pentru a genera graficul 3D.", 20, 20);
            return;
        }

        int n = z.length, m = z[0].length;
        java.util.function.IntFunction<Double> Nx = i -> -1 + 2.0 * i / (n - 1);
        java.util.function.IntFunction<Double> Ny = j -> -1 + 2.0 * j / (m - 1);

        // floor
        Point2D.Float[] floor = {
                project(-1, -1, 0).p, project(1, -1, 0).p, project(1, 1, 0).p, project(-1, 1, 0).p
        };
        int[] fx = {(int) floor[0].x, (int) floor[1].x, (int) floor[2].x, (int) floor[3].x};
        int[] fy = {(int) floor[0].y, (int) floor[1].y, (int) floor[2].y, (int) floor[3].y};
        g.setColor(new Color(240, 240, 240));
        g.fillPolygon(fx, fy, 4);
        g.setColor(Color.GRAY);
        g.drawPolygon(fx, fy, 4);

        class Quad {
            double depth;
            int[] px, py;
            Color col;
        }
        List<Quad> quads = new ArrayList<>();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < m - 1; j++) {
                int[][] c = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                int[] px = new int[4], py = new int[4];
                double dsum = 0, zsum = 0;
                for (int k = 0; k < 4; k++) {
                    int ci = c[k][0], cj = c[k][1];
                    double zv = z[ci][cj];
                    Proj pr = project(Nx.apply(ci), Ny.apply(cj), zv / Z_MAX);
                    px[k] = Math.round(pr.p.x);
                    py[k] = Math.round(pr.p.y);
                    dsum += pr.depth;
                    zsum += zv;
                }
                Quad q = new Quad();
                q.depth = dsum / 4;
                q.px = px;
                q.py = py;
                q.col = gradient(zsum / 4 / Z_MAX);
                quads.add(q);
            }
        }
        quads.sort(Comparator.comparingDouble((Quad q) -> q.depth).reversed());
        for (Quad q : quads) {
            g.setColor(q.col);
            g.fillPolygon(q.px, q.py, 4);
            g.setColor(new Color(60, 60, 60, 70));
            g.setStroke(new BasicStroke(0.6f));
            g.drawPolygon(q.px, q.py, 4);
        }

        // marker
        if (mx != null && my != null && mz != null && !Double.isNaN(mz)) {
            double nx = (mx - vx.getMin()) / (vx.getMax() - vx.getMin()) * 2 - 1;
            double ny = (my - vy.getMin()) / (vy.getMax() - vy.getMin()) * 2 - 1;
            Proj p = project(nx, ny, mz / Z_MAX);
            Proj p0 = project(nx, ny, 0);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10, new float[]{2, 3}, 0));
            g.drawLine(Math.round(p0.p.x), Math.round(p0.p.y), Math.round(p.p.x), Math.round(p.p.y));
            g.fillOval(Math.round(p.p.x) - 6, Math.round(p.p.y) - 6, 12, 12);
            g.setColor(Color.WHITE);
            g.fillOval(Math.round(p.p.x) - 3, Math.round(p.p.y) - 3, 6, 6);
        }

        // axis labels
        g.setColor(Color.BLACK);
        g.setFont(getFont());
        
        g.drawString(String.format(Locale.US, "%.1f", vx.getMin()),
                (int) project(-1, -1.12, 0).p.x, (int) project(-1, -1.12, 0).p.y);
        g.drawString(String.format(Locale.US, "%.1f", vx.getMax()) + " " + vx.getUnit(),
                (int) project(1, -1.12, 0).p.x, (int) project(1, -1.12, 0).p.y);
        g.drawString(vx.getName() + " →",
                (int) project(0, -1.2, 0).p.x, (int) project(0, -1.2, 0).p.y);
        g.drawString(String.format(Locale.US, "%.1f", vy.getMax()) + " " + vy.getUnit(),
                (int) project(1.12, 1, 0).p.x, (int) project(1.12, 1, 0).p.y);
        g.drawString(vy.getName() + " →",
                (int) project(1.18, 0, 0).p.x, (int) project(1.18, 0, 0).p.y);
        g.drawString("Timp [h]: 12",
                (int) project(-1.05, -1, 1).p.x, (int) project(-1.05, -1, 1).p.y);

        // color legend
        int lx = getWidth() - 70, ly = 50, lh = Math.max(60, getHeight() - 130);
        for (int i = 0; i < lh; i++) {
            g.setColor(gradient(1.0 - (double) i / lh));
            g.drawLine(lx, ly + i, lx + 18, ly + i);
        }
        g.setColor(Color.GRAY);
        g.drawRect(lx, ly, 18, lh);
        g.setColor(Color.BLACK);
        g.drawString("12 h", lx + 22, ly + 6);
        g.drawString("0 h", lx + 22, ly + lh);

        g.drawString(info, 8, 16);
        g.setColor(Color.DARK_GRAY);
        g.drawString("Trageți cu mouse-ul pentru a roti graficul.", 8, getHeight() - 10);
    }
}
