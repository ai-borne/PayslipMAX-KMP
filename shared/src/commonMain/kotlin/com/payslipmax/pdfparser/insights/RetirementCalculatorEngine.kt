package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.tax.PensionRuleKnowledgeBase

/**
 * Offline estimators for post-retirement entitlements of commissioned officers, gated by
 * [com.payslipmax.pdfparser.subscription.FeatureGate.RETIREMENT_CALCULATORS].
 *
 * IMPORTANT: every figure is an **estimate from current pay** — a payslip carries current, not
 * last-drawn-at-retirement, emoluments — and must be verified with PAO / PCDA(O). Formulas follow the
 * CCS / Defence pension rules (7th CPC era). Pure and gate-agnostic (gating is a caller concern).
 *
 * Sources:
 * - PCDA (P) Allahabad Defence Pension Guide & 7th CPC.
 * - Commutation Table II (Ages 20 to 67).
 * - PCDA Rule: DR (Dearness Relief) is calculated on 100% Uncommuted Basic Pension.
 * - PCDA Rule: MSP is EXCLUDED from Leave Encashment computation.
 */
object RetirementCalculatorEngine {
    const val GRATUITY_MAX_EMOLUMENT_MULTIPLE = PensionRuleKnowledgeBase.GRATUITY_MAX_EMOLUMENT_MULTIPLE
    const val GRATUITY_CEILING = PensionRuleKnowledgeBase.GRATUITY_CEILING
    const val MAX_COMMUTE_FRACTION = PensionRuleKnowledgeBase.MAX_COMMUTE_FRACTION_DEFENCE
    const val LEAVE_MAX_DAYS = PensionRuleKnowledgeBase.LEAVE_MAX_DAYS

    val COMMUTATION_FACTORS: Map<Int, Double> get() = PensionRuleKnowledgeBase.COMMUTATION_FACTORS

    fun commutationFactor(ageNextBirthday: Int): Double? = PensionRuleKnowledgeBase.getCommutationFactor(ageNextBirthday)

    /**
     * Retiring pension = 50% of reckonable emoluments (Basic Pay + MSP + Class Pay + NPA + Special Pay).
     */
    fun retiringPension(
        basicPay: Double,
        militaryServicePay: Double,
        classPay: Double = 0.0,
        npa: Double = 0.0,
        specialPay: Double = 0.0,
    ): Double {
        val emoluments = basicPay + militaryServicePay + classPay + npa + specialPay
        return 0.5 * emoluments
    }

    /**
     * Retirement Gratuity = ¼ × emoluments (Basic + MSP + NPA + DA) × completed six-monthly periods of service,
     * capped at 16.5× emoluments and absolute ceiling of ₹25 Lakhs (post 01.01.2024).
     */
    fun retirementGratuity(
        basicPay: Double,
        dearnessAllowance: Double,
        qualifyingYears: Double,
        militaryServicePay: Double = 0.0,
        npa: Double = 0.0,
    ): Double {
        if (qualifyingYears <= 0.0) return 0.0
        val emoluments = basicPay + dearnessAllowance + militaryServicePay + npa
        val sixMonthlyPeriods = (qualifyingYears * 2).toInt()
        val raw = 0.25 * emoluments * sixMonthlyPeriods
        return minOf(raw, GRATUITY_MAX_EMOLUMENT_MULTIPLE * emoluments, GRATUITY_CEILING)
    }

    /** Commuted lump sum = monthly pension × fraction (≤ 0.5) × 12 × [factor]. */
    fun commutedLumpSum(
        monthlyPension: Double,
        fraction: Double,
        factor: Double,
    ): Double {
        if (monthlyPension <= 0.0 || factor <= 0.0) return 0.0
        return monthlyPension * fraction.coerceIn(0.0, MAX_COMMUTE_FRACTION) * 12.0 * factor
    }

    /** Monthly pension remaining after commuting [fraction] (restored after 15 years). */
    fun residualPension(
        monthlyPension: Double,
        fraction: Double,
    ): Double = monthlyPension * (1.0 - fraction.coerceIn(0.0, MAX_COMMUTE_FRACTION))

