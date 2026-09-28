package com.payslipmax.pcdao.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class PayMatrixData(
    val commission: String = "7th Central Pay Commission",
    @SerialName("effective_date")
    val effectiveDate: String = "2016-01-01",
    @SerialName("source_document")
    val sourceDocument: String? = null,
    @SerialName("source_pages")
    val sourcePages: String? = null,
    @SerialName("index_of_rationalisation")
    val indexOfRationalisation: IndexOfRationalisation = IndexOfRationalisation(),
    @SerialName("regular_officers_pay_matrix")
    val regularOfficersPayMatrix: Map<String, List<Int>> = emptyMap(),
    @SerialName("officer_rank_pay_level_mapping")
    val officerRankPayLevelMapping: OfficerRankPayLevelMapping = OfficerRankPayLevelMapping(),
    @SerialName("military_service_pay")
    val militaryServicePay: MilitaryServicePayConfig = MilitaryServicePayConfig(),
)

@Serializable
data class IndexOfRationalisation(
    @SerialName("level_10_to_11")
    val level10To11: Double = 2.57,
    @SerialName("level_12A_and_13")
    val level12AAnd13: Double = 2.67,
    @SerialName("level_13A_and_above")
    val level13AAndAbove: Double = 2.72,
    @SerialName("revision_note")
    val revisionNote: String? = null,
)

@Serializable
data class OfficerRankPayLevelMapping(
    @SerialName("regular_army")
    val regularArmy: Map<String, JsonElement> = emptyMap(),
    @SerialName("military_nursing_service")
    val militaryNursingService: Map<String, String> = emptyMap(),
    @SerialName("ncc_whole_time_lady_officers")
    val nccWholeTimeLadyOfficers: Map<String, String> = emptyMap(),
)

@Serializable
data class MilitaryServicePayConfig(
    @SerialName("regular_officers")
    val regularOfficers: MspEntry = MspEntry(rateMonthly = 15500),
    @SerialName("mns_officers")
    val mnsOfficers: MspEntry = MspEntry(rateMonthly = 10800),
    val ineligible: List<String> = emptyList(),
)

@Serializable
data class MspEntry(
    @SerialName("rate_monthly")
    val rateMonthly: Int = 0,
    @SerialName("applicable_levels")
    val applicableLevels: List<String> = emptyList(),
    @SerialName("applicable_ranks")
    val applicableRanks: List<String> = emptyList(),
    @SerialName("counts_for_da")
    val countsForDa: Boolean = true,
    @SerialName("counts_for_pension")
    val countsForPension: Boolean = true,
    @SerialName("counts_for_hra")
    val countsForHra: Boolean = false,
    val authority: String? = null,
)

@Serializable
data class PayStageEntry(
    val level: String,
    val stage: Int,
    val basicPay: Int,
)
