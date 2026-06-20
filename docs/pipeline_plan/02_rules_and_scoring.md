# 02. Rule Engine Partitioning & Prioritization Engine

This document details Phase 3 (AI vs. Rule Engine Separation) and Phase 4 (Insight Prioritization Engine) for the PayslipMax AI Insights system.

---

## Phase 3 — AI vs. Rule Engine Separation

To maintain unit-level profitability, 100% offline availability, and absolute correctness, PayslipMax employs a hybrid intelligence pipeline. We strictly separate deterministic calculations from linguistic reasoning.

```
       [Parsed Payslip JSON Payload]
                    │
          ┌─────────┴─────────┐
          ▼                   ▼
  [Rule-Based Engine]  [AI-Assisted Engine]
  • Runs Local & Fast  • Runs Cloud/Proxy
  • Verifies Math      • Writes Draft Letters
  • Flags Anomalies    • Explains Text Remarks
  • Compares Trends    • Recommends Wealth Strategy
```

### 1. The Rule-Based Engine (Deterministic Local Execution)
All structural audits, numerical variance checks, and strict anomaly triggers are processed on-device via local Kotlin/Swift logic. This ensures zero cloud costs for standard alerts:
* **Salary Trend Analysis**: Basic Pay hikes, DA revision percentages, arrears audits, and MoM gross/net pay variances.
* **Allowance Checkers**: Checks if HRA is zero while License Fee and Furniture Rent deductions are also zero.
* **Deduction Audits**: Spikes in Income Tax (TDS), unexpected debit recovery flags, or LTC ticket recovery amounts.
* **Compounding Metrics**: DSOP monthly subscription rate calculations (verifying it is above the 6% statutory minimum), and tracking interest credits.
* **Why**: Rules do not hallucinate, require no internet connection, execute in less than 5 milliseconds, and incur $0 in API costs.

### 2. The AI-Assisted Engine (Cognitive Cloud Execution)
Gemini is triggered selectively for tasks requiring natural language reasoning, template customization, and career-behavioral analysis:
* **Formal Letter Synthesis**: Creating official, audit-compliant representation letters to PCDA(O) Pune, incorporating personal details, account numbers, and specific dates.
* **Unstructured Remarks Interpretation**: PCDA statements often contain highly abbreviated ledger remarks (e.g. "ADJ TPTA SEC-54 CR"). The LLM maps these against military glossary vectors to explain them in plain English.
* **Behavioral Career Narrative**: Explaining how career milestones (like promotion from Captain to Major, or relocation to a peace station) impact their long-term wealth, DSOP targets, and tax tax tax trajectories.
* **Why**: LLMs are excellent at synthesizing custom text matching strict military etiquette and converting raw numbers into humanized, emotionally reassuring advice.

---

## Phase 4 — Insight Prioritization Engine

To prevent dashboard clutter, a ranking model scores and filters generated insights on five dimensions:

### 1. Scoring Dimensions (Scale: 1–10)
* **Importance (IMP)**: The severity of the financial impact.
  - *Example*: Rent recovery risk (high retroactive debt) = 9/10. Standard basic pay increment = 5/10.
* **Confidence (CONF)**: The data support accuracy.
  - *Example*: DA arrears matching historical math exactly = 10/10. Housing rent risk (predictive, missing quarters voucher) = 8/10.
* **Novelty (NOV)**: Preventing alert fatigue.
  - *Formula*: `10` if the insight has not been displayed in the last 3 months; `2` if identical to the previous month's active insight.
* **Actionability (ACT)**: Can the user solve this?
  - *Example*: Anomaly with a pre-filled PCDA email draft = 10/10. High tax rate (pure information) = 5/10.
* **Premium Value (PREM)**: Does this justify subscription?
  - *Example*: Verifying arrears credits or preventing debt recoveries = 10/10. General savings rate advice = 6/10.

### 2. Ranking Algorithm
The engine calculates a weighted priority score for each candidate insight:

$$\text{Priority Score} = (\text{IMP} \times 0.35) + (\text{CONF} \times 0.20) + (\text{NOV} \times 0.15) + (\text{ACT} \times 0.20) + (\text{PREM} \times 0.10)$$

```kotlin
// Prioritization logic inside InsightEngine.kt
fun prioritize(insights: List<InsightCandidate>): List<Insight> {
    return insights
        .map { candidate ->
            val priority = (candidate.imp * 0.35) + 
                           (candidate.conf * 0.20) + 
                           (candidate.nov * 0.15) + 
                           (candidate.act * 0.20) + 
                           (candidate.prem * 0.10)
            Insight(candidate, priority)
        }
        // Filter out insights below the quality threshold
        .filter { it.priorityScore >= 7.0 }
        // Sort descending by priority score
        .sortedByDescending { it.priorityScore }
        // Limit display to prevent information overload
        .take(4)
}
```

This prioritization ensures that when the user opens their monthly dashboard, they are only presented with high-signal, actionable items.
