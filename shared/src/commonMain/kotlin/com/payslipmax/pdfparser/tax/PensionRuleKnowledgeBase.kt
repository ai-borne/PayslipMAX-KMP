package com.payslipmax.pdfparser.tax

/**
 * Statutory knowledge base for Defence Pension, Gratuity, Commutation, and Encashment rules.
 * Sourced from PCDA (P) Allahabad, CGDA Defence Pension Guide, 7th CPC recommendations,
 * and Income Tax Act Section 10 exemptions for Armed Forces Personnel.
 */
object PensionRuleKnowledgeBase {
    const val GRATUITY_CEILING = 2_500_000.0 // ₹25 Lakhs post-01.01.2024 (when DA >= 50%)
    const val GRATUITY_MAX_EMOLUMENT_MULTIPLE = 16.5
    const val MAX_COMMUTE_FRACTION_DEFENCE = 0.50 // 50% for Armed Forces Officers
    const val LEAVE_MAX_DAYS = 300
    const val AGIF_EXTENDED_COVER_DEDUCTION = 160_000.0 // ₹1.6 Lakhs deducted for 26-year extended cover
    const val DISABILITY_100_PERCENT_RATE = 0.30 // 30% of last drawn emoluments for 100% disability

    /**
     * Official CCS Commutation Table II factors by age next birthday (Ages 20 to 67).
     * Sourced from CCS (Commutation of Pension) Rules and PCDA Defence Pension Tables.
     */
    val COMMUTATION_FACTORS: Map<Int, Double> =
        mapOf(
            20 to 9.188, 21 to 9.187, 22 to 9.186, 23 to 9.185, 24 to 9.184, 25 to 9.183,
            26 to 9.182, 27 to 9.180, 28 to 9.178, 29 to 9.176, 30 to 9.173,
            31 to 9.169, 32 to 9.164, 33 to 9.159, 34 to 9.152, 35 to 9.145,
            36 to 9.136, 37 to 9.126, 38 to 9.116, 39 to 9.103, 40 to 9.090,
            41 to 9.075, 42 to 9.059, 43 to 9.040, 44 to 9.019, 45 to 8.996,
            46 to 8.971, 47 to 8.943, 48 to 8.913, 49 to 8.881, 50 to 8.846,
            51 to 8.808, 52 to 8.768, 53 to 8.724, 54 to 8.678, 55 to 8.627,
            56 to 8.572, 57 to 8.512, 58 to 8.446, 59 to 8.371, 60 to 8.287,
            61 to 8.194, 62 to 8.093, 63 to 7.982, 64 to 7.862, 65 to 7.731,
            66 to 7.591, 67 to 7.431,
        )

    fun getCommutationFactor(ageNextBirthday: Int): Double? = COMMUTATION_FACTORS[ageNextBirthday]

    /**
     * Section 10 Income Tax Exemption Info for Defence Personnel.
     */
    object TaxExemptions {
        const val GRATUITY_SECTION = "Sec 10(10)(i) - 100% Tax Exempt for Defence"
        const val COMMUTATION_SECTION = "Sec 10(10)(i) - 100% Tax Exempt for Defence"
        const val LEAVE_ENCASHMENT_SECTION = "Sec 10(10AA)(i) - 100% Tax Exempt for Defence"
        const val DISABILITY_PENSION_SECTION = "100% Non-Taxable for Armed Forces"
        const val GALLANTRY_PENSION_SECTION = "Sec 10(18)(i) - 100% Tax Exempt"
    }
}
