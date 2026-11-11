package uscarearufe.ui;

import uscarearufe.fuzzy.*;
import uscarearufe.model.DryingModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MainForm extends JFrame {
    private final MamdaniEngine engine = DryingModel.createEngine();

    private final JTextField txtT = new JTextField();
    private final JTextField txtH = new JTextField();
    private final JTextField txtV = new JTextField();
    private final JLabel lblStatus = new JLabel(" ");
    private final JLabel lblTimp = new JLabel("—");
    private final JLabel lblInterp = new JLabel(" ");
    private final JTextArea txtRecom = new JTextArea();
    private final DefaultTableModel rulesModel;
    private final JTable tblRules;
    private final JComboBox<String> cmbPair = new JComboBox<>(new String[]{
            "Temperatură – Umiditate", "Temperatură – Vânt", "Umiditate – Vânt"
    });
    private final Surface3DPanel surface = new Surface3DPanel();

    private final FuzzyPlotPanel plotT = new FuzzyPlotPanel(DryingModel.TEMPERATURA);
    private final FuzzyPlotPanel plotH = new FuzzyPlotPanel(DryingModel.UMIDITATE);
    private final FuzzyPlotPanel plotV = new FuzzyPlotPanel(DryingModel.VANT);
    private final FuzzyPlotPanel plotOut = new FuzzyPlotPanel(DryingModel.TIMP, true,
            new Color[]{new Color(76, 175, 80), new Color(255, 152, 0), new Color(244, 67, 54)});

    private double tVal = 20, hVal = 50, vVal = 3;
    private Double lastHours;

    public MainForm() {
        super("Prognoza timpului de uscare a rufelor — sistem fuzzy Mamdani");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);

        rulesModel = new DefaultTableModel(new String[]{"Nr.", "Regula", "Grad de activare"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (FuzzyRule r : DryingModel.RULES)
            rulesModel.addRow(new Object[]{r.number, r.getText(), "—"});
        tblRules = new JTable(rulesModel);
        tblRules.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblRules.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblRules.getColumnModel().getColumn(1).setPreferredWidth(720);
        tblRules.getColumnModel().getColumn(2).setPreferredWidth(120);

        buildLayout();
        resetInputs();
    }

    private void buildLayout() {
        JTabbedPane tabs = new JTabbedPane();
        JPanel left = new JPanel(new BorderLayout(8, 8));
        left.setPreferredSize(new Dimension(400, 0));
        left.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Result
        JPanel gbRes = new JPanel(new BorderLayout(4, 4));
        gbRes.setBorder(new TitledBorder("Rezultat"));
        lblTimp.setFont(lblTimp.getFont().deriveFont(Font.BOLD, 22f));
        lblTimp.setForeground(new Color(25, 90, 160));
        lblInterp.setVerticalAlignment(SwingConstants.TOP);
        txtRecom.setEditable(false);
        txtRecom.setLineWrap(true);
        txtRecom.setWrapStyleWord(true);
        txtRecom.setBackground(Color.WHITE);
        JLabel lblRec = new JLabel("Recomandări:");
        JPanel northRes = new JPanel();
        northRes.setLayout(new BoxLayout(northRes, BoxLayout.Y_AXIS));
        northRes.add(lblTimp);
        northRes.add(Box.createVerticalStrut(6));
        northRes.add(lblInterp);
        northRes.add(Box.createVerticalStrut(6));
        northRes.add(lblRec);
        gbRes.add(northRes, BorderLayout.NORTH);
        gbRes.add(new JScrollPane(txtRecom), BorderLayout.CENTER);

        lblStatus.setForeground(new Color(178, 34, 34));
        lblStatus.setVerticalAlignment(SwingConstants.TOP);

        // Inputs
        JPanel gbIn = new JPanel(null);
        gbIn.setBorder(new TitledBorder("Date de intrare"));
        gbIn.setPreferredSize(new Dimension(380, 210));
        addInputRow(gbIn, 30, "Temperatură [°C]:", txtT, DryingModel.TEMPERATURA);
        addInputRow(gbIn, 68, "Umiditate [%]:", txtH, DryingModel.UMIDITATE);
        addInputRow(gbIn, 106, "Viteza vântului [m/s]:", txtV, DryingModel.VANT);
        JButton btnCalc = new JButton("Calculează");
        btnCalc.setBounds(15, 150, 160, 36);
        JButton btnReset = new JButton("Resetare");
        btnReset.setBounds(190, 150, 160, 36);
        btnCalc.addActionListener(e -> calculate());
        btnReset.addActionListener(e -> resetInputs());
        gbIn.add(btnCalc);
        gbIn.add(btnReset);
        getRootPane().setDefaultButton(btnCalc);

        left.add(gbIn, BorderLayout.NORTH);
        left.add(lblStatus, BorderLayout.CENTER);
        left.add(gbRes, BorderLayout.SOUTH);
        // fix: status between input and result
        JPanel leftCenter = new JPanel(new BorderLayout());
        leftCenter.add(lblStatus, BorderLayout.NORTH);
        leftCenter.add(gbRes, BorderLayout.CENTER);
        left.removeAll();
        left.setLayout(new BorderLayout(8, 8));
        left.add(gbIn, BorderLayout.NORTH);
        left.add(leftCenter, BorderLayout.CENTER);

        // Tab 1: membership
        JPanel tabMf = new JPanel(new GridLayout(2, 2, 6, 6));
        tabMf.setBorder(new EmptyBorder(6, 6, 6, 6));
        tabMf.add(plotT);
        tabMf.add(plotH);
        tabMf.add(plotV);
        tabMf.add(plotOut);
        tabs.addTab("Funcții de apartenență", tabMf);

        // Tab 2: 3D
        JPanel tab3d = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cmbPair.setSelectedIndex(0);
        cmbPair.addActionListener(e -> buildSurface());
        JButton btnSurf = new JButton("Construiește suprafața");
        btnSurf.addActionListener(e -> buildSurface());
        top.add(cmbPair);
        top.add(btnSurf);
        tab3d.add(top, BorderLayout.NORTH);
        tab3d.add(surface, BorderLayout.CENTER);
        tabs.addTab("Suprafețe 3D", tab3d);
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 1) buildSurface();
        });

        // Tab 3: rules
        tabs.addTab("Baza de reguli", new JScrollPane(tblRules));

        setLayout(new BorderLayout());
        add(left, BorderLayout.WEST);
        add(tabs, BorderLayout.CENTER);
    }

    private void addInputRow(JPanel gb, int y, String label, JTextField tb, LinguisticVariable v) {
        JLabel lb = new JLabel(label);
        lb.setBounds(15, y + 4, 150, 20);
        tb.setBounds(170, y, 80, 24);
        JLabel range = new JLabel(String.format(Locale.US, "(%.0f … %.0f)", v.getMin(), v.getMax()));
        range.setForeground(Color.DARK_GRAY);
        range.setBounds(260, y + 4, 110, 20);
        gb.add(lb);
        gb.add(tb);
        gb.add(range);
    }

    private void resetInputs() {
        txtT.setText("20");
        txtH.setText("50");
        txtV.setText("3");
        lblStatus.setText(" ");
        calculate();
    }

    private boolean tryParseField(JTextField tb, LinguisticVariable v, List<String> msgs, double[] out) {
        String text = tb.getText().trim().replace(',', '.');
        String msg = null;
        if (text.isEmpty())
            msg = "Câmpul „" + v.getName() + "” este necompletat.";
        else {
            try {
                double value = Double.parseDouble(text);
                if (Double.isNaN(value) || Double.isInfinite(value))
                    msg = "„" + v.getName() + "”: valoarea introdusă nu este un număr valid.";
                else if (value < v.getMin() || value > v.getMax())
                    msg = String.format(Locale.US, "„%s” trebuie să fie în intervalul [%.0f; %.0f] %s.",
                            v.getName(), v.getMin(), v.getMax(), v.getUnit());
                else
                    out[0] = value;
            } catch (NumberFormatException e) {
                msg = "„" + v.getName() + "”: valoarea introdusă nu este un număr valid.";
            }
        }
        if (msg != null) msgs.add(msg);
        return msg == null;
    }

    private void calculate() {
        List<String> msgs = new ArrayList<>();
        double[] tArr = new double[1], hArr = new double[1], vArr = new double[1];
        boolean ok = tryParseField(txtT, DryingModel.TEMPERATURA, msgs, tArr);
        ok &= tryParseField(txtH, DryingModel.UMIDITATE, msgs, hArr);
        ok &= tryParseField(txtV, DryingModel.VANT, msgs, vArr);

        if (!ok) {
            lblStatus.setText("<html>Date incorecte sau incomplete:<br>• " +
                    String.join("<br>• ", msgs) + "</html>");
            return;
        }
        lblStatus.setText(" ");
        tVal = tArr[0];
        hVal = hArr[0];
        vVal = vArr[0];

        Map<String, Double> input = new HashMap<>();
        input.put(DryingModel.TEMPERATURA.getName(), tVal);
        input.put(DryingModel.UMIDITATE.getName(), hVal);
        input.put(DryingModel.VANT.getName(), vVal);
        InferenceResult result = engine.infer(input);

        for (RuleActivation a : result.activations) {
            int row = a.rule.number - 1;
            rulesModel.setValueAt(String.format(Locale.US, "%.3f", a.strength), row, 2);
        }

        plotT.setState(tVal);
        plotH.setState(hVal);
        plotV.setState(vVal);

        if (!result.hasOutput) {
            lastHours = null;
            lblTimp.setText("—");
            lblInterp.setText("<html>Nicio regulă nu s-a activat pentru aceste valori; rezultatul nu poate fi calculat.</html>");
            txtRecom.setText("");
            plotOut.setState(null, result.xs, result.aggregated, null);
            return;
        }

        double hours = result.crisp;
        lastHours = hours;
        lblTimp.setText(String.format(Locale.US, "%.2f ore", hours));
        lblInterp.setText("<html>≈ " + DryingModel.formatDuration(hours) + "<br>" +
                DryingModel.interpret(hours, result) + "</html>");
        StringBuilder sb = new StringBuilder();
        for (String s : DryingModel.recommend(result, hours))
            sb.append("• ").append(s).append("\n\n");
        txtRecom.setText(sb.toString().trim());
        plotOut.setState(null, result.xs, result.aggregated, hours);

        buildSurface();
    }

    private void buildSurface() {
        final int N = 21;
        LinguisticVariable vx, vy;
        double fixedVal;
        String fixedName;
        double mx, my;
        int pair = cmbPair.getSelectedIndex() < 0 ? 0 : cmbPair.getSelectedIndex();
        switch (pair) {
            case 0:
                vx = DryingModel.TEMPERATURA;
                vy = DryingModel.UMIDITATE;
                fixedName = DryingModel.VANT.getName();
                fixedVal = vVal;
                mx = tVal;
                my = hVal;
                break;
            case 1:
                vx = DryingModel.TEMPERATURA;
                vy = DryingModel.VANT;
                fixedName = DryingModel.UMIDITATE.getName();
                fixedVal = hVal;
                mx = tVal;
                my = vVal;
                break;
            default:
                vx = DryingModel.UMIDITATE;
                vy = DryingModel.VANT;
                fixedName = DryingModel.TEMPERATURA.getName();
                fixedVal = tVal;
                mx = hVal;
                my = vVal;
                break;
        }

        double[][] z = new double[N][N];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                Map<String, Double> inp = new HashMap<>();
                inp.put(DryingModel.TEMPERATURA.getName(), tVal);
                inp.put(DryingModel.UMIDITATE.getName(), hVal);
                inp.put(DryingModel.VANT.getName(), vVal);
                inp.put(fixedName, fixedVal);
                inp.put(vx.getName(), vx.getMin() + (vx.getMax() - vx.getMin()) * i / (N - 1.0));
                inp.put(vy.getName(), vy.getMin() + (vy.getMax() - vy.getMin()) * j / (N - 1.0));
                InferenceResult r = engine.infer(inp);
                z[i][j] = r.hasOutput ? r.crisp : DryingModel.TIMP.getMax() / 2;
            }
        }

        String unit = fixedName.equals(DryingModel.VANT.getName()) ? "m/s"
                : fixedName.equals(DryingModel.UMIDITATE.getName()) ? "%" : "°C";
        String info = String.format(Locale.US,
                "Timp de uscare = f(%s, %s);  %s fix = %.2f %s",
                vx.getName(), vy.getName(), fixedName, fixedVal, unit);
        surface.setData(vx, vy, z, info, mx, my, lastHours);
    }
}
