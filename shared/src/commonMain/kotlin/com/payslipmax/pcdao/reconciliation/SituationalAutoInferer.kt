package com.payslipmax.pcdao.reconciliation

import com.payslipmax.pdfparser.domain.ParsedPayslip
import kotlin.math.round

class SituationalAutoInferer {
    fun inferFlags(
        payslip: ParsedPayslip,
        allPayslips: List<ParsedPayslip> = emptyList(),
    ): InferredSituationalFlags {
        val basicPay = payslip.earnings.basicPay
        val da = payslip.earnings.dearnessAllowance
        val daPercent = computeDaPercentage(basicPay, da)
        val rankLevel = inferRankLevel(basicPay)
        val isPeaceHigher = payslip.earnings.transportAllowance >= 7200.0
        val isField = payslip.earnings.fieldAllowance > 0 || payslip.earnings.riskHardshipAllowance > 0
        val isHighDsop = (payslip.deductions.dsopSubscription * 12.0) > 500000.0
        val hasCea = payslip.earnings.childrenEducationAllowance > 0 || payslip.earnings.arrearsCea > 0
        val hasHra = payslip.earnings.houseRentAllowance > 0 || payslip.earnings.arrearsHra > 0
        val hasGovtAccomm = payslip.deductions.licenseFee > 0
        val isPromoEligible = inferPromotionEligibility(payslip, allPayslips)
        val promoAdvisory = if (isPromoEligible) PROMOTION_ADVISORY else null

        val isNpa = payslip.earnings.npa > 0 || payslip.earnings.nonPracticingAllowance > 0
        val isCfaa = payslip.earnings.fieldAllowance in 10500.0..13125.0
        val isCmfaa = payslip.earnings.fieldAllowance in 6300.0..7875.0
        val isTechnical = payslip.earnings.technicalPay > 0 || payslip.earnings.technicalAllowance > 0
        val isTlc = isField && hasHra
        val isFullMonthLeave = payslip.earnings.daysWorked == 0

        return InferredSituationalFlags(
            inferredRankLevel = rankLevel,
            inferredDaPercent = daPercent,
            inferredDaCrossed50 = daPercent >= 50.0,
            inferredPeaceHigher = isPeaceHigher,
            inferredField = isField,
            inferredHighDsop = isHighDsop,
            inferredCeaActive = hasCea,
            inferredHraActive = hasHra,
            inferredGovtAccomm = hasGovtAccomm,
            inferredPromotionEligible = isPromoEligible,
            inferredPromotionAdvisory = promoAdvisory,
            inferredNpaActive = isNpa,
            inferredCfaaActive = isCfaa,
            inferredCmfaaActive = isCmfaa,
            inferredTechnicalActive = isTechnical,
            inferredTwoLocationConcession = isTlc,
            inferredFullMonthLeave = isFullMonthLeave,
        )
    }

    fun inferActiveContext(
        payslip: ParsedPayslip,
        allPayslips: List<ParsedPayslip> = emptyList(),
    ): ActiveSituationalContext {
        val flags = inferFlags(payslip, allPayslips)
        val tileIds = mutableSetOf<String>()
        val specializedFactors = mutableSetOf<SpecializedMilitaryFactor>()

        inferPostingTiles(flags, tileIds)
        inferHousingTiles(flags, tileIds)
        inferFundAndCeaTiles(flags, tileIds)
        inferCadreAndLeaveTiles(flags, tileIds)
        inferSpecializedFactors(payslip, flags, specializedFactors)

        if (flags.inferredPromotionEligible) {
            tileIds.add(SituationalTileKeys.PROMOTION_ACTIVE)
        }

        val childrenCount = if (flags.inferredCeaActive) 2 else 0

        return ActiveSituationalContext(
            inferredFlags = flags,
            activeTileIds = tileIds,
            activeSpecializedFactors = specializedFactors,
            numberOfChildrenCea = childrenCount,
        )
    }

    fun inferPromotionEligibility(
        payslip: ParsedPayslip,
        allPayslips: List<ParsedPayslip> = emptyList(),
    ): Boolean {
        val basicPay = payslip.earnings.basicPay
        val tenureYears = calculateServiceTenureYears(payslip, allPayslips)
        val rank = inferRankLevel(basicPay)

        val isLtToCapt = (rank == "10" && tenureYears in 1..3 && basicPay in 57800.0..61300.0)
        val isCaptToMajor = ((rank == "10" || rank == "10B") && tenureYears in 5..7 && basicPay in 61300.0..73200.0)
        val isMajorToLtCol = (rank == "11" && tenureYears in 12..14 && basicPay in 69400.0..121200.0)
        val isLtColToCol = (rank == "12A" && tenureYears in 25..27 && basicPay in 121200.0..212400.0)

        return isLtToCapt || isCaptToMajor || isMajorToLtCol || isLtColToCol
    }

