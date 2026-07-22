package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.ParsedPayslip

enum class DataConfidenceLevel {
    HIGH,
    MODERATE,
    PRELIMINARY,
}

data class RetirementPlannerResult(
    val confidenceLevel: DataConfidenceLevel,
    val basicPay: Double,
    val militaryServicePay: Double,
    val dearnessAllowance: Double,
    val daPercentage: Double,
    val reckonableEmoluments: Double,
    val basicPension: Double,
    val netMonthlyPensionFull: Double,
    val netMonthlyPensionCommuted50: Double,
    val commutedLumpSum50: Double,
    val retirementGratuity: Double,
    val leaveEncashment: Double,
    val dsopBalance: Double,
    val agifMaturity: Double,
    val totalDay1Corpus: Double,
    val qualifyingYears: Double,
    val ageNextBirthday: Int,
    val commutationScenarios: List<RetirementCalculatorEngine.CommutationScenario>,
    val isPmr: Boolean = false,
    val officerName: String? = null,
    val officerRank: String? = null,
    val taxFreeCorpus: Double = 0.0,
    val taxableMonthlyPension: Double = 0.0,
    val commutationBreakEvenRoi: Double = 0.0,
)

object RetirementPlannerResultBuilder {
    fun build(
        payslip: ParsedPayslip?,
        overrideQualifyingYears: Double? = null,
        overrideAgeNextBirthday: Int? = null,
        overrideLeaveDays: Int? = null,
        overrideDsopBalance: Double? = null,
        isPmr: Boolean = false,
    ): RetirementPlannerResult {
        val basic = payslip?.earnings?.basicPay ?: 100000.0
        val msp = payslip?.earnings?.militaryServicePay ?: 15500.0
        val da = payslip?.earnings?.dearnessAllowance ?: 50000.0
        val rawOfficerName = payslip?.officer?.name

        val (rank, name) = parseRankAndName(rawOfficerName)

        val daPercentage = if (basic > 0.0) (da / basic) * 100.0 else 50.0

        val ageNextBirthday =
            overrideAgeNextBirthday
                ?: RetirementYearResolver.estimateAgeNextBirthday(basic, rawOfficerName)

        val qualifyingYears =
            overrideQualifyingYears
                ?: RetirementYearResolver.DEFAULT_QUALIFYING_YEARS_FULL_PENSION

        val leaveDays = overrideLeaveDays ?: RetirementCalculatorEngine.LEAVE_MAX_DAYS

        val dsopBalance =
            overrideDsopBalance
                ?: payslip?.taxAndSavings?.dsopFund?.closingBalance
                ?: 0.0

        val confidence =
            when {
                payslip != null && dsopBalance > 0.0 -> DataConfidenceLevel.HIGH
                payslip != null -> DataConfidenceLevel.MODERATE
                else -> DataConfidenceLevel.PRELIMINARY
            }

        val basicPension = RetirementCalculatorEngine.retiringPension(basic, msp)
        val reckonableEmoluments = basic + msp

        val fullMonthly = RetirementCalculatorEngine.calculateNetMonthlyPension(basicPension, 0.0, daPercentage)
        val commuted50Monthly = RetirementCalculatorEngine.calculateNetMonthlyPension(basicPension, 0.50, daPercentage)

        val factor = RetirementCalculatorEngine.commutationFactor(ageNextBirthday) ?: 8.678
        val commutedLumpSum50 = RetirementCalculatorEngine.commutedLumpSum(basicPension, 0.50, factor)

        val gratuity = RetirementCalculatorEngine.retirementGratuity(basic, da, qualifyingYears, msp)
        val leaveEncashment = RetirementCalculatorEngine.leaveEncashment(basic, da, leaveDays)

        val agifMaturity = RetirementCalculatorEngine.calculateAgifMaturity(1_000_000.0)
        val totalDay1Corpus = dsopBalance + gratuity + leaveEncashment + agifMaturity + commutedLumpSum50

        val scenarios = RetirementCalculatorEngine.calculateCommutationMatrix(basicPension, ageNextBirthday, daPercentage)
        val surrenderedMonthly = fullMonthly - commuted50Monthly
        val breakEvenRoi = if (commutedLumpSum50 > 0.0) (surrenderedMonthly * 12.0 / commutedLumpSum50) * 100.0 else 0.0

        return RetirementPlannerResult(
            confidenceLevel = confidence,
            basicPay = basic,
            militaryServicePay = msp,
            dearnessAllowance = da,
            daPercentage = daPercentage,
            reckonableEmoluments = reckonableEmoluments,
            basicPension = basicPension,
            netMonthlyPensionFull = fullMonthly,
            netMonthlyPensionCommuted50 = commuted50Monthly,
            commutedLumpSum50 = commutedLumpSum50,
            retirementGratuity = gratuity,
            leaveEncashment = leaveEncashment,
            dsopBalance = dsopBalance,
            agifMaturity = agifMaturity,
            totalDay1Corpus = totalDay1Corpus,
            qualifyingYears = qualifyingYears,
            ageNextBirthday = ageNextBirthday,
            commutationScenarios = scenarios,
            isPmr = isPmr,
            officerName = name,
            officerRank = rank,
            taxFreeCorpus = totalDay1Corpus,
            taxableMonthlyPension = commuted50Monthly,
            commutationBreakEvenRoi = breakEvenRoi,
        )
    }

    private fun parseRankAndName(raw: String?): Pair<String?, String?> {
        if (raw.isNullOrBlank()) return null to null
        val tokens = raw.trim().split("\\s+".toRegex())
        return if (tokens.size > 1) {
            tokens.first() to tokens.drop(1).joinToString(" ")
        } else {
            null to tokens.first()
        }
    }
}
