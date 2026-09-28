package com.payslipmax.pcdao.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CanonicalRulesData(
    @SerialName("total_rules")
    val totalRules: Int = 0,
    val rules: List<CanonicalRule> = emptyList(),
)

@Serializable
data class CanonicalRule(
    @SerialName("rule_id")
    val ruleId: String,
    val domain: String,
    val title: String,
    val conditions: JsonElement? = null,
    val action: JsonElement? = null,
    val calculation: JsonElement? = null,
    @SerialName("effective_from")
    val effectiveFrom: String? = null,
    @SerialName("effective_to")
    val effectiveTo: String? = null,
    @SerialName("source_document")
    val sourceDocument: String? = null,
    @SerialName("source_page")
    val sourcePage: String? = null,
    val authority: String? = null,
    val confidence: String? = null,
    val status: String? = null,
    val notes: String? = null,
    @SerialName("action_type")
    val actionType: String? = null,
    @SerialName("raw_action")
    val rawAction: JsonElement? = null,
    @SerialName("mandatory_paperwork")
    val mandatoryPaperwork: List<String> = emptyList(),
)

@Serializable
data class PromotionWorkflowData(
    val category: String = "Promotion Pay Fixation & PCDA(O) Administrative Workflows",
    @SerialName("promotion_pay_fixation")
    val promotionPayFixation: PromotionFixationConfig? = null,
    @SerialName("pcdao_ledger_system")
    val pcdaoLedgerSystem: PcdaoLedgerConfig? = null,
    @SerialName("occurrence_codes")
    val occurrenceCodes: List<OccurrenceCodeEntry> = emptyList(),
)

@Serializable
data class PromotionFixationConfig(
    @SerialName("statutory_rules")
    val statutoryRules: String? = null,
    @SerialName("election_window_months")
    val electionWindowMonths: Int = 1,
    @SerialName("deadline_description")
    val deadlineDescription: String? = null,
    @SerialName("finality_rule")
    val finalityRule: String? = null,
    @SerialName("default_if_not_exercised")
    val defaultIfNotExercised: String? = null,
    val options: Map<String, PromotionOptionDetails> = emptyMap(),
)

@Serializable
data class PromotionOptionDetails(
    val name: String,
    val steps: List<String> = emptyList(),
    @SerialName("financial_advantage")
    val financialAdvantage: String? = null,
)

@Serializable
data class PcdaoLedgerConfig(
    @SerialName("cda_account_structure")
    val cdaAccountStructure: JsonElement? = null,
    val sections: Map<String, String> = emptyMap(),
    @SerialName("irla_concept")
    val irlaConcept: String? = null,
)

@Serializable
data class OccurrenceCodeEntry(
    val code: String,
    val description: String? = null,
    @SerialName("pcdao_action")
    val pcdaoAction: String? = null,
)
