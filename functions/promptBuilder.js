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
    ? anomalies.map((a) => `  • [${a.type}] ${a.description}`).join("\n")
    : "  • No anomalies detected this month.";

  const prevMonth = history.length > 0 ? history[history.length - 1] : null;
  const comparisonText = prevMonth
    ? `Gross: ₹${Math.round(prevMonth.grossPay)}, Net: ₹${Math.round(prevMonth.netPay)}, Tax: ₹${Math.round(prevMonth.incomeTax)}, DSOP: ₹${Math.round(prevMonth.dsopSubscription || 0)} (${prevMonth.year}-${String(prevMonth.monthNum).padStart(2, "0")})`
    : "No historical data available.";

  return `You are an expert Chartered Accountant auditing Indian Defence Services pay.
Generate a concise, fact-based salary audit using ONLY bullet points under each ## heading.
DO NOT use paragraphs, intro/outro text, narrative summaries, motivational talk, or filler text.
Limit the response to 8-12 bullets total. Every observation must be grounded in the provided numbers.

## This Month's Pay Summary
- Gross Pay: ₹${Math.round(earnings.grossPay ?? summary.grossPay)}
- Net Take-Home: ₹${Math.round(summary.netRemittance)}
- Income Tax Deducted: ₹${Math.round(deductions.incomeTax)}
- DSOP Subscription: ₹${Math.round(deductions.dsopSubscription)}
- Savings Rate: ${monthlySavingRate.toFixed(1)}% (target ≥ 20%), Tax Ratio: ${taxRatio.toFixed(1)}%, Health Score: ${healthScore}/100

## Previous Month Context
- Previous Month Pay Details: ${comparisonText}

## Flagged Anomalies
${anomalySection}

## Format the response exactly like this template (only include headings if there are matching findings):

## Salary Changes
• [Type of change - e.g., Basic Pay / DA / MSP] [increased/decreased/unchanged] by [exact ₹ delta]
• [Metric] [increased/decreased] by [exact ₹ delta]

## Missing Allowances
• [Allowance Name] absent this month (was present in previous month)

## New Deductions
• [Deduction Name] [increased/decreased] by [exact ₹ delta]

## Risk Alerts
• [Risk Observation] (e.g. Net salary decreased by X%) -> [Impact/Action]

## Opportunities
• [Opportunity - e.g. DSOP contribution optimized at ₹X / NPS Section 80CCD(1B) space unutilised] -> [Action]

## Action Required
• [Required Action - e.g. Verify missing Transport Allowance / Check HRA tax declaration]`;
}

module.exports = { buildPrompt };
