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
    ? `Gross ₹${Math.round(prevMonth.grossPay)}, Net ₹${Math.round(prevMonth.netPay)}, Tax ₹${Math.round(prevMonth.incomeTax)} (${prevMonth.year}-${String(prevMonth.monthNum).padStart(2, "0")})`
    : "No historical data available.";

  return `You are a senior Chartered Accountant specialising in Indian Defence pay regulations (6th/7th Pay Commission). Analyse the payslip data and write a concise financial report in Markdown.
Strictly format each section using ONLY bullet points (no paragraphs). Keep the output under 400 words so it does not get truncated.

## This Month's Pay Summary
- Gross Pay: ₹${Math.round(earnings.grossPay ?? summary.grossPay)}
- Net Take-Home: ₹${Math.round(summary.netRemittance)}
- Income Tax Deducted: ₹${Math.round(deductions.incomeTax)}
- DSOP Subscription: ₹${Math.round(deductions.dsopSubscription)}
- Savings Rate: ${monthlySavingRate.toFixed(1)}% (target ≥ 20%), Tax Ratio: ${taxRatio.toFixed(1)}%, Health Score: ${healthScore}/100

## Previous Month Context
- Previous month pay details: ${comparisonText}

## Flagged Anomalies
${anomalySection}

## Your Report Must Include (use ## headings and ONLY bullet points):
1. **Crystal Clear Takeaway** — 1-2 bullet points stating the single most critical financial action/status.
2. **Month-on-Month Comparison** — Bullet points with exact rupee delta and % change for Gross, Net, and Tax.
3. **Tax Optimisation & DSOP** — Actions for 80C/80D/NPS and check if DSOP is below ₹41,666/month limit.
4. **Audit & Anomaly Analysis** — Explain flagged anomalies and corrective actions under Defence pay rules.
5. **Action Plan** — 3-5 concrete next steps.`;
}

module.exports = { buildPrompt };
