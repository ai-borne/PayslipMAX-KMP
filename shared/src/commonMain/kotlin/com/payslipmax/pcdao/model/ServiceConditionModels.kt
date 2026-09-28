package com.payslipmax.pcdao.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TravelTadaData(
    @SerialName("allowance_group")
    val allowanceGroup: String = "Travelling & Daily Allowance (TA/DA)",
    @SerialName("governing_rules")
    val governingRules: String? = null,
    @SerialName("source_document")
    val sourceDocument: String? = null,
    @SerialName("travel_entitlements")
    val travelEntitlements: List<TravelEntitlement> = emptyList(),
    @SerialName("daily_allowance_rates")
    val dailyAllowanceRates: List<DailyAllowanceEntry> = emptyList(),
    @SerialName("transfer_and_baggage")
    val transferAndBaggage: TransferAndBaggageConfig? = null,
    @SerialName("deadlines_and_documentation")
    val deadlinesAndDocumentation: DeadlinesAndDocumentation? = null,
)

@Serializable
data class TransferAndBaggageConfig(
    @SerialName("composite_transfer_grant")
    val compositeTransferGrant: CompositeTransferGrantConfig? = null,
)

@Serializable
data class TravelEntitlement(
    val levels: List<String> = emptyList(),
    @SerialName("rank_group")
    val rankGroup: String,
    @SerialName("air_travel_class")
    val airTravelClass: String,
    @SerialName("train_travel_class")
    val trainTravelClass: String,
    @SerialName("premium_trains_class")
    val premiumTrainsClass: String? = null,
    @SerialName("sea_travel_class")
    val seaTravelClass: String? = null,
)

@Serializable
data class DailyAllowanceEntry(
    val levels: List<String> = emptyList(),
    @SerialName("hotel_reimbursement_ceiling_daily")
    val hotelReimbursementCeilingDaily: Int = 0,
    @SerialName("hotel_escalated_at_50_da")
    val hotelEscalatedAt50Da: Int = 0,
    @SerialName("food_reimbursement_lump_sum_daily")
    val foodReimbursementLumpSumDaily: Int = 0,
    @SerialName("food_escalated_at_50_da")
    val foodEscalatedAt50Da: Int = 0,
)

@Serializable
data class CompositeTransferGrantConfig(
    @SerialName("standard_rate_percent_of_basic")
    val standardRatePercentOfBasic: Int = 80,
    @SerialName("island_rate_percent_of_basic")
    val islandRatePercentOfBasic: Int = 100,
    @SerialName("minimum_distance_km")
    val minimumDistanceKm: Int = 20,
)

@Serializable
data class DeadlinesAndDocumentation(
    @SerialName("claim_deadlines")
    val claimDeadlines: Map<String, ClaimDeadlineItem> = emptyMap(),
    @SerialName("mandatory_documentation")
    val mandatoryDocumentation: List<String> = emptyList(),
)

@Serializable
data class ClaimDeadlineItem(
    @SerialName("deadline_days")
    val deadlineDays: Int,
    val penalty: String? = null,
    val notes: String? = null,
)

@Serializable
data class ServiceConditionsData(
    val category: String = "Service Conditions, Statutory Funds & Leave Schemes",
    @SerialName("source_document")
    val sourceDocument: String? = null,
    @SerialName("dsop_fund")
    val dsopFund: DsopfConfig = DsopfConfig(),
    @SerialName("agif_insurance")
    val agifInsurance: AgifConfig = AgifConfig(),
    @SerialName("retirement_ages")
    val retirementAges: RetirementAgesConfig? = null,
)

@Serializable
data class DsopfConfig(
    val eligibility: String? = null,
    @SerialName("minimum_subscription_percent")
    val minimumSubscriptionPercent: Double = 6.0,
    @SerialName("maximum_subscription_percent")
    val maximumSubscriptionPercent: Double = 100.0,
    @SerialName("emoluments_base")
    val emolumentsBase: String? = null,
    @SerialName("annual_tax_exempt_subscription_limit")
    val annualTaxExemptSubscriptionLimit: Long = 500000L,
    @SerialName("subscription_stoppage_before_retirement")
    val subscriptionStoppageBeforeRetirement: String? = null,
    @SerialName("variation_rules")
    val variationRules: String? = null,
)

@Serializable
data class AgifConfig(
    @SerialName("monthly_subscription_regular_officers")
    val monthlySubscriptionRegularOfficers: Int = 10000,
    @SerialName("life_insurance_cover")
    val lifeInsuranceCover: Long = 10000000L,
    @SerialName("effective_cover_formatted")
    val effectiveCoverFormatted: String? = "₹1 Crore",
    val authority: String? = null,
)

@Serializable
data class RetirementAgesConfig(
    @SerialName("combat_arms")
    val combatArms: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
    val services: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
)
