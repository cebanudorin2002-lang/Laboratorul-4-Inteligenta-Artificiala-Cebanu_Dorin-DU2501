# UscareaRufe — Java (IntelliJ)

Prognoza timpului de uscare a rufelor — sistem fuzzy Mamdani.  
Пересборка с C# (WinForms) на Java (Swing).

**Студент:** Cebanu Dorin DU2501 · **Вариант:** 3 (LAB4)

---

## Как открыть в IntelliJ IDEA

1. **File → Open** → папка `UscareaRufeJava` (где `pom.xml`)
2. Trust / Load Maven — **Yes**
3. JDK **11+**
4. Run config **Main (GUI)** → ▶ Run  
   или `Main.java` → правый клик → **Run 'Main.main()'**

---

## Структура

```
UscareaRufeJava/
├── pom.xml
├── UscareaRufe.iml
├── .idea/runConfigurations/Main__GUI_.xml
└── src/main/java/uscarearufe/
    ├── Main.java
    ├── fuzzy/     — MembershipFunction, LinguisticVariable, FuzzyRule, MamdaniEngine
    ├── model/     — DryingModel (переменные, 13 правил, рекомендации)
    └── ui/        — MainForm, FuzzyPlotPanel, Surface3DPanel
```

## Что сохранено

- Mamdani: fuzzify → AND/OR → clip → aggregate max → centroid  
- 3 входа (T, H, V), выход «Timp de uscare», 13 правил  
- GUI: ввод, результат, рекомендации, графики μ, 3D-поверхности (вращение мышью), таблица правил  
