package com.payslipmax.pcdao.reconciliation

import kotlinx.serialization.Serializable

@Serializable
enum class SituationalCategory {
    POSTING,
    HOUSING,
    CHILDREN_CEA,
    CAREER_PROMOTION,
    FUNDS_LTC,
}

@Serializable
data class InferredSituationalFlags(
    val inferredRankLevel: String? = null,
    val inferredDaPercent: Double = 0.0,
    val inferredDaCrossed50: Boolean = false,
    val inferredPeaceHigher: Boolean = false,
    val inferredField: Boolean = false,
    val inferredHighDsop: Boolean = false,
    val inferredCeaActive: Boolean = false,
    val inferredHraActive: Boolean = false,
    val inferredGovtAccomm: Boolean = false,
)

@Serializable
enum class SpecializedMilitaryFactor {
    SIACHEN_GLACIER,
    HIGH_ALTITUDE_III,
    MARCOS_SPECIAL_FORCES,
    DIVYANG_CHILD,
    DIVYANG_OFFICER,
    GALLANTRY_AWARD,
    COMPOSITE_TRANSFER_GRANT,
    FLYING_ALLOWANCE,
    TECHNICAL_ALLOWANCE,
}

@Serializable
data class SituationalTile(
    val id: String,
    val category: SituationalCategory,
    val title: String,
    val description: String,
    val valuePreview: String,
    val isAutoInferred: Boolean = false,
    val isSelected: Boolean = false,
    val isRadioStyle: Boolean = false,
)

@Serializable
data class SpecializedFactorEntry(
    val factor: SpecializedMilitaryFactor,
    val title: String,
    val description: String,
    val rateDescription: String,
    val statutoryAuthority: String,
    val isActive: Boolean = false,
)

@Serializable
data class ActiveSituationalContext(
    val inferredFlags: InferredSituationalFlags = InferredSituationalFlags(),
    val activeTileIds: Set<String> = emptySet(),
    val activeSpecializedFactors: Set<SpecializedMilitaryFactor> = emptySet(),
    val sprCityTier: String = "Y",
    val customDaPercent: Double? = null,
    val monthsToRetirement: Int? = null,
    val numberOfChildrenCea: Int = 0,
    val hasHostelChild: Boolean = false,
    val rejectedDo2Number: String? = null,
    val rejectedDo2Date: String? = null,
)

object SituationalTileKeys {
    const val POST_PEACE_HIGHER = "post_peace_higher"
    const val POST_PEACE_OTHER = "post_peace_other"
    const val POST_FIELD_HAFAA = "post_field_hafaa"
    const val POST_SIACHEN = "post_siachen"
    const val POST_SDA_NE = "post_sda_ne"
    const val HOUSE_GOVT_MQ = "house_govt_mq"
    const val HOUSE_FAMILY_SPR = "house_family_spr"
    const val HOUSE_LIVING_OUT_NAC = "house_living_out_nac"
    const val HOUSE_GOVT_CONVEYANCE = "house_govt_conveyance"
    const val CEA_NONE = "cea_none"
    const val CEA_ONE_CHILD = "cea_one_child"
    const val CEA_TWO_CHILDREN = "cea_two_children"
    const val CEA_HOSTEL = "cea_hostel"
    const val PROMOTION_ACTIVE = "promo_active"
    const val RETIRE_NEAR = "retire_near"
    const val DNI_SCHEDULED = "dni_scheduled"
    const val AVAILED_LTC = "availed_ltc"
    const val DSOP_HIGH_PACING = "dsop_high_pacing"
    const val TRANSFER_CTG = "transfer_ctg"

    val STATION_POSTING_KEYS = setOf(POST_FIELD_HAFAA, POST_PEACE_HIGHER, POST_PEACE_OTHER, POST_SIACHEN)
    val PEACE_STATION_KEYS = setOf(POST_PEACE_HIGHER, POST_PEACE_OTHER)
    val CEA_CHILD_KEYS = setOf(CEA_NONE, CEA_ONE_CHILD, CEA_TWO_CHILDREN)
}

@Serializable
data class EscalatedRates(
    val daRate: Double,
    val isEscalated: Boolean,
    val ceaMonthlyPerChild: Double,
    val ceaAnnualPerChild: Double,
    val hostelSubsidyMonthly: Double,
    val hostelSubsidyAnnual: Double,
    val dressAllowanceAnnual: Double,
    val hraRates: Map<String, Double>,
    val siachenMonthlyRate: Double,
    val hafaMonthlyRate: Double,
)

@Serializable
data class ResolvedEntitlement(
    val allowanceKey: String,
    val allowanceName: String,
    val entitledMonthly: Double,
    val entitledAnnual: Double,
    val statutoryAuthority: String,
    val relevantRuleId: String,
    val explanation: String,
    val isEscalated: Boolean = false,
)

@Serializable
data class ShadowLedgerReconciliationResult(
    val lineItems: List<com.payslipmax.pcdao.model.LedgerDifferenceItem> = emptyList(),
    val discrepancies: List<com.payslipmax.pcdao.model.AuditDiscrepancy> = emptyList(),
    val totalUnclaimedAnnual: Double = 0.0,
    val totalRecoveryHazard: Double = 0.0,
    val criticalAlarmCount: Int = 0,
    val summaryMessage: String = "",
)
