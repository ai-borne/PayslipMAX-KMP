# 04. Data Moat, Roadmap & Metrics

This document details Phase 7 (Intelligence Moat), Phase 8 (Engineering Plan), and Phase 9 (Success Criteria) for the PayslipMax AI Insights system.

---

## Phase 7 — Intelligence Moat

Unlike typical expense trackers, PayslipMax becomes exponentially more valuable the longer it is used. The data moat builds over four chronological milestones:

```
 [1 Month: Single Snapshot] ──► [6 Months: Optimal Audit] ──► [2 Years: Tax Visibility] ──► [5 Years: Career Ledger]
 • Basic savings rate checks    • DA arrears verification      • FY tax-spike forecasts     • Promotion base curves
 • Standalone pay lookup        • Quarters rent risk alerts    • Posting shift tracking     • Comprehensive retirement wealth
 • Easily replicated            • MoM variance baselines       • Multi-year trends          • Insurmountable switching cost
```

### 1. 1 Month (Single Snapshot - Entry Level)
* **Intelligence**: Basic pay component extraction.
* **User Value**: Single-month savings rate check (minimum 6% DSOP rule).
* **Moat**: Weak. A competitor can easily copy text extraction.

### 2. 6 Months (Optimal Audit - Mid Level)
* **Intelligence**: Chronological MoM ledger baselines.
* **User Value**: Audits retroactive arrears (like 2% DA hikes) by comparing historical basic pay against current credits. Warns of quarters recovery risks by tracking consecutive months of HRA/License Fee anomalies.
* **Moat**: Solid. Auditing arrears and predicting retroactive debt requires historical context.

### 3. 2 Years (Tax Visibility - High Level)
* **Intelligence**: Full-cycle financial year tax trajectories.
* **User Value**: Maps allowance evolution as officers relocate. Prevents Jan/Feb tax adjustments from crashing net pay by forecasting YTD income tax trajectories early in the FY.
* **Moat**: High. Multi-year salary tracking builds trust and makes the tool indispensable.

### 4. 5 Years (Career Ledger - Compound Moat)
* **Intelligence**: Compounded career and retirement wealth ledger.
* **User Value**: Visualizes salary growth curves through promotions (e.g. Captain -> Major -> Lt Col). Calculates retirement gratuity and pension projections based on actual historical DSOP growth and basic pay base progressions.
* **Moat**: Insurmountable. Leaving the platform means deleting a verified, military-specific career ledger that cannot be recreated by banks or tax software.

---

## Phase 8 — Engineering Plan

We break down development into a phase-wise implementation roadmap.

### Phase 1: MVP (Deterministic Core)
* **Deliverables**:
  1. Build the local rules engine (`DeterministicIntelligenceEngine.kt`/`Swift`) implementing DA math, HRA checks, and debit spikes.
  2. Implement the metrics dashboard UI (Compose / SwiftUI) displaying the core four indicators.
  3. Create the SQLite caching layer (`Room`/`GRDB`) to store parsed results.
* **Complexity**: Low.
* **Dependencies**: PDF Parsing Parser.
* **AI Requirement**: None (100% Rules).
* **Estimated Effort**: 2 weeks.

### Phase 2: V2 (AI-Assisted Action Layer)
* **Deliverables**:
  1. Deploy the Serverless Cloud Function Proxy (Firebase Cloud Functions) with secure Secret Manager API keys.
  2. Integrate the AI Abstraction Layer to fetch and parse JSON responses from `gemini-2.5-flash`.
  3. Implement the slide-over Action Drawer to view, edit, and copy pre-filled PCDA representation letters.
  4. Build the interactive DSOP compounding simulator tool.
* **Complexity**: Medium.
* **Dependencies**: MVP, Firebase project.
* **AI Requirement**: Cloud Gemini API.
* **Estimated Effort**: 3 weeks.

### Phase 3: V3 (100% On-Device Hybrid AI)
* **Deliverables**:
  1. Migrate the prompt assembly and claim drafting logic on-device using MediaPipe LLM Inference API (for Android) and CoreML/Swift-Transformers (for iOS).
  2. Embed and test a lightweight local LLM (e.g. Gemma-2B-IT).
  3. Implement 100% offline representation letter drafting.
* **Complexity**: High.
* **Dependencies**: V2, Hardware check utilities.
* **AI Requirement**: On-device Gemma-2B weights.
* **Estimated Effort**: 5 weeks.

---

## Phase 9 — Success Criteria

To measure value and validate product quality before release, we track five key metrics:
1. **Insight Open Rate**: Target $>90\%$ of users who upload a new statement tap on the "AI Insights" dashboard within 24 hours of parsing.
2. **Session Retention Time**: Average user session duration on the Insights screen $>90$ seconds (verifying users are reading the "So What?" narratives and playing with the DSOP simulator).
3. **Action Rate (Click-Through)**: Target $>25\%$ click-through rate on primary Action buttons (e.g. "Draft Letter") when severe warnings (debit recovery, rent risk) are displayed.
4. **Subscription Conversion Boost**: A target $>15\%$ increase in free-to-premium conversions within 30 days of releasing the Arrears Auditor and Rent Risk alert.
5. **Annual Churn Reduction**: Target $<5\%$ churn rate for premium users who have uploaded $12+$ months of statements, proving the strength of the career data moat.
