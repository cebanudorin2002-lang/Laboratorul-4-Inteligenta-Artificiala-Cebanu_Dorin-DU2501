# Laboratorul 4 — Sistem fuzzy Mamdani pentru prognoza timpului de uscare a rufelor

**Varianta:** 3 (LAB4)
**Limbaj / tehnologii:** Java 11+, Swing (GUI), Maven
**Metoda:** inferență fuzzy de tip Mamdani, defuzificare prin centroid

---

## 1. Scopul lucrării

Se realizează un sistem fuzzy care estimează **timpul de uscare a rufelor (în ore)** pe baza a trei factori de mediu: temperatura, umiditatea aerului și viteza vântului. Sistemul oferă și o interpretare a rezultatului, recomandări practice, grafice ale funcțiilor de apartenență și suprafețe 3D ale dependenței ieșirii de două intrări.

---

## 2. Variabilele lingvistice

### 2.1. Intrări

| Variabilă | Unitate | Univers de discurs | Termeni |
|---|---|---|---|
| Temperatură | °C | [0, 40] | scăzută, medie, ridicată |
| Umiditate | % | [0, 100] | scăzută, medie, ridicată |
| Vânt | m/s | [0, 12] | slab, moderat, puternic |

### 2.2. Ieșire

| Variabilă | Unitate | Univers de discurs | Termeni |
|---|---|---|---|
| Timp de uscare | ore | [0, 12] | scurt, mediu, lung |

### 2.3. Funcții de apartenență

Toate funcțiile sunt trapezoidale `(a, b, c, d)`; triunghiul este cazul particular `b = c`.

**Temperatură**

| Termen | Tip | Parametri |
|---|---|---|
| scăzută | trapez | (0, 0, 8, 16) |
| medie | triunghi | (10, 20, 30) |
| ridicată | trapez | (24, 32, 40, 40) |

**Umiditate**

| Termen | Tip | Parametri |
|---|---|---|
| scăzută | trapez | (0, 0, 25, 45) |
| medie | triunghi | (30, 50, 70) |
| ridicată | trapez | (55, 75, 100, 100) |

**Vânt**

| Termen | Tip | Parametri |
|---|---|---|
| slab | trapez | (0, 0, 1, 3.5) |
| moderat | triunghi | (2, 4.5, 7) |
| puternic | trapez | (5.5, 8, 12, 12) |

**Timp de uscare**

| Termen | Tip | Parametri |
|---|---|---|
| scurt | trapez | (0, 0, 2, 4) |
| mediu | triunghi | (3, 6, 9) |
| lung | trapez | (8, 10, 12, 12) |

Formula gradului de apartenență:

```
μ(x) = 0                      dacă x < a sau x > d
μ(x) = 1                      dacă b ≤ x ≤ c
μ(x) = (x − a) / (b − a)      dacă a ≤ x < b
μ(x) = (d − x) / (d − c)      dacă c < x ≤ d
```

Termenii „umăr” (de exemplu `scăzută = (0, 0, 8, 16)`) au `a = b`, deci pornesc de la valoarea 1 la marginea universului. Constructorul verifică condiția `a ≤ b ≤ c ≤ d`.

---

## 3. Baza de reguli (13 reguli)

| Nr. | Regulă | Operator | Pondere |
|---|---|---|---|
| 1 | DACĂ temperatura este ridicată ȘI umiditatea este scăzută ATUNCI timpul este **scurt** | AND | 1.0 |
| 2 | DACĂ temperatura este scăzută ȘI umiditatea este ridicată ATUNCI timpul este **lung** | AND | 1.0 |
| 3 | DACĂ vântul este puternic ȘI umiditatea este scăzută ATUNCI timpul este **scurt** | AND | 1.0 |
| 4 | DACĂ temperatura este medie ȘI umiditatea este medie ATUNCI timpul este **mediu** | AND | 1.0 |
| 5 | DACĂ temperatura este ridicată ȘI vântul este puternic ATUNCI timpul este **scurt** | AND | 1.0 |
| 6 | DACĂ temperatura este scăzută ȘI vântul este slab ATUNCI timpul este **lung** | AND | 1.0 |
| 7 | DACĂ umiditatea este ridicată ȘI vântul este slab ATUNCI timpul este **lung** | AND | 1.0 |
| 8 | DACĂ temperatura este medie ȘI vântul este moderat ATUNCI timpul este **mediu** | AND | 1.0 |
| 9 | DACĂ temperatura este ridicată ȘI umiditatea este ridicată ATUNCI timpul este **mediu** | AND | 1.0 |
| 10 | DACĂ temperatura este scăzută SAU umiditatea este ridicată ATUNCI timpul este **lung** | OR | 0.4 |
| 11 | DACĂ temperatura este medie ȘI umiditatea este scăzută ATUNCI timpul este **scurt** | AND | 0.7 |
| 12 | DACĂ temperatura este ridicată ȘI vântul este moderat ATUNCI timpul este **scurt** | AND | 1.0 |
| 13 | DACĂ umiditatea este medie ȘI vântul este slab ATUNCI timpul este **mediu** | AND | 1.0 |

