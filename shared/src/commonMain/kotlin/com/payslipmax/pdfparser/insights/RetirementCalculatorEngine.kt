package com.payslipmax.pdfparser.insights

/**
 * Offline estimators for post-retirement entitlements of commissioned officers, gated by
 * [com.payslipmax.pdfparser.subscription.FeatureGate.RETIREMENT_CALCULATORS].
 *
 * IMPORTANT: every figure is an **estimate from current pay** — a payslip carries current, not
 * last-drawn-at-retirement, emoluments — and must be verified with PAO / PCDA(O). Formulas follow the
 * CCS / Defence pension rules (7th CPC era). Pure and gate-agnostic (gating is a caller concern).
 *
 * Sources (retrieved Jul 2026):
 * - Retiring pension 50% of reckonable emoluments: PCDA Pension; DESW.
 * - Retirement gratuity ¼·emoluments·six-monthly-periods, cap 16.5× and ₹25L (post 01.01.2024): gconnect; CGDA Defence Pension Guide.
 * - Commutation (armed forces up to 50%), lump = pension·frac·12·factor, CCS Table II factors: pensionersportal; CCS Commutation Rules 1981.
 * - Leave encashment (Basic+DA)/30·days, max 300 days: 7th CPC (igecorner).
 */
object RetirementCalculatorEngine {
    /** Retiring pension = 50% of reckonable emoluments (Basic Pay + Military Service Pay). DA is not reckonable. */
    fun retiringPension(
        basicPay: Double,
        militaryServicePay: Double,
    ): Double = 0.5 * (basicPay + militaryServicePay)

    // --- Retirement gratuity ---
    const val GRATUITY_MAX_EMOLUMENT_MULTIPLE = 16.5
    const val GRATUITY_CEILING = 2_500_000.0

    /**
     * ¼ × emoluments (Basic + DA) × completed six-monthly periods of qualifying service, capped at
     * [GRATUITY_MAX_EMOLUMENT_MULTIPLE]× emoluments and the absolute [GRATUITY_CEILING].
     */
    fun retirementGratuity(
        basicPay: Double,
        dearnessAllowance: Double,
        qualifyingYears: Double,
    ): Double {
        if (qualifyingYears <= 0.0) return 0.0
        val emoluments = basicPay + dearnessAllowance
        val sixMonthlyPeriods = (qualifyingYears * 2).toInt()
        val raw = 0.25 * emoluments * sixMonthlyPeriods
        return minOf(raw, GRATUITY_MAX_EMOLUMENT_MULTIPLE * emoluments, GRATUITY_CEILING)
    }

    // --- Commutation of pension ---

    /** Armed-forces commissioned officers may commute up to 50% of pension (civil cap is 40%). */
    const val MAX_COMMUTE_FRACTION = 0.5

    /**
     * CCS commutation Table II factors by *age next birthday*. This table is fixed (effective 01.01.1996,
     * used across 6th/7th CPC), so it is a stable constant rather than a pay-commission variable. Only the
     * officer-retirement band is covered; ages outside it return `null` so the caller fails loud rather
     * than fabricating a factor.
     */
    val COMMUTATION_FACTORS: Map<Int, Double> =
        mapOf(
            55 to 8.627, 56 to 8.572, 57 to 8.512, 58 to 8.446, 59 to 8.371,
            60 to 8.287, 61 to 8.194, 62 to 8.093, 63 to 7.982, 64 to 7.862, 65 to 7.731,
        )

    fun commutationFactor(ageNextBirthday: Int): Double? = COMMUTATION_FACTORS[ageNextBirthday]

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

    // --- Leave encashment ---
    const val LEAVE_MAX_DAYS = 300

    /** Encashment on retirement = (Basic + DA) / 30 × days, capped at [LEAVE_MAX_DAYS] days. */
    fun leaveEncashment(
        basicPay: Double,
        dearnessAllowance: Double,
        days: Int,
    ): Double {
        if (days <= 0) return 0.0
        return (basicPay + dearnessAllowance) / 30.0 * days.coerceAtMost(LEAVE_MAX_DAYS)
    }
}