    private fun calculateServiceTenureYears(
        payslip: ParsedPayslip,
        allPayslips: List<ParsedPayslip>,
    ): Int {
        if (allPayslips.size < 2) return 0
        val sorted = allPayslips.sortedWith(compareBy({ it.year }, { it.monthNum }))
        val oldest = sorted.first()
        val monthsDiff = (payslip.year - oldest.year) * 12 + (payslip.monthNum - oldest.monthNum)
        return (monthsDiff / 12).coerceAtLeast(0)
    }

    private fun inferPostingTiles(
        flags: InferredSituationalFlags,
        tileIds: MutableSet<String>,
    ) {
        if (flags.inferredCfaaActive) {
            tileIds.add(SituationalTileKeys.POST_FIELD_CFAA)
        } else if (flags.inferredCmfaaActive) {
            tileIds.add(SituationalTileKeys.POST_FIELD_CMFAA)
        } else if (flags.inferredField) {
            tileIds.add(SituationalTileKeys.POST_FIELD_HAFAA)
        } else if (flags.inferredPeaceHigher) {
            tileIds.add(SituationalTileKeys.POST_PEACE_HIGHER)
        } else {
            tileIds.add(SituationalTileKeys.POST_PEACE_OTHER)
        }
    }

    private fun inferHousingTiles(
        flags: InferredSituationalFlags,
        tileIds: MutableSet<String>,
    ) {
        if (flags.inferredGovtAccomm) {
            tileIds.add(SituationalTileKeys.HOUSE_GOVT_MQ)
        } else if (flags.inferredHraActive) {
            tileIds.add(SituationalTileKeys.HOUSE_FAMILY_SPR)
        }
        if (flags.inferredTwoLocationConcession) {
            tileIds.add(SituationalTileKeys.HOUSE_TWO_LOCATION_CONCESSION)
        }
    }

    private fun inferFundAndCeaTiles(
        flags: InferredSituationalFlags,
        tileIds: MutableSet<String>,
    ) {
        if (flags.inferredHighDsop) {
            tileIds.add(SituationalTileKeys.DSOP_HIGH_PACING)
        }
        if (flags.inferredCeaActive) {
            tileIds.add(SituationalTileKeys.CEA_TWO_CHILDREN)
        }
    }

    private fun inferCadreAndLeaveTiles(
        flags: InferredSituationalFlags,
        tileIds: MutableSet<String>,
    ) {
        if (flags.inferredNpaActive) {
            tileIds.add(SituationalTileKeys.CADRE_AMC_NPA)
        }
        if (flags.inferredTechnicalActive) {
            tileIds.add(SituationalTileKeys.CADRE_TECHNICAL_OFFICER)
        }
        if (flags.inferredFullMonthLeave) {
            tileIds.add(SituationalTileKeys.LEAVE_FULL_MONTH)
        }
    }

    private fun inferSpecializedFactors(
        payslip: ParsedPayslip,
        flags: InferredSituationalFlags,
        specializedFactors: MutableSet<SpecializedMilitaryFactor>,
    ) {
        if (payslip.earnings.specialForcesPay > 0 || payslip.deductions.recSpecialForces > 0) {
            specializedFactors.add(SpecializedMilitaryFactor.MARCOS_SPECIAL_FORCES)
        }
        if (payslip.earnings.riskHardshipAllowance >= 42500.0) {
            specializedFactors.add(SpecializedMilitaryFactor.SIACHEN_GLACIER)
        }
        if (flags.inferredNpaActive) {
            specializedFactors.add(SpecializedMilitaryFactor.NON_PRACTICING_ALLOWANCE_AMC)
        }
        if (flags.inferredTechnicalActive) {
            specializedFactors.add(SpecializedMilitaryFactor.TECHNICAL_ALLOWANCE)
        }
    }

    private fun computeDaPercentage(
        basicPay: Double,
        da: Double,
    ): Double {
        if (basicPay <= 0.0) return 0.0
        val rawRate = (da / basicPay) * 100.0
        return round(rawRate * 10.0) / 10.0
    }

    private fun inferRankLevel(basicPay: Double): String? {
        val payInt = basicPay.toInt()
        val level12ACells = setOf(121200, 124800, 128500, 132400, 136400, 140400, 140500, 144700, 149000, 153500)
        val level11Cells = setOf(69400, 71500, 73600, 75800, 78100, 80400, 82800, 85300, 87900, 90500)
        val level10Cells = setOf(56100, 57800, 59500, 61300, 63100, 65000, 67000, 69000, 71100, 73200)

        return when {
            payInt in level12ACells -> "12A"
            payInt in level11Cells -> "11"
            payInt in level10Cells -> "10"
            payInt in 56100..69000 -> "10"
            payInt in 69400..90500 -> "11"
            payInt in 121200..167700 -> "12A"
            payInt in 167800..215900 -> "13"
            payInt >= 216000 -> "14"
            else -> null
        }
    }

    companion object {
        const val PROMOTION_ADVISORY: String =
            "Substantive Promotion Approaching: Compare Option 1 vs Option 2 to maximize 36-month pay."
    }
}