**Observații**

- Căldura, aerul uscat și vântul accelerează uscarea; frigul, umezeala și lipsa vântului o încetinesc.
- Regula 9 (cald, dar foarte umed) dă un rezultat intermediar, deoarece aerul saturat limitează evaporarea.
- Regula 10 folosește operatorul `OR` (se activează ușor), de aceea are pondere mică (0.4).
- Regula 11 este o regulă mai puțin sigură, cu pondere 0.7.

---

## 4. Algoritmul de inferență Mamdani

Implementat în clasa `MamdaniEngine`, metoda `infer(...)`.

1. **Fuzificare.** Valoarea numerică (limitată la universul variabilei) se transformă în grade de apartenență la fiecare termen al variabilei.
2. **Evaluarea antecedentului.** `AND` → minim, `OR` → maxim; rezultatul se înmulțește cu ponderea regulii.
3. **Implicația.** Termenul de ieșire al regulii este „tăiat” la nivelul activării. Dacă mai multe reguli conduc la același termen, se păstrează maximul.
4. **Agregarea.** Pe o grilă de 601 puncte din universul ieșirii:
   `agg(x) = max_termeni ( min( clip_termen, μ_termen(x) ) )`
5. **Defuzificarea (centroid).**

   ```
   timp = Σ xᵢ · agg(xᵢ) / Σ agg(xᵢ)
   ```

   Dacă `Σ agg ≈ 0` (nicio regulă activă), indicatorul `hasOutput` este `false`, iar rezultatul este `NaN`; aplicația nu afișează o valoare inventată.

---

## 5. Structura proiectului

```
UscareaRufeJava/
├── pom.xml
├── UscareaRufe.iml
├── README.md
└── src/main/java/uscarearufe/
    ├── Main.java                      – punct de intrare
    ├── fuzzy/
    │   ├── MembershipFunction.java    – funcția de apartenență trapezoidală
    │   ├── LinguisticVariable.java    – variabilă lingvistică + fuzificare
    │   ├── Condition.java             – condiție „variabilă este termen”
    │   ├── LogicOp.java               – enum AND / OR
    │   ├── FuzzyRule.java             – regulă DACĂ … ATUNCI …
    │   ├── RuleActivation.java        – regulă + tăria activării
    │   ├── InferenceResult.java       – rezultatele intermediare și finale
    │   └── MamdaniEngine.java         – motorul de inferență
    ├── model/
    │   └── DryingModel.java           – variabilele, regulile, interpretarea, recomandările
    └── ui/
        ├── MainForm.java              – fereastra principală (4 secțiuni)
        ├── FuzzyPlotPanel.java        – graficele funcțiilor de apartenență
        └── Surface3DPanel.java        – suprafața 3D rotibilă cu mouse-ul
```

Separarea pe pachete: motorul fuzzy (`fuzzy`) este generic și independent de problemă, modelul (`model`) conține datele concrete ale variantei 3, iar interfața (`ui`) doar afișează rezultatele.

### Clasele principale

| Clasă | Rol |
|---|---|
| `MembershipFunction` | Calculează μ(x); validează parametrii `a ≤ b ≤ c ≤ d`; metode statice `triunghi` și `trapez`. |
| `LinguisticVariable` | Păstrează numele, unitatea, universul și termenii; `fuzzify(x)` returnează gradele pentru toți termenii. |
| `FuzzyRule` | Conține condițiile, operatorul, termenul de ieșire și ponderea; `getText()` generează textul regulii. |
| `MamdaniEngine` | Precalculează curbele termenilor de ieșire și execută cei 5 pași ai inferenței. |
| `DryingModel` | Definește variabilele și cele 13 reguli; `interpret(...)` și `recommend(...)` produc textul rezultatului. |

---

## 6. Interpretarea rezultatului și recomandările

### 6.1. Clasificarea timpului (`interpret`)

| Timp (ore) | Calificativ |
|---|---|
| < 3 | foarte rapidă |
| 3 – 5 | rapidă |
| 5 – 7 | medie |
| 7 – 9 | lentă |
| ≥ 9 | foarte lentă / îndelungată |

Se afișează și termenul de ieșire dominant, cu gradul lui de activare. Durata se formatează ca „X h YY min” (`formatDuration`).

### 6.2. Recomandări (`recommend`)

Recomandările se bazează pe gradele fuzzy ale intrărilor, nu pe valorile brute:

| Condiție | Recomandare |
|---|---|
| vânt slab ≥ 0.5 | loc cu ventilație bună sau ventilator |
| vânt slab ≥ 0.2 sau timp > 6 h | distanță mai mare între haine |
| umiditate ridicată ≥ 0.5 | cameră ventilată / dezumidificator |
| temperatură scăzută ≥ 0.5 | sursă de căldură sau soare |
| timp > 7 h | stoarcere / centrifugare bună, întoarcerea periodică a rufelor |
| timp < 3.5 h și nicio altă recomandare | condiții foarte bune, uscare afară |
| în rest | condiții acceptabile, loc aerisit |

---

## 7. Interfața grafică

Fereastra principală (`MainForm`) are patru secțiuni:

1. **Calcul** — câmpuri pentru temperatură, umiditate și vânt; butoanele *Calculează* și *Resetare*; rezultatul, interpretarea și recomandările.
2. **Funcții de apartenență** — graficele μ pentru fiecare variabilă, cu valoarea curentă marcată, forma agregată și centroidul.
3. **Suprafețe 3D** — dependența timpului de uscare de două intrări (a treia fixată); suprafața se rotește cu mouse-ul (proiecție 3D implementată manual în `Surface3DPanel`).
4. **Baza de reguli** — tabelul cu cele 13 reguli.

---

## 8. Exemple numerice (verificate)

### Exemplul 1: T = 28 °C, U = 30 %, V = 5 m/s

**Fuzificare**

| Variabilă | scăzută / slab | medie / moderat | ridicată / puternic |
|---|---|---|---|
| Temperatură (28) | 0 | 0.20 | 0.50 |
| Umiditate (30) | 0.75 | 0 | 0 |
| Vânt (5) | 0 | 0.80 | 0 |

**Reguli active**

| Regulă | Calcul | Activare | Termen |
|---|---|---|---|
| 1 | min(0.5; 0.75) | 0.50 | scurt |
| 8 | min(0.2; 0.8) | 0.20 | mediu |
| 11 | min(0.2; 0.75) · 0.7 | 0.14 | scurt |
| 12 | min(0.5; 0.8) | 0.50 | scurt |

Restul regulilor au activare 0.

**Tăiere:** scurt = 0.50, mediu = 0.20, lung = 0.

**Defuzificare (centroid):** timp ≈ **3.37 h** (aprox. 3 h 22 min) → uscare **rapidă**.

### Exemplul 2: T = 10 °C, U = 80 %, V = 1 m/s

**Fuzificare:** temperatură scăzută = 0.75; umiditate ridicată = 1.0; vânt slab = 1.0.

**Reguli active:** R2 = 0.75 (lung), R6 = 0.75 (lung), R7 = 1.0 (lung), R10 = max(0.75; 1) · 0.4 = 0.40 (lung).

**Tăiere:** lung = 1.0, restul 0.

**Defuzificare:** timp ≈ **10.45 h** (aprox. 10 h 27 min) → uscare **foarte lentă / îndelungată**.

---

## 9. Cum se rulează

1. În IntelliJ IDEA: **File → Open** → folderul proiectului (cel cu `pom.xml`).
2. Se acceptă încărcarea proiectului Maven (**Trust / Load Maven**).
3. Se setează **JDK 11 sau mai nou**.
4. Se pornește configurația **Main (GUI)** sau `Main.java` → *Run 'Main.main()'*.

---

## 10. Întrebări posibile la apărare

| Întrebare | Răspuns |
|---|---|
| De ce min pentru AND și max pentru OR? | Sunt operatorii clasici Zadeh pentru intersecția și reuniunea mulțimilor fuzzy. |
| Ce diferențiază Mamdani de Sugeno? | La Mamdani consecința regulii este o mulțime fuzzy care se defuzifică; la Sugeno este o funcție sau o constantă. Mamdani este mai intuitiv. |
| Ce rol au ponderile regulilor? | Reduc influența regulilor mai puțin sigure (de exemplu 0.4 la regula 10). |
| De ce se folosește centroidul? | Ține cont de întreaga formă agregată și dă o ieșire netedă, continuă. |
| De ce 601 puncte? | Este rezoluția discretizării pentru calculul centroidului; mai multe puncte înseamnă o aproximare mai bună a integralei. |
| Ce se întâmplă dacă nicio regulă nu se activează? | `hasOutput = false`, iar aplicația nu afișează un rezultat. |
| De ce termenii se suprapun? | Suprapunerea permite tranziții graduale și activarea simultană a mai multor reguli. |

---

## 11. Concluzii

Sistemul fuzzy Mamdani cu 3 intrări, 1 ieșire și 13 reguli estimează coerent timpul de uscare a rufelor: condițiile favorabile (cald, uscat, vânt) conduc la durate scurte, iar cele nefavorabile (rece, umed, fără vânt) la durate lungi. Arhitectura pe straturi (motor fuzzy generic, model specific, interfață) face aplicația ușor de extins cu variabile sau reguli noi.
