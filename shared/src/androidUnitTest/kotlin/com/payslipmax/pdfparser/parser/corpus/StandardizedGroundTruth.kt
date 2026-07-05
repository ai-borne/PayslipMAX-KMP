package com.payslipmax.pdfparser.parser.corpus

import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.domain.TaxAndSavings
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Reads the human-curated `payslips_data_standardized.json` (snake_case) and maps the numeric
 * sections into the project's domain models, which is the ground truth the corpus asserts against.
 * Officer identity is intentionally NOT taken from here — it is replaced with de-identified
 * placeholders during capture.
 */
class StandardizedGroundTruth(jsonText: String) {
    private val byFile: Map<String, JsonObject>

    init {
        val array = CorpusFixtures.json.parseToJsonElement(jsonText) as JsonArray
        byFile =
            array.associate { element ->
                val obj = element.jsonObject
                val file = (obj["file"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""
                file to obj
            }
    }

    fun hasFile(filename: String): Boolean = byFile.containsKey(filename)

    fun earnings(filename: String): Earnings? =
        section(filename, "earnings")?.let { CorpusFixtures.json.decodeFromJsonElement(Earnings.serializer(), snakeToCamel(it)) }

    fun deductions(filename: String): Deductions? =
        section(filename, "deductions")?.let { CorpusFixtures.json.decodeFromJsonElement(Deductions.serializer(), snakeToCamel(it)) }

    fun summary(filename: String): PayslipSummary? =
        section(filename, "summary")?.let { CorpusFixtures.json.decodeFromJsonElement(PayslipSummary.serializer(), snakeToCamel(it)) }

    fun taxAndSavings(filename: String): TaxAndSavings? =
        section(filename, "tax_and_savings")?.let { CorpusFixtures.json.decodeFromJsonElement(TaxAndSavings.serializer(), snakeToCamel(it)) }

    private fun section(
        filename: String,
        key: String,
    ): JsonObject? = (byFile[filename]?.get(key) as? JsonObject)

    private fun snakeToCamel(element: JsonElement): JsonElement =
        when (element) {
            is JsonObject ->
                JsonObject(
                    element.entries.associate { (k, v) -> toCamel(k) to snakeToCamel(v) },
                )
            else -> element
        }

    private fun toCamel(key: String): String =
        key.split('_').mapIndexed { index, part ->
            if (index == 0) part else part.replaceFirstChar { it.uppercaseChar() }
        }.joinToString("")
}
