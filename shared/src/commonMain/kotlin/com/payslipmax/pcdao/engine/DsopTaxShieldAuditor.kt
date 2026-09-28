package com.payslipmax.pcdao.engine

import com.payslipmax.pcdao.model.AuditDiscrepancy
import com.payslipmax.pcdao.model.DiscrepancySeverity
import com.payslipmax.pcdao.model.DiscrepancyType
import kotlin.math.roundToLong

class DsopTaxShieldAuditor {
    fun audit(request: DsopTaxShieldRequest): DsopTaxShieldResult {
        val annualProjected =
            if (request.annualSubscription > 0.0) {
                request.annualSubscription
            } else {
                request.monthlySubscription * 12
            }

        val excess = (annualProjected - request.annualTaxExemptLimit).coerceAtLeast(0.0)
        val taxableInterest = (excess * request.interestRate * 100.0).roundToLong() / 100.0
        val taxDrag = (taxableInterest * request.marginalTaxRate * 100.0).roundToLong() / 100.0

        val daysToRetire = resolveDaysToRetirement(request)
        val hasActiveSubscription = request.monthlySubscription > 0.0 || (request.annualSubscription > 0.0)
        val stoppageViolated = daysToRetire != null && daysToRetire <= MANDATORY_STOPPAGE_DAYS && hasActiveSubscription

        val discrepancies = mutableListOf<AuditDiscrepancy>()
        if (excess > 0.0) {
            discrepancies.add(buildTaxExposureDiscrepancy(request, annualProjected, excess, taxableInterest))
        }
        if (stoppageViolated) {
            discrepancies.add(buildRetirementStoppageDiscrepancy(request, daysToRetire))
        }

        return DsopTaxShieldResult(
            monthlySubscription = request.monthlySubscription,
            annualProjectedSubscription = annualProjected,
            taxExemptLimit = request.annualTaxExemptLimit,
            excessContribution = excess,
            taxableInterest = taxableInterest,
            estimatedTaxDrag = taxDrag,
            isRetirementStoppageViolated = stoppageViolated,
            daysToRetirement = daysToRetire,
            discrepancies = discrepancies,
        )
    }

    fun auditSubscription(
        monthlySubscription: Double,
        monthsToRetirement: Int? = null,
        interestRate: Double = DEFAULT_INTEREST_RATE,
    ): DsopTaxShieldResult {
        val req =
            DsopTaxShieldRequest(
                monthlySubscription = monthlySubscription,
                annualSubscription = monthlySubscription * 12,
                interestRate = interestRate,
                monthsToRetirement = monthsToRetirement,
            )
        return audit(req)
    }

    private fun resolveDaysToRetirement(request: DsopTaxShieldRequest): Int? {
        if (request.daysToRetirement != null) return request.daysToRetirement
        if (request.monthsToRetirement != null) return request.monthsToRetirement * 30
        if (request.retirementDate != null && request.asOfDate != null) {
            return PcdaoDateUtils.daysBetween(request.asOfDate, request.retirementDate).coerceAtLeast(0)
        }
        return null
    }

    private fun buildTaxExposureDiscrepancy(
        request: DsopTaxShieldRequest,
        annual: Double,
        excess: Double,
        taxableInterest: Double,
    ): AuditDiscrepancy =
        AuditDiscrepancy(
            id = "TAX_EXPOSURE_DSOPF_5L_CAP",
            title = "Annual DSOPF Contribution Exceeds ₹5,00,000 Tax-Free Ceiling",
            type = DiscrepancyType.TAX_EXPOSURE,
            severity = DiscrepancySeverity.WARNING,
            monthlyImpact = request.monthlySubscription,
            annualImpact = taxableInterest,
            drawnAmount = excess,
            entitledAmount = request.annualTaxExemptLimit,
            netDue = -taxableInterest,
            authority = "Income Tax Act 1961, Section 10(11) Second Proviso (Finance Act 2021); Rule 96(2)",
            explanation =
                "Annual subscription of ₹${annual.toLong()} exceeds the statutory ₹5,00,000 tax-free limit by " +
                    "₹${excess.toLong()}. Interest earned on excess (₹${taxableInterest.toLong()} at " +
                    "${(request.interestRate * 100)}% p.a.) is fully taxable as Income from Other Sources.",
            recommendedAction =
                "Reduce monthly DSOPF subscription via PCDA(O) variation window (permissible twice a year) to cap " +
                    "annual contributions under ₹5,00,000, or declare interest in ITR to avoid penal interest under Sec 234B/C.",
            relevantRuleId = "DSOP_002",
        )

    private fun buildRetirementStoppageDiscrepancy(
        request: DsopTaxShieldRequest,
        daysToRetire: Int,
    ): AuditDiscrepancy =
        AuditDiscrepancy(
            id = "HAZARD_DSOPF_RETIREMENT_STOPPAGE_VIOLATION",
            title = "Mandatory DSOPF Stoppage Alarm (Rule 14: Retirement in $daysToRetire Days)",
            type = DiscrepancyType.RECOVERY_HAZARD,
            severity = DiscrepancySeverity.CRITICAL,
            monthlyImpact = request.monthlySubscription,
            annualImpact = request.monthlySubscription * 3,
            drawnAmount = request.monthlySubscription,
            entitledAmount = 0.0,
            netDue = 0.0,
            authority = "Rule 14, Defence Services Officers Provident Fund (DSOPF) Rules; Pay & Allowances Handbook Chapter 19",
            explanation =
                "Officer is due for retirement in $daysToRetire days (within the mandatory 3-month/90-day window). " +
                    "Active subscription of ₹${request.monthlySubscription.toLong()} violates Rule 14 and will delay " +
                    "issuance of DSOPF Final Settlement (Form 19) and pension payment order.",
            recommendedAction =
                "Immediately submit Part II Order casualty for DSOPF subscription stoppage to Task Desk / PCDA(O) Pune " +
                    "to prevent delay in pension and terminal fund release.",
            relevantRuleId = "DSOP_FINAL_001",
        )

    companion object {
        const val DEFAULT_INTEREST_RATE: Double = 0.071 // 7.1% p.a.
        const val MANDATORY_STOPPAGE_DAYS: Int = 90 // 3 months
    }
}
