package com.ssbmax.pdfparser.parser.corpus

import com.ssbmax.pdfparser.domain.Deductions
import com.ssbmax.pdfparser.domain.Earnings
import com.ssbmax.pdfparser.domain.Officer
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.domain.PayslipSummary
import com.ssbmax.pdfparser.domain.TaxAndSavings
import com.ssbmax.pdfparser.parser.PositionedToken
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/*
 * Shared schema + helpers for the offline payslip regression corpus (Phase 0 safety net).
 *
 * Fixtures are committed under `androidUnitTest/resources/corpus/` as two files per payslip:
 *  - `<id>.input.json`    — the de-identified text the parser consumes (CorpusInput).
 *  - `<id>.expected.json` — the human-verified ground truth (CorpusExpected).
 *
 * Both the opt-in capture utility (CorpusCaptureTest) and the always-on regression test
 * (PayslipCorpusRegressionTest) read from this single source of truth.
 */

/** The exact, de-identified inputs fed to [com.ssbmax.pdfparser.parser.PayslipTextParser.parse]. */
@Serializable
data class CorpusInput(
    val id: String,
    val filename: String,
    val year: Int,
    val monthNum: Int,
    val leftColumnText: String,
    val middleColumnText: String,
    val fullText: String,
    val taxPageText: String,
    val dsopPageText: String,
)

/** Human-verified ground-truth values a correct parse must reproduce (within tolerance). */
@Serializable
data class CorpusExpected(
    val id: String,
    val filename: String,
    val year: Int,
    val monthNum: Int,
    val officer: Officer,
    val summary: PayslipSummary,
    val earnings: Earnings,
    val deductions: Deductions,
    val taxAndSavings: TaxAndSavings? = null,
)

/**
 * De-identified token IR captured from a payslip (Phase 2). Mirrors the [TokenizedPayslip] contract
 * but carries the corpus id/filename so it can be committed alongside the text fixtures and replayed
 * by the always-on token regression test without a device.
 */
@Serializable
data class CorpusTokens(
    val id: String,
    val filename: String,
    val year: Int,
    val tableTokens: List<PositionedToken>,
    val taxTokens: List<PositionedToken>,
    val dsopTokens: List<PositionedToken>,
)

object CorpusFixtures {
    const val RESOURCE_DIR = "corpus"

    /** Committed iOS PDFKit token dumps (Phase 2 parity corpus), keyed by the same fixture ids as [RESOURCE_DIR]. */
    const val IOS_TOKENS_RESOURCE_DIR = "corpus_ios_tokens"

    /** Tolerance (in rupees) for numeric field comparison — mirrors PlatformPdfParserTest. */
    const val TOLERANCE = 1.0

    val json: Json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    const val INDEX_RESOURCE = "corpus/index.json"

    fun inputResourcePath(id: String): String = "$RESOURCE_DIR/$id.input.json"

    fun expectedResourcePath(id: String): String = "$RESOURCE_DIR/$id.expected.json"

    fun tokensResourcePath(id: String): String = "$RESOURCE_DIR/$id.tokens.json"

    fun iosTokensResourcePath(id: String): String = "$IOS_TOKENS_RESOURCE_DIR/$id.tokens.json"

    /** Reads a committed corpus resource from the test classpath, or null if absent. */
    fun readResource(path: String): String? =
        CorpusFixtures::class.java.classLoader
            ?.getResourceAsStream(path)
            ?.bufferedReader()
            ?.use { it.readText() }

    /** The committed fixture ids, in stable order. */
    fun loadIndex(): List<String> =
        readResource(INDEX_RESOURCE)
            ?.let { json.decodeFromString(ListSerializer(String.serializer()), it) }
            ?: emptyList()

    fun loadInput(id: String): CorpusInput =
        json.decodeFromString(CorpusInput.serializer(), readResource(inputResourcePath(id)) ?: error("Missing input fixture: $id"))

    fun loadExpected(id: String): CorpusExpected =
        json.decodeFromString(CorpusExpected.serializer(), readResource(expectedResourcePath(id)) ?: error("Missing expected fixture: $id"))

    fun loadTokens(id: String): CorpusTokens =
        json.decodeFromString(CorpusTokens.serializer(), readResource(tokensResourcePath(id)) ?: error("Missing tokens fixture: $id"))

    /** Committed iOS token dump for the same fixture id, or null if not yet captured (see [IOS_TOKENS_RESOURCE_DIR]). */
    fun loadIosTokens(id: String): CorpusTokens? =
        readResource(iosTokensResourcePath(id))?.let { json.decodeFromString(CorpusTokens.serializer(), it) }

    /** Stable fixture id derived from a payslip filename, safe for use as a file name. */
    fun idFor(filename: String): String =
        filename
            .removeSuffix(".pdf")
            .removeSuffix(".PDF")
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')

    /**
     * Compares a freshly parsed payslip against ground truth, returning a list of human-readable
     * mismatches. An empty list means a perfect match.
     */
    fun diff(
        expected: CorpusExpected,
        actual: ParsedPayslip,
    ): List<String> {
        val mismatches = mutableListOf<String>()

        if (expected.officer.accountNo != actual.officer.accountNo) {
            mismatches += "officer.accountNo: expected '${expected.officer.accountNo}', got '${actual.officer.accountNo}'"
        }
        if (expected.officer.pan != actual.officer.pan) {
            mismatches += "officer.pan: expected '${expected.officer.pan}', got '${actual.officer.pan}'"
        }
        if (expected.officer.name != actual.officer.name) {
            mismatches += "officer.name: expected '${expected.officer.name}', got '${actual.officer.name}'"
        }

        mismatches += numericDiff("summary", json.encodeToJsonElement(expected.summary).jsonObject, json.encodeToJsonElement(actual.summary).jsonObject)
        mismatches += numericDiff("earnings", json.encodeToJsonElement(expected.earnings).jsonObject, json.encodeToJsonElement(actual.earnings).jsonObject)
        mismatches += numericDiff("deductions", json.encodeToJsonElement(expected.deductions).jsonObject, json.encodeToJsonElement(actual.deductions).jsonObject)

        val expTax = expected.taxAndSavings
        val actTax = actual.taxAndSavings
        if (expTax != null && actTax != null) {
            mismatches += numericDiff("tax", json.encodeToJsonElement(expTax).jsonObject, json.encodeToJsonElement(actTax).jsonObject)
        }

        return mismatches
    }

    private fun numericDiff(
        label: String,
        expected: JsonObject,
        actual: JsonObject,
    ): List<String> {
        val mismatches = mutableListOf<String>()
        for (key in (expected.keys + actual.keys)) {
            val e = (expected[key] as? JsonPrimitive)?.doubleOrNull ?: continue
            val a = (actual[key] as? JsonPrimitive)?.doubleOrNull ?: 0.0
            if (kotlin.math.abs(e - a) > TOLERANCE) {
                mismatches += "$label.$key: expected $e, got $a"
            }
        }
        return mismatches
    }
}
