package com.payslipmax.pcdao.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AllowancesAndAdditionsData(
    val category: String = "Additions to Pay & Educational Allowances",
    val commission: String = "7th Central Pay Commission",
    @SerialName("source_documents")
    val sourceDocuments: List<String> = emptyList(),
    @SerialName("gallantry_awards")
    val gallantryAwards: GallantryAwardsConfig = GallantryAwardsConfig(),
    @SerialName("specialized_allowances")
    val specializedAllowances: SpecializedAllowancesConfig = SpecializedAllowancesConfig(),
    @SerialName("house_rent_allowance")
    val houseRentAllowance: HouseRentAllowanceConfig = HouseRentAllowanceConfig(),
    @SerialName("children_education_allowance")
    val childrenEducationAllowance: ChildrenEducationAllowanceConfig = ChildrenEducationAllowanceConfig(),
)

@Serializable
data class GallantryAwardsConfig(
    val authority: String? = null,
    @SerialName("tax_exemption")
    val taxExemption: String? = null,
    val awards: List<GallantryAwardItem> = emptyList(),
    @SerialName("bar_rule")
    val barRule: String? = null,
)

@Serializable
data class GallantryAwardItem(
    val decoration: String,
    @SerialName("monthly_rate")
    val monthlyRate: Int,
    @SerialName("rank_independent")
    val rankIndependent: Boolean = true,
    @SerialName("effective_from")
    val effectiveFrom: String? = null,
)

@Serializable
data class SpecializedAllowancesConfig(
    @SerialName("flying_allowance")
    val flyingAllowance: SpecializedAllowanceRateItem? = null,
    @SerialName("special_forces_allowance")
    val specialForcesAllowance: SpecializedAllowanceRateItem? = null,
    @SerialName("qualification_allowance_aviation")
    val qualificationAllowanceAviation: AviationQualificationConfig? = null,
    @SerialName("technical_allowance")
    val technicalAllowance: TechnicalAllowanceConfig? = null,
)

@Serializable
data class SpecializedAllowanceRateItem(
    @SerialName("rate_monthly")
    val rateMonthly: Int,
    @SerialName("escalated_rate_at_50_da")
    val escalatedRateAt50Da: Int,
    @SerialName("eligible_personnel")
    val eligiblePersonnel: String? = null,
    val authority: String? = null,
)

@Serializable
data class AviationQualificationConfig(
    @SerialName("master_aviation_instructor")
    val masterAviationInstructor: Int = 1125,
    @SerialName("senior_aviation_instructor_class_1")
    val seniorAviationInstructorClass1: Int = 900,
    @SerialName("senior_aviation_instructor_class_2")
    val seniorAviationInstructorClass2: Int = 630,
)

@Serializable
data class TechnicalAllowanceConfig(
    @SerialName("tier_1_monthly")
    val tier1Monthly: Int = 3000,
    @SerialName("tier_2_monthly")
    val tier2Monthly: Int = 4500,
    val description: String? = null,
)

@Serializable
data class HouseRentAllowanceConfig(
    @SerialName("definition_of_basic_pay")
    val definitionOfBasicPay: String? = null,
    val slabs: HraSlabsContainer = HraSlabsContainer(),
    @SerialName("minimum_floor_amounts")
    val minimumFloorAmounts: HraMinFloorConfig = HraMinFloorConfig(),
    @SerialName("selected_place_of_residence")
    val selectedPlaceOfResidence: Map<String, String> = emptyMap(),
)

@Serializable
data class HraSlabsContainer(
    @SerialName("base_rates_at_launch")
    val baseRatesAtLaunch: HraRatesByArea = HraRatesByArea(x = 24, y = 16, z = 8),
    @SerialName("revised_rates_when_da_crosses_25")
    val revisedRatesWhenDaCrosses25: HraRatesByArea = HraRatesByArea(x = 27, y = 18, z = 9),
    @SerialName("revised_rates_when_da_crosses_50")
    val revisedRatesWhenDaCrosses50: HraRatesByArea = HraRatesByArea(x = 30, y = 20, z = 10),
)

@Serializable
data class HraRatesByArea(
    @SerialName("X")
    val x: Int = 0,
    @SerialName("Y")
    val y: Int = 0,
    @SerialName("Z")
    val z: Int = 0,
)

@Serializable
data class HraMinFloorConfig(
    @SerialName("X_class_city_min")
    val xClassCityMin: Int = 5400,
    @SerialName("Y_class_city_min")
    val yClassCityMin: Int = 3600,
    @SerialName("Z_class_city_min")
    val zClassCityMin: Int = 1800,
)

@Serializable
data class ChildrenEducationAllowanceConfig(
    @SerialName("cea_annual_rate")
    val ceaAnnualRate: Int = 33750,
    @SerialName("cea_pre_50_da_rate")
    val ceaPre50DaRate: Int = 27000,
    @SerialName("hostel_subsidy_annual_rate")
    val hostelSubsidyAnnualRate: Int = 101250,
    @SerialName("hostel_subsidy_pre_50_da_rate")
    val hostelSubsidyPre50DaRate: Int = 81000,
    @SerialName("effective_date")
    val effectiveDate: String = "2024-04-01",
    @SerialName("divyang_child_multiplier")
    val divyangChildMultiplier: Double = 2.0,
    @SerialName("divyang_cea_annual_rate")
    val divyangCeaAnnualRate: Int = 67500,
    @SerialName("max_children")
    val maxChildren: Int = 2,
)

@Serializable
data class SpecialCompensatoryData(
    @SerialName("allowance_group")
    val allowanceGroup: String = "Special Compensatory & Operational Allowances",
    @SerialName("dress_allowance")
    val dressAllowance: DressAllowanceConfig? = null,
    @SerialName("tough_location_allowance")
    val toughLocationAllowance: ToughLocationConfig? = null,
    @SerialName("regional_duty_allowances")
    val regionalDutyAllowances: RegionalDutyConfig? = null,
)

@Serializable
data class DressAllowanceConfig(
    @SerialName("annual_rate_army_officers")
    val annualRateArmyOfficers: Int = 20000,
    @SerialName("annual_rate_mns_officers")
    val annualRateMnsOfficers: Int = 15000,
    @SerialName("credit_month")
    val creditMonth: String = "July",
    val escalation: String? = null,
)

@Serializable
data class ToughLocationConfig(
    val categories: Map<String, ToughLocationItem> = emptyMap(),
    val escalation: String? = null,
    @SerialName("mutual_exclusion_rule")
    val mutualExclusionRule: String? = null,
)

@Serializable
data class ToughLocationItem(
    @SerialName("monthly_rate")
    val monthlyRate: Int,
    @SerialName("places_covered")
    val placesCovered: String? = null,
    @SerialName("escalated_at_50_da")
    val escalatedAt50Da: Int? = null,
)

@Serializable
data class RegionalDutyConfig(
    @SerialName("special_duty_allowance_sda")
    val specialDutyAllowanceSda: SpecialDutyConfig? = null,
)

@Serializable
data class SpecialDutyConfig(
    @SerialName("rate_percent_of_basic_pay")
    val ratePercentOfBasicPay: Int = 10,
    @SerialName("applicable_regions")
    val applicableRegions: String? = null,
)
