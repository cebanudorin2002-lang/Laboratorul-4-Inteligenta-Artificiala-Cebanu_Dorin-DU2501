package uscarearufe.model;

import uscarearufe.fuzzy.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Modelul fuzzy pentru estimarea timpului de uscare a rufelor (Varianta 3). */
public final class DryingModel {
    private DryingModel() {}

    public static final LinguisticVariable TEMPERATURA = new LinguisticVariable(
            "Temperatură", "°C", 0, 40,
            MembershipFunction.trapez("scăzută", 0, 0, 8, 16),
            MembershipFunction.triunghi("medie", 10, 20, 30),
            MembershipFunction.trapez("ridicată", 24, 32, 40, 40));

    public static final LinguisticVariable UMIDITATE = new LinguisticVariable(
            "Umiditate", "%", 0, 100,
            MembershipFunction.trapez("scăzută", 0, 0, 25, 45),
            MembershipFunction.triunghi("medie", 30, 50, 70),
            MembershipFunction.trapez("ridicată", 55, 75, 100, 100));

    public static final LinguisticVariable VANT = new LinguisticVariable(
            "Vânt", "m/s", 0, 12,
            MembershipFunction.trapez("slab", 0, 0, 1, 3.5),
            MembershipFunction.triunghi("moderat", 2, 4.5, 7),
            MembershipFunction.trapez("puternic", 5.5, 8, 12, 12));

    public static final LinguisticVariable TIMP = new LinguisticVariable(
            "Timp de uscare", "ore", 0, 12,
            MembershipFunction.trapez("scurt", 0, 0, 2, 4),
            MembershipFunction.triunghi("mediu", 3, 6, 9),
            MembershipFunction.trapez("lung", 8, 10, 12, 12));

    public static final List<LinguisticVariable> INPUTS =
            Collections.unmodifiableList(Arrays.asList(TEMPERATURA, UMIDITATE, VANT));

    public static final List<FuzzyRule> RULES = buildRules();

    private static List<FuzzyRule> buildRules() {
        List<FuzzyRule> list = new ArrayList<>();
        list.add(new FuzzyRule(1,  LogicOp.AND, TIMP, "scurt",  1.0, new Condition(TEMPERATURA, "ridicată"), new Condition(UMIDITATE, "scăzută")));
        list.add(new FuzzyRule(2,  LogicOp.AND, TIMP, "lung",   1.0, new Condition(TEMPERATURA, "scăzută"),  new Condition(UMIDITATE, "ridicată")));
        list.add(new FuzzyRule(3,  LogicOp.AND, TIMP, "scurt",  1.0, new Condition(VANT, "puternic"), new Condition(UMIDITATE, "scăzută")));
        list.add(new FuzzyRule(4,  LogicOp.AND, TIMP, "mediu",  1.0, new Condition(TEMPERATURA, "medie"),    new Condition(UMIDITATE, "medie")));
        list.add(new FuzzyRule(5,  LogicOp.AND, TIMP, "scurt",  1.0, new Condition(TEMPERATURA, "ridicată"), new Condition(VANT, "puternic")));
        list.add(new FuzzyRule(6,  LogicOp.AND, TIMP, "lung",   1.0, new Condition(TEMPERATURA, "scăzută"),  new Condition(VANT, "slab")));
        list.add(new FuzzyRule(7,  LogicOp.AND, TIMP, "lung",   1.0, new Condition(UMIDITATE, "ridicată"), new Condition(VANT, "slab")));
        list.add(new FuzzyRule(8,  LogicOp.AND, TIMP, "mediu",  1.0, new Condition(TEMPERATURA, "medie"),    new Condition(VANT, "moderat")));
        list.add(new FuzzyRule(9,  LogicOp.AND, TIMP, "mediu",  1.0, new Condition(TEMPERATURA, "ridicată"), new Condition(UMIDITATE, "ridicată")));
        list.add(new FuzzyRule(10, LogicOp.OR,  TIMP, "lung",   0.4, new Condition(TEMPERATURA, "scăzută"),  new Condition(UMIDITATE, "ridicată")));
        list.add(new FuzzyRule(11, LogicOp.AND, TIMP, "scurt",  0.7, new Condition(TEMPERATURA, "medie"),    new Condition(UMIDITATE, "scăzută")));
        list.add(new FuzzyRule(12, LogicOp.AND, TIMP, "scurt",  1.0, new Condition(TEMPERATURA, "ridicată"), new Condition(VANT, "moderat")));
        list.add(new FuzzyRule(13, LogicOp.AND, TIMP, "mediu",  1.0, new Condition(UMIDITATE, "medie"),    new Condition(VANT, "slab")));
        return Collections.unmodifiableList(list);
    }

    public static MamdaniEngine createEngine() {
        return createEngine(601);
    }

    public static MamdaniEngine createEngine(int resolution) {
        return new MamdaniEngine(INPUTS, RULES, TIMP, resolution);
    }

    public static String formatDuration(double hours) {
        int totalMin = (int) Math.round(hours * 60);
        return (totalMin / 60) + " h " + String.format(Locale.US, "%02d", totalMin % 60) + " min";
    }

    public static String interpret(double h, InferenceResult r) {
        String level;
        if (h < 3.0) level = "foarte rapidă";
        else if (h < 5.0) level = "rapidă";
        else if (h < 7.0) level = "medie";
        else if (h < 9.0) level = "lentă";
        else level = "foarte lentă / îndelungată";

        Map.Entry<String, Double> dom = r.outputClip.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow();
        return "Uscare " + level + ": aproximativ " + formatDuration(h) + ". " +
                "Termenul dominant al ieșirii este „" + dom.getKey() +
                "” (grad de activare " + String.format(Locale.US, "%.2f", dom.getValue()) + ").";
    }

    public static List<String> recommend(InferenceResult r, double hours) {
        List<String> rec = new ArrayList<>();
        Map<String, Double> t = r.fuzzified.get(TEMPERATURA.getName());
        Map<String, Double> u = r.fuzzified.get(UMIDITATE.getName());
        Map<String, Double> v = r.fuzzified.get(VANT.getName());

        if (v.get("slab") >= 0.5)
            rec.add("Vânt slab: alegeți un loc cu ventilație bună (curent de aer, balcon deschis) " +
                    "sau folosiți un ventilator.");
        if (v.get("slab") >= 0.2 || hours > 6)
            rec.add("Măriți distanța dintre haine și nu le suprapuneți, pentru ca aerul să circule liber.");
        if (u.get("ridicată") >= 0.5)
            rec.add("Umiditate ridicată: uscați într-o cameră ventilată sau cu dezumidificator; " +
                    "evitați spațiile închise și reci.");
        if (t.get("scăzută") >= 0.5)
            rec.add("Temperatură scăzută: plasați rufele aproape de o sursă de căldură sau la soare, " +
                    "dacă este posibil.");
        if (hours > 7)
            rec.add("Stoarceți/centrifugați bine rufele înainte de întindere și întoarceți-le periodic.");
        if (hours < 3.5 && rec.isEmpty())
            rec.add("Condiții foarte bune: puteți întinde rufele afară, ele se vor usca repede.");
        if (rec.isEmpty())
            rec.add("Condiții acceptabile: întindeți rufele într-un loc aerisit și verificați-le periodic.");
        return rec;
    }
}
