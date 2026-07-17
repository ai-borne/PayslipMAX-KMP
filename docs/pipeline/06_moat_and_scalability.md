# 06. Accrued Intelligence, Moats & Roadmap

This document describes how PayslipMax accumulates defensive competitive advantage and details the technical roadmap for AI scalability.

---

## 1. Historical Intelligence Layer (Accrued Value)

Unlike standard expense trackers, PayslipMax becomes exponentially more valuable the longer it is used. We call this **Accrued Intelligence**:

```
[Month 1]
Single Payslip Snapshot
• Simple data lookup
• Initial health score
• No chronological context
       ↓
[Month 12]
1-Year Salary Intelligence
• MoM changes (DA hikes detected)
• Tax-saving projections vs actuals
• Missing allowances identified (was present in Month 2, absent in Month 12)
       ↓
[Month 60]
5-Year Career & Wealth Intelligence
• Multi-year salary growth curve
• Promotion effects (Basic Pay increments over time)
• Posting changes detected (Class A City TPTA vs Class C City)
• PCDA PCDA(O) audit anomaly patterns
```

### Why a Competitor Cannot Replicate
A new competitor cannot bootstrap this value because the platform's alerts rely on comparative chronological data. Without access to a user's multi-year historical ledger records, a competitor can only perform shallow, single-month validation.

---

## 2. Competitive Moats

```
┌────────────────────────────────────────────────────────┐
│                    COMPETITIVE MOATS                   │
├───────────────────┬────────────────────────────────────┤
│ Data Moat         │ User-contributed, encrypted historical │
│                   │ pay ledgers spanning up to 5+ years│
├───────────────────┼────────────────────────────────────┤
│ Insight Moat      │ Multi-year salary anomaly knowledge │
│                   │ base and PCDA PCDA(O) resolution rules│
├───────────────────┼────────────────────────────────────┤
│ Switching Cost    │ High. Leaving means deleting all   │
│                   │ historical salary growth records   │
├───────────────────┼────────────────────────────────────┤
│ AI Moat           │ Fine-tuned prompts representing    │
│                   │ military allowance audit heuristics│
├───────────────────┼────────────────────────────────────┤
│ Trust Moat        │ Offline-first storage (encrypted   │
│                   │ local SQLite database)             │
└───────────────────┴────────────────────────────────────┘
```

---

## 3. Scalability Roadmap

To scale performance, manage costs, and preserve user privacy, the AI pipeline executes in three progressive phases:

```
[Phase 1: Cloud AI] ───► [Phase 2: Hybrid AI] ───► [Phase 3: 100% Local AI]
• Serverless Cloud Proxy  • Local parsing & rule engine  • On-device LLM inference
• Gemini 2.5 Flash API    • Cloud proxy for complex      • Apple Core AI (iOS)
                          • prompts & drafts             • Android AI Edge / Gemma
```

### Migration Path

#### Phase 1: Cloud AI (Legacy State)
All prompt assembly and JSON parsing happens on the client, and execution goes to `gemini-2.5-flash` via the Firebase cloud function proxy.
- *Pros*: Quick delivery, high reasoning quality.
- *Cons*: Token costs, dependency on internet connectivity.

#### Phase 2: Hybrid AI (Current Implemented State)
Deterministic calculation rules (tax saving maths, missing allowances, DA arrears, quarters rent recovery, unexpected debits) execute 100% offline inside the partitioned client engine [DeterministicIntelligenceEngine](file:///Users/test/Downloads/PDFParser/shared/src/commonMain/kotlin/com/payslipmax/pdfparser/insights/DeterministicIntelligenceEngine.kt). Only complex reasoning checks and narrative generation are routed through the cloud proxy.
- *Pros*: Reduces cloud proxy usage by ~70%, lowering token costs and improving performance.
- *Status*: Fully implemented and active in production.

#### Phase 3: 100% Local AI (Active Transition State)
The mobile app prepares for a lightweight local LLM (e.g., Gemma 2B) utilizing Google's MediaPipe LLM Inference API and CoreML. 
- *Status*: The decoupled `AIProviderManager` and `AIInsightProvider` contract interfaces are fully implemented. A setting switch toggle is added to `SettingsScreen.kt` permitting the user to opt-in to `useLocalAi` mode, which routes requests to [LocalGemmaProvider](file:///Users/test/Downloads/PDFParser/shared/src/commonMain/kotlin/com/payslipmax/pdfparser/insights/LocalGemmaProvider.kt) (stubbed in the current codebase).
- *Pros*: Zero cloud inference costs, 100% offline functionality, absolute privacy.
- *Cons*: Higher initial app download size (~1.5–2 GB for model weights), battery/hardware performance constraints on older SoCs.
