package com.payslipmax.pdfparser.insights

/**
 * Resolves retirement parameters (Superannuation Age, Default Qualifying Service, Age Next Birthday)
 * based on Armed Forces Officer ranks and parsed payslip data.
 */
object RetirementYearResolver {
    const val DEFAULT_QUALIFYING_YEARS_FULL_PENSION = 20.0
    const val DEFAULT_SUPERANNUATION_AGE = 54 // Colonel & below

    /**
     * Resolves superannuation age by officer rank.
     */
    fun resolveSuperannuationAge(rank: String?): Int {
        if (rank == null) return DEFAULT_SUPERANNUATION_AGE
        val upperRank = rank.uppercase()
        return when {
            upperRank.contains("GEN") && !upperRank.contains("MAJ") -> 60
            upperRank.contains("MAJ GEN") || upperRank.contains("MAJOR GENERAL") -> 58
            upperRank.contains("BRIG") || upperRank.contains("BRIGADIER") -> 56
            upperRank.contains("COL") || upperRank.contains("COLONEL") -> 54
            upperRank.contains("LT COL") || upperRank.contains("LIEUTENANT COLONEL") -> 54
            upperRank.contains("MAJ") || upperRank.contains("MAJOR") -> 54
            upperRank.contains("CAPT") || upperRank.contains("CAPTAIN") -> 54
            upperRank.contains("LT") || upperRank.contains("LIEUTENANT") -> 54
            else -> DEFAULT_SUPERANNUATION_AGE
        }
    }

    /**
     * Estimates age next birthday from current parsed basic pay / pay level if explicit DOB is unparsed.
     */
    fun estimateAgeNextBirthday(
        basicPay: Double,
        rank: String?,
    ): Int {
        val superAge = resolveSuperannuationAge(rank)
        // High basic pay indicates longer service length nearing retirement age
        return when {
            basicPay >= 180000.0 -> superAge
            basicPay >= 140000.0 -> superAge - 2
            basicPay >= 110000.0 -> superAge - 5
            else -> 48
        }
    }
}
