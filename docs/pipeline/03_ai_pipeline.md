# 03. AI Pipeline & Context Strategy

This document details the step-by-step AI generation pipeline and evaluates the optimal context length strategy.

---

## 1. AI Generation Pipeline Step-by-Step

```
User Uploads Payslip 
  ↓
Parser Generates JSON (Sanitized)
  ↓
Historical Payslip Data Retrieved (Local DB)
  ↓
Prompt Assembly (Redacted PII)
  ↓
Gemini API Call (via Serverless Cloud Proxy)
  ↓
Insight Response (Valid JSON Schema)
  ↓
Local Caching (SQLite DB)
  ↓
UI Rendering (Native Compose components)
```

### Steps Detail
1. **User Uploads Payslip**: The user imports the PDF payslip into the application.
2. **Parser Generates JSON**: The parsing engine extracts salary details and outputs a sanitized JSON schema, running the `RedactionSanitizer` to ensure zero PII is present.
3. **Historical Payslip Data Retrieved**: The app reads up to the last 6 months of historical ledger records from the local SQLite database.
4. **Prompt Assembly**: The app combines the current month pay details, deterministic anomalies, financial metrics, and the historical ledger summary into a consolidated text payload.
5. **Gemini API Call**: The app makes a secure POST request to the Firebase Cloud Function proxy, passing the payload. The proxy injects the developer's Secret Manager API key and makes a REST call to `gemini-2.5-flash` with `maxOutputTokens: 8192` and a strict `responseSchema`.
6. **Insight Response**: Gemini runs its reasoning process (outputting internal thoughts) and returns a structural JSON response matching the expected schema.
7. **Local Caching**: The client app parses the JSON and writes it to the local SQLite database `financial_insights` table, keyed by the month.
8. **UI Rendering**: The app decodes the JSON payload into native `@Serializable` classes and renders the Native Compose subviews (salary changes list, opportunity cards, risk badges) with zero markdown rendering artifacts.

---

## 2. Payslip Context Strategy Evaluation

To determine how much historical statement data should be included in the prompt, we compare five strategies:

| Context Strategy | Cost (Input Tokens) | Accuracy of Audits | User Value (Actionable Suggestions) | Recommendation |
| :--- | :--- | :--- | :--- | :--- |
| **Current Month Only** | Minimal (~400 tokens) | Low. Cannot detect MoM spikes or missing allowances. | Poor. Only basic standalone checks. | **Not Recommended** |
| **Last 3 Months** | Low (~600 tokens) | Moderate. Catches recent changes but misses semi-annual DA hikes. | Fair. Alerting is limited. | **Sub-optimal** |
| **Last 6 Months** | **Balanced (~900 tokens)** | **High. Fully covers semi-annual DA increments & tax forecasting.** | **Excellent. Enables PCDA audit projections and DSOP simulator calculations.** | **Recommended (Optimal)** |
| **Last 12 Months** | High (~1,500 tokens) | High. Incremental benefit over 6 months is negligible. | Excellent. Slightly redundant. | **Excessive Cost** |
| **Full History** | Scalable risk (10k+ tokens) | High. Risk of model distraction or token boundary failure. | Marginal improvement. | **Inefficient** |

### Why 6 Months is the Optimal Choice
1. **Semi-Annual Alignment**: The Indian Armed Forces adjust Dearness Allowance (DA) twice a year (typically effective January and July, paid in March and September). A 6-month window guarantees that the engine captures the preceding transition point.
2. **Tax Forecast Stability**: In India, the financial year runs from April to March. A 6-month historical view provides sufficient context (e.g. tracking Year-To-Date numbers) to compute tax projections accurately during transition quarters without blowing up input token costs.
3. **Model Context Window Performance**: Keeping context limited to 6 months minimizes prompt dilution, ensuring that `gemini-2.5-flash` is highly focused on active pay anomalies instead of older historical noise.