    /**
     * Net Monthly Pension Payout received by pensioner.
     * CRITICAL PCDA RULE: Dearness Relief (DR) is calculated on 100% UNCOMMUTED Basic Pension!
     */
    fun calculateNetMonthlyPension(
        basicPension: Double,
        commuteFraction: Double,
        daPercentage: Double,
    ): Double {
        if (basicPension <= 0.0) return 0.0
        val validFraction = commuteFraction.coerceIn(0.0, MAX_COMMUTE_FRACTION)
        val commutedDeduction = basicPension * validFraction
        val dearnessRelief = basicPension * (daPercentage / 100.0)
        return (basicPension - commutedDeduction) + dearnessRelief
    }

    /**
     * Leave Encashment on retirement = (Basic Pay + DA + NPA) / 30 × days, capped at 300 days.
     * CRITICAL PCDA RULE: Military Service Pay (MSP) is EXCLUDED from Leave Encashment computation!
     */
    fun leaveEncashment(
        basicPay: Double,
        dearnessAllowance: Double,
        days: Int,
        npa: Double = 0.0,
    ): Double {
        if (days <= 0) return 0.0
        return (basicPay + dearnessAllowance + npa) / 30.0 * days.coerceAtMost(LEAVE_MAX_DAYS)
    }

    /**
     * Disability Pension = 30% of last drawn emoluments for 100% disability, scaled proportionally
     * for disability percentage between 20% and 100%. 100% non-taxable for defence personnel.
     */
    fun calculateDisabilityPension(
        emoluments: Double,
        disabilityPercentage: Int,
    ): Double {
        if (emoluments <= 0.0 || disabilityPercentage < 20) return 0.0
        val validPercentage = disabilityPercentage.coerceAtMost(100) / 100.0
        val fullDisabilityPension = emoluments * PensionRuleKnowledgeBase.DISABILITY_100_PERCENT_RATE
        return fullDisabilityPension * validPercentage
    }

    /**
     * AGIF Maturity Net Payout = Accumulated Amount - Outstanding Loans - Extended Insurance Cover Deduction (₹1.6L).
     */
    fun calculateAgifMaturity(
        accumulatedBalance: Double,
        outstandingLoans: Double = 0.0,
    ): Double {
        if (accumulatedBalance <= 0.0) return 0.0
        val net = accumulatedBalance - outstandingLoans - PensionRuleKnowledgeBase.AGIF_EXTENDED_COVER_DEDUCTION
        return maxOf(0.0, net)
    }

    data class CommutationScenario(
        val fraction: Double,
        val lumpSum: Double,
        val residualPension: Double,
        val netMonthlyPayout: Double,
        val breakEvenRoiPercent: Double,
    )

    /**
     * Calculates 0%, 25%, and 50% commutation scenarios and break-even reinvestment yield %.
     */
    fun calculateCommutationMatrix(
        basicPension: Double,
        ageNextBirthday: Int,
        daPercentage: Double = 53.0,
    ): List<CommutationScenario> {
        val factor = commutationFactor(ageNextBirthday) ?: 8.5
        val fractions = listOf(0.0, 0.25, 0.50)

        val fullMonthly = calculateNetMonthlyPension(basicPension, 0.0, daPercentage)

        return fractions.map { frac ->
            val lump = commutedLumpSum(basicPension, frac, factor)
            val residual = residualPension(basicPension, frac)
            val netMonthly = calculateNetMonthlyPension(basicPension, frac, daPercentage)
            val monthlySurrendered = fullMonthly - netMonthly

            // Break-even annual post-tax ROI required on lump sum to match surrendered monthly cashflow
            val breakEvenRoi = if (lump > 0.0) (monthlySurrendered * 12.0 / lump) * 100.0 else 0.0

            CommutationScenario(
                fraction = frac,
                lumpSum = lump,
                residualPension = residual,
                netMonthlyPayout = netMonthly,
                breakEvenRoiPercent = breakEvenRoi,
            )
        }
    }
}
