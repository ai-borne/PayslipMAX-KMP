"use strict";

/**
 * promptBuilder.js
 *
 * Pure function module — zero external dependencies.
 * Transforms a validated InsightProxyRequest into a Gemini prompt string.
 * All PII has already been stripped by the Android client (RedactionSanitizer).
 *
 * Max 80 lines (including comments) — line limit enforced.
 */

/**
 * @param {object} payload - Validated InsightProxyRequest body
 * @param {object} payload.payslip - Redacted ParsedPayslip (no PII)
 * @param {Array}  payload.anomalies - Deterministic anomaly list from local engine
 * @param {number} payload.monthlySavingRate - Savings rate %
 * @param {number} payload.taxRatio - Effective tax ratio %
 * @param {number} payload.healthScore - Financial health score 0-100
 * @param {Array}  [payload.history=[]] - Last N ledger records
 * @returns {string} Formatted Gemini prompt
 */
function buildPrompt(payload) {
  const { payslip, anomalies, monthlySavingRate, taxRatio, healthScore, history = [] } = payload;
  const { earnings, deductions, summary } = payslip;

  const anomalySection = anomalies.length > 0
    ? anomalies.map((a) => `[${a.type}] ${a.description}`).join(", ")
    : "None";

  const prevMonth = history.length > 0 ? history[history.length - 1] : null;
  const comparisonText = prevMonth
    ? `Gross: ₹${Math.round(prevMonth.grossPay)}, Net: ₹${Math.round(prevMonth.netPay)}, Tax: ₹${Math.round(prevMonth.incomeTax)}, DSOP: ₹${Math.round(prevMonth.dsopSubscription || 0)} (${prevMonth.year}-${String(prevMonth.monthNum).padStart(2, "0")})`
    : "None";

  return `You are an expert Chartered Accountant auditing Indian Defence Services pay.
Analyze the pay details and return a structured JSON report. Do NOT include any markdown wrapping, code block formatting (such as \`\`\`json), or conversational filler. Return only valid raw JSON.

Current Pay Data:
- Gross Pay: ₹${Math.round(earnings.grossPay ?? summary.grossPay)}
- Net Take-Home: ₹${Math.round(summary.netRemittance)}
- Income Tax: ₹${Math.round(deductions.incomeTax)}
- DSOP: ₹${Math.round(deductions.dsopSubscription)}
- Savings Rate: ${monthlySavingRate.toFixed(1)}% (target >= 20%), Tax Ratio: ${taxRatio.toFixed(1)}%, Health Score: ${healthScore}/100

Previous Month Pay Context: ${comparisonText}
Flagged Anomalies: ${anomalySection}

Return JSON matching the following keys:
{
  "salaryChanges": [
    {"item": "e.g. Basic Pay", "change": "increased|decreased|unchanged", "amount": 1200}
  ],
  "missingAllowances": ["e.g. Transport Allowance"],
  "newDeductions": [
    {"item": "e.g. Water Charges", "change": "increased|decreased", "amount": 100}
  ],
  "riskAlerts": [
    {"observation": "Net salary decreased by 5%", "action": "Verify Base Pay and DA adjustments"}
  ],
  "opportunities": [
    {"opportunity": "NPS Section 80CCD(1B) headroom of ₹50,000 unutilized", "action": "Invest ₹50,000 in NPS to save tax"}
  ],
  "actionRequired": [
    "Verify missing Transport Allowance with PCDA Pune"
  ]
}`;
}

module.exports = { buildPrompt };
