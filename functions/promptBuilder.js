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
  const { earnings, deductions, summary, taxAndSavings } = payslip;

  const anomalySection = anomalies.length > 0
    ? anomalies.map((a) => `[${a.type}] ${a.description}`).join(", ")
    : "None";

  // Summarize up to the last 6 statements in history for compact context
  const histSummary = history.slice(-6).map((h) =>
    `${h.year}-${String(h.monthNum).padStart(2, "0")}: Gross: ₹${Math.round(h.grossPay)}, Net: ₹${Math.round(h.netPay)}, DSOP: ₹${Math.round(h.dsopSubscription || 0)}, Tax: ₹${Math.round(h.incomeTax || 0)}`
  ).join(" | ");

  const ytdInfo = taxAndSavings ? `YTD Gross: ₹${Math.round(taxAndSavings.grossSalaryYtd)}, YTD Tax: ₹${Math.round(taxAndSavings.taxDeductedYtd)}, DSOP Balance: ₹${Math.round(taxAndSavings.dsopFund?.closingBalance || 0)}` : "None";

  return `You are an expert Chartered Accountant auditing Indian Defence Services pay.
Analyze the pay details and return a structured JSON report. Do NOT include markdown wrapping or conversational filler. Return only valid raw JSON.

Current Pay: Gross: ₹${Math.round(earnings.grossPay ?? summary.grossPay)}, Net: ₹${Math.round(summary.netRemittance)}, Tax: ₹${Math.round(deductions.incomeTax)}, DSOP: ₹${Math.round(deductions.dsopSubscription)}
Savings Rate: ${monthlySavingRate.toFixed(1)}% (target >= 20%), Tax Ratio: ${taxRatio.toFixed(1)}%, Health Score: ${healthScore}/100

YTD & Fund Context: ${ytdInfo}
Flagged Anomalies: ${anomalySection}
History of past months: ${histSummary || "None"}

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
