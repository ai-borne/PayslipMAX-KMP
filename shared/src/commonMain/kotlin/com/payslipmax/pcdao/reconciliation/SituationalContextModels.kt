package com.payslipmax.pcdao.reconciliation

import kotlinx.serialization.Serializable

@Serializable
enum class SituationalCategory {
    POSTING_OPS,
    HOUSING_TLC,
    CHILDREN_CEA,
    DUTY_COURSES_LEAVE,
    CAREER_CADRES,
    FUNDS_RETIREMENT,
    ;

    companion object {
        val POSTING = POSTING_OPS
        val HOUSING = HOUSING_TLC
        val CAREER_PROMOTION = CAREER_CADRES
        val FUNDS_LTC = FUNDS_RETIREMENT
    }
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
    val inferredPromotionEligible: Boolean = false,
    val inferredPromotionAdvisory: String? = null,
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
    NON_PRACTICING_ALLOWANCE_AMC,
    PARACHUTE_ALLOWANCE,
    TRAINING_ALLOWANCE,
    HIGH_ALTITUDE_CAT_I,
    HIGH_ALTITUDE_CAT_II,
    ISLAND_SPECIAL_DUTY,
    TOUGH_LOCATION_ALLOWANCE,
    LANGUAGE_ALLOWANCE,
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
    const val POST_FIELD_CFAA = "post_field_cfaa"
    const val POST_FIELD_CMFAA = "post_field_cmfaa"
    const val POST_SIACHEN = "post_siachen"
    const val POST_SDA_NE = "post_sda_ne"
    const val POST_ISDA_ISLAND = "post_isda_island"

    const val HOUSE_GOVT_MQ = "house_govt_mq"
    const val HOUSE_FAMILY_SPR = "house_family_spr"
    const val HOUSE_PEACE_RETENTION = "house_peace_retention"
    const val HOUSE_SF_ACCOMMODATION = "house_sf_accommodation"
    const val HOUSE_LIVING_OUT_NAC = "house_living_out_nac"
    const val HOUSE_TWO_LOCATION_CONCESSION = "house_two_location_concession"
    const val HOUSE_GOVT_CONVEYANCE = "house_govt_conveyance"

    const val CEA_NONE = "cea_none"
    const val CEA_ONE_CHILD = "cea_one_child"
    const val CEA_TWO_CHILDREN = "cea_two_children"
    const val CEA_HOSTEL = "cea_hostel"

    const val DUTY_COURSE_LONG = "duty_course_long"
    const val DUTY_FIELD_FIRING = "duty_field_firing"
    const val DUTY_TEMPORARY_DUTY = "duty_temporary_duty"
    const val LEAVE_FULL_MONTH = "leave_full_month"

    const val CADRE_AMC_NPA = "cadre_amc_npa"
    const val CADRE_TECHNICAL_OFFICER = "cadre_technical_officer"
    const val PROMOTION_ACTIVE = "promo_active"
    const val DNI_SCHEDULED = "dni_scheduled"

    const val RETIRE_NEAR = "retire_near"
    const val DSOP_HIGH_PACING = "dsop_high_pacing"
    const val TRANSFER_CTG = "transfer_ctg"
    const val AVAILED_LTC = "availed_ltc"
    const val TAX_ARREARS_SEC89 = "tax_arrears_sec89"

    val STATION_POSTING_KEYS =
        setOf(
            POST_SIACHEN,
            POST_FIELD_HAFAA,
            POST_FIELD_CFAA,
            POST_FIELD_CMFAA,
            POST_PEACE_HIGHER,
            POST_PEACE_OTHER,
        )
    val PEACE_STATION_KEYS = setOf(POST_PEACE_HIGHER, POST_PEACE_OTHER)
    val HOUSING_KEYS =
        setOf(
            HOUSE_GOVT_MQ,
            HOUSE_FAMILY_SPR,
            HOUSE_PEACE_RETENTION,
            HOUSE_SF_ACCOMMODATION,
            HOUSE_LIVING_OUT_NAC,
        )
    val CEA_CHILD_KEYS = setOf(CEA_NONE, CEA_ONE_CHILD, CEA_TWO_CHILDREN)
    val LEAVE_KEYS = setOf(LEAVE_FULL_MONTH)
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
