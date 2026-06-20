# 03. UX Architecture & Prompt Design

This document details Phase 5 (UX Architecture) and Phase 6 (AI Prompt Architecture) for the PayslipMax AI Insights system.

---

## Phase 5 — UX Architecture

We translate the premium aesthetics of the preview into a production layout framework.

### 1. Information Hierarchy
To ensure high readability and emotional engagement, the screen layout follows a strict top-to-bottom hierarchy:
1. **Context Control (Top)**: Month/Year selector. Establishes the current time anchor.
2. **Flagship Cashflow indicators (Mid-Top)**: Clean metrics cards displaying Net Remittance (success green), Gross Pay, DSOP Balance (info blue), and Effective Tax Rate (warning orange).
3. **Prioritized Audits (Middle)**: Glassmorphic cards containing the highest priority insights. 
4. **Historical Timeline (Bottom)**: Chronological timeline summarizing past updates and compiled intelligence.
5. **Developer Critique (Developer Builds Only)**: Collapsible bottom sheet for feedback.

### 2. Card Taxonomy
Production utilizes four standard card templates to handle visual signaling:
* **Danger Card (Red Border)**: For severe cashflow impacts (e.g. debit recoveries).
  - *Design*: `border-left: 5px solid var(--color-danger); bg: rgba(239, 68, 68, 0.05);`
* **Warning Card (Amber Border)**: For pending financial risks (e.g. missing rent deductions).
  - *Design*: `border-left: 5px solid var(--color-warning); bg: rgba(245, 158, 11, 0.05);`
* **Success Card (Green Border)**: For verified credits and compounding milestones.
  - *Design*: `border-left: 5px solid var(--color-success); bg: rgba(16, 185, 129, 0.05);`
* **Info Card (Blue Border)**: For general policy revisions or calendar projections.
  - *Design*: `border-left: 5px solid var(--color-info); bg: rgba(59, 130, 246, 0.05);`

### 3. Sorting & Layout Logic
* **Sorting Rule**: Sort by `Priority Score` descending.
* **Secondary Sort (Tie-breaker)**: If priority scores are equal, sort by the absolute financial value (money impact in ₹) descending.
* **CTAs**: Ensure every card answering "So What?" includes a primary action button (e.g. "Draft Letter" or "Open Simulator") to close the loop.

---

## Phase 6 — AI Prompt Architecture

For insights requiring AI synthesis (e.g. drafting claims or explaining ledger remarks), the system leverages the following prompt structure.

### 1. System Prompt
```
Role: You are a senior military financial auditor and expert Chartered Accountant (CA) specializing in Indian Armed Forces pay structures, PCDA(O) rules, and defense tax codes.

Instructions:
1. Analyze the provided current payslip JSON and the 6-month historical timeline.
2. Draft highly actionable, brief financial insights and formal letters to the Officer-in-Charge, PCDA(O) Pune.
3. Keep the tone professional, direct, and respectful, complying with military etiquette.
4. Avoid generic financial advice. Reference specific military pay components: DSOP, MSP, TPTA, Dearness Allowance (DA), AGIF, and HRA.
5. Output your analysis STRICTLY in structured JSON format matching the specified schema. Do not output markdown, HTML, or conversational text.
```

### 2. Data Payload Schema (Sent to Cloud Function Proxy)
```json
{
  "current_month": {
    "year": 2026,
    "month_num": 4,
    "earnings": { "basic_pay": 149000, "dearness_allowance": 98700, "arrears_da": 9870 },
    "deductions": { "dsop_subscription": 40000, "ticket_recovery": 3056 }
  },
  "history_summary": [
    { "month": "March 2026", "basic": 149000, "net": 183115, "anomalies": [] },
    { "month": "December 2025", "basic": 144700, "net": 161357, "anomalies": ["Debit Recovery: 15742"] }
  ]
}
```

### 3. Target Output JSON Schema
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "insightId": { "type": "string" },
    "headline": { "type": "string" },
    "category": { "type": "string", "enum": ["danger", "warning", "success", "info"] },
    "importance": { "type": "integer", "minimum": 1, "maximum": 10 },
    "confidence": { "type": "integer", "minimum": 1, "maximum": 10 },
    "actionLabel": { "type": ["string", "null"] },
    "actionEmailSubject": { "type": ["string", "null"] },
    "actionEmailBody": { "type": ["string", "null"] },
    "narrative": { "type": "string" }
  },
  "required": ["insightId", "headline", "category", "importance", "confidence", "narrative"]
}
```
Using this schema ensures the cloud response is parsed directly into native Kotlin/Swift models without any markdown rendering artifacts.
