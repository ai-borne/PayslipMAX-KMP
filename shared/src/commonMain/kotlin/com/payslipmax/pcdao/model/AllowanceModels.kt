package com.payslipmax.pcdao.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransportAllowanceData(
    @SerialName("allowance_name")
    val allowanceName: String = "Transport Allowance (TPTA)",
    @SerialName("effective_date")
    val effectiveDate: String = "2017-07-01",
    @SerialName("source_document")
    val sourceDocument: String? = null,
    val authority: String? = null,
    val rates: TransportRates = TransportRates(),
    @SerialName("higher_rate_cities")
    val higherRateCities: List<HigherRateCity> = emptyList(),
    @SerialName("divyang_rules")
    val divyangRules: DivyangTptaConfig? = null,
    @SerialName("disallowance_conditions")
    val disallowanceConditions: List<DisallowanceCondition> = emptyList(),
)

@Serializable
data class TransportRates(
    val slabs: List<TransportSlab> = emptyList(),
)

@Serializable
data class TransportSlab(
    val levels: List<String> = emptyList(),
    @SerialName("rank_range")
    val rankRange: String? = null,
    @SerialName("higher_rate_cities_monthly")
    val higherRateCitiesMonthly: Int = 0,
    @SerialName("other_places_monthly")
    val otherPlacesMonthly: Int = 0,
    @SerialName("da_applicable")
    val daApplicable: Boolean = true,
    @SerialName("official_car_option_rate")
    val officialCarOptionRate: Int? = null,
    @SerialName("official_car_option_irrespective_of_city")
    val officialCarOptionIrrespectiveOfCity: Boolean = false,
    val notes: String? = null,
)

@Serializable
data class HigherRateCity(
    val city: String,
    val type: String,
    val state: String,
)

@Serializable
data class DivyangTptaConfig(
    @SerialName("rate_multiplier")
    val rateMultiplier: Double = 2.0,
    @SerialName("eligible_categories")
    val eligibleCategories: List<String> = emptyList(),
    @SerialName("campus_restriction_exempt")
    val campusRestrictionExempt: Boolean = true,
    val notes: String? = null,
)

@Serializable
data class DisallowanceCondition(
    val scenario: String,
    val condition: String,
    val entitlement: String,
)

@Serializable
data class RiskHardshipData(
    @SerialName("allowance_group")
    val allowanceGroup: String = "Risk & Hardship Allowance",
    @SerialName("effective_date")
    val effectiveDate: String = "2019-02-22",
    @SerialName("rh_matrix")
    val rhMatrix: RhMatrix = RhMatrix(),
    @SerialName("field_allowances")
    val fieldAllowances: Map<String, FieldAllowanceEntry> = emptyMap(),
    @SerialName("high_altitude_allowance")
    val highAltitudeAllowance: Map<String, HighAltitudeEntry> = emptyMap(),
    @SerialName("one_level_down_rules")
    val oneLevelDownRules: OneLevelDownRules? = null,
    @SerialName("escalation_and_exclusions")
    val escalationAndExclusions: EscalationAndExclusions? = null,
)

@Serializable
data class RhMatrix(
    @SerialName("RH_MAX")
    val rhMax: SiachenEntry = SiachenEntry(),
    val cells: Map<String, RhCellEntry> = emptyMap(),
)

@Serializable
data class SiachenEntry(
    val name: String = "Siachen Allowance",
    val code: String = "RH_MAX",
    @SerialName("monthly_rate")
    val monthlyRate: Int = 42500,
    @SerialName("escalated_rate_at_50_da")
    val escalatedRateAt50Da: Int = 53125,
    val description: String? = null,
)

@Serializable
data class RhCellEntry(
    val risk: String,
    val hardship: String,
    val rate: Int,
    val code: String,
    val desc: String,
)

@Serializable
data class FieldAllowanceEntry(
    @SerialName("full_name")
    val fullName: String,
    val rate: Int,
    @SerialName("escalated_rate_at_50_da")
    val escalatedRateAt50Da: Int,
    @SerialName("rh_cell")
    val rhCell: String,
    val authority: String? = null,
)

@Serializable
data class HighAltitudeEntry(
    val description: String,
    val rate: Int,
    @SerialName("escalated_rate_at_50_da")
    val escalatedRateAt50Da: Int,
    @SerialName("rh_cell")
    val rhCell: String,
)

@Serializable
data class OneLevelDownRules(
    @SerialName("applicable_establishments")
    val applicableEstablishments: List<String> = emptyList(),
    @SerialName("downgrade_transitions")
    val downgradeTransitions: Map<String, String> = emptyMap(),
    @SerialName("grandfathering_protection")
    val grandfatheringProtection: String? = null,
)

@Serializable
data class EscalationAndExclusions(
    @SerialName("escalation_clause")
    val escalationClause: EscalationClause? = null,
    @SerialName("mutual_exclusions")
    val mutualExclusions: List<String> = emptyList(),
)

@Serializable
data class EscalationClause(
    @SerialName("percentage_increase")
    val percentageIncrease: Int = 25,
    @SerialName("da_threshold_percent")
    val daThresholdPercent: Int = 50,
    val formula: String? = null,
)
