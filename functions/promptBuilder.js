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

  const anomalySection =
    anomalies.length > 0
      ? anomalies.map((a) => `  • [${a.type}] ${a.description}`).join("\n")
      : "  • No anomalies detected this month.";

  const historySection =
    history.length > 0
      ? history
          .slice(-6)
          .map((h) => `  ${h.year}-${String(h.monthNum).padStart(2, "0")}: Net ₹${Math.round(h.netPay)}, Gross ₹${Math.round(h.grossPay)}, Tax ₹${Math.round(h.incomeTax)}`)
          .join("\n")
      : "  • No historical data provided.";

  return `You are a senior Chartered Accountant specialising in Indian Defence Services pay regulations (6th and 7th Pay Commission). Analyse the following monthly payslip data and produce a structured, professional financial report in Markdown.

## This Month's Pay Summary
- Gross Pay: ₹${Math.round(earnings.grossPay ?? summary.grossPay)}
- Net Take-Home: ₹${Math.round(summary.netRemittance)}
- Income Tax Deducted: ₹${Math.round(deductions.incomeTax)}
- DSOP Subscription: ₹${Math.round(deductions.dsopSubscription)}

## Financial Health Metrics
- Monthly Savings Rate: ${monthlySavingRate.toFixed(1)}% (target ≥ 20%)
- Effective Tax Ratio: ${taxRatio.toFixed(1)}%
- Overall Health Score: ${healthScore}/100

## Anomalies Flagged by Local Audit Engine
${anomalySection}

## 6-Month Historical Context
${historySection}

## Your Report Must Include (in Markdown with ## headings)
1. **Executive Summary** — 2-3 sentence assessment of the officer's financial health this month.
2. **Tax Optimisation** — Specific, actionable 80C/80D/NPS recommendations based on the numbers above.
3. **DSOP Advisory** — Whether the current DSOP contribution is tax-optimised (below ₹41,666/month limit for tax-free interest).
4. **Anomaly Analysis** — For each flagged anomaly, explain the likely cause and corrective action under CCS/Pay Commission rules.
5. **Action Items** — Numbered list of 3-5 concrete next steps the officer should take before next payslip.

Be precise, cite rupee amounts, and write in formal but accessible English. Do not make up numbers not present above.`;
}

module.exports = { buildPrompt };
