package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.parser.corpus.CorpusFixtures
import com.ssbmax.pdfparser.parser.corpus.CorpusTokens
import com.ssbmax.pdfparser.parser.diff.DivergenceCategory
import com.ssbmax.pdfparser.parser.diff.TokenDiff
import com.ssbmax.pdfparser.parser.diff.TokenDiffReport
import org.junit.Test
import java.io.File

/**
 * Opt-in corpus parity tool: compares committed Android tokens against iOS tokens captured
 * by [IosTokenCaptureTest] and emits a categorized divergence report.
 *
 * Skips entirely when the iOS token directory is absent (CI-safe). Run locally after capturing:
 *   ./gradlew :shared:iosSimulatorArm64Test --tests "*IosTokenCaptureTest*"
 * then:
 *   PAYSLIP_LOCAL_CORPUS_TOKENS_OUT=/tmp/ios_corpus_tokens ./gradlew :shared:testDebugUnitTest \
 *       --tests "*TokenParityDiffTest*"
 *
 * No assertions — the test always passes; the value is the printed report.
 */
class TokenParityDiffTest {
    /** Failing eras per the execution plan: ≤ Oct 2023 and ≥ Mar 2025. */
    private val failingEraPatterns =
        listOf(
            Regex(".*_2022$"),
            Regex("0[1-9]_.*_2023$"),
            Regex("10_oct_2023$"),
            Regex("0[3-9]_.*_2025$"),
            Regex("1[0-2]_.*_2025$"),
            Regex(".*_2026$"),
        )

    @Test
    fun compareIosAndAndroidTokensAcrossCorpus() {
        val iosTokenDir =
            System.getenv("PAYSLIP_LOCAL_CORPUS_TOKENS_OUT")
                ?.takeIf { it.isNotBlank() } ?: "/tmp/ios_corpus_tokens"

        val dir = File(iosTokenDir)
        if (!dir.exists()) {
            println("[TokenParityDiff] iOS token dir '$iosTokenDir' absent — skipping (expected in CI).")
            println("[TokenParityDiff] Capture first: ./gradlew :shared:iosSimulatorArm64Test --tests \"*IosTokenCaptureTest*\"")
            return
        }

        val ids = CorpusFixtures.loadIndex()
        if (ids.isEmpty()) {
            println("[TokenParityDiff] Corpus index is empty — no fixtures to compare.")
            return
        }

        val results = mutableListOf<Pair<String, TokenDiffReport?>>()
        var missing = 0

        for (id in ids) {
            val iosFile = File(dir, "$id.tokens.json")
            if (!iosFile.exists()) {
                println("[TokenParityDiff] ⚠️  iOS tokens missing for $id — skipping.")
                missing++
                continue
            }
            val iosTokensResult =
                runCatching {
                    CorpusFixtures.json.decodeFromString(CorpusTokens.serializer(), iosFile.readText())
                }
            if (iosTokensResult.isFailure) {
                println("[TokenParityDiff] ❌ Failed to parse iOS tokens for $id: ${iosTokensResult.exceptionOrNull()?.message}")
                results += id to null
                continue
            }
            val androidTokensResult = runCatching { CorpusFixtures.loadTokens(id) }
            if (androidTokensResult.isFailure) {
                println("[TokenParityDiff] ❌ Failed to load Android tokens for $id: ${androidTokensResult.exceptionOrNull()?.message}")
                results += id to null
                continue
            }

            val report =
                TokenDiff.compare(
                    androidTokensResult.getOrThrow().tableTokens,
                    iosTokensResult.getOrThrow().tableTokens,
                )
            results += id to report
        }

        printReport(results, missing, ids.size)
    }

    private fun printReport(
        results: List<Pair<String, TokenDiffReport?>>,
        missing: Int,
        total: Int,
    ) {
        val separator = "═".repeat(72)
        println("\n$separator")
        println("TOKEN PARITY DIFF REPORT  (Android corpus vs iOS captured tokens)")
        println(separator)

        val withReports = results.filter { it.second != null }
        val perfect = withReports.filter { it.second!!.deltas.isEmpty() }
        val diverged = withReports.filter { it.second!!.deltas.isNotEmpty() }

        println(
            "Fixtures total: $total | Compared: ${withReports.size} | Missing iOS: $missing | " +
                "Perfect: ${perfect.size} | Diverged: ${diverged.size}",
        )
        println()

        val failingEra = diverged.filter { (id, _) -> failingEraPatterns.any { it.matches(id) } }
        val otherEra = diverged.filter { (id, _) -> failingEraPatterns.none { it.matches(id) } }

        if (failingEra.isNotEmpty()) {
            println("── FAILING-ERA DIVERGENCES (${failingEra.size}) ──────────────────────────────")
            failingEra.forEach { (id, report) -> printFixtureRow(id, report!!, highlight = true) }
            println()
        }

        if (otherEra.isNotEmpty()) {
            println("── OTHER-ERA DIVERGENCES (${otherEra.size}) ──────────────────────────────────")
            otherEra.forEach { (id, report) -> printFixtureRow(id, report!!, highlight = false) }
            println()
        }

        if (perfect.isNotEmpty()) {
            println("── PERFECT MATCHES (${perfect.size}) ─────────────────────────────────────────")
            perfect.forEach { (id, _) -> println("  ✅ $id") }
            println()
        }

        printCategorySummary(withReports.mapNotNull { it.second })
        println(separator)
    }

    private fun printFixtureRow(
        id: String,
        report: TokenDiffReport,
        highlight: Boolean,
    ) {
        val prefix = if (highlight) "  ❌" else "  ⚠️ "
        val dominant = report.dominantCategory?.name ?: "NONE"
        println("$prefix $id  [android=${report.androidCount} ios=${report.iosCount} deltas=${report.deltas.size} dominant=$dominant]")
        DivergenceCategory.entries.filter { (report.categoryCounts[it] ?: 0) > 0 }.forEach { cat ->
            println("       ${cat.name}: ${report.categoryCounts[cat]}")
        }
    }

    private fun printCategorySummary(reports: List<TokenDiffReport>) {
        println("── CATEGORY TOTALS ACROSS ALL FIXTURES ──────────────────────────────")
        DivergenceCategory.entries.forEach { cat ->
            val total = reports.sumOf { it.categoryCounts.getOrDefault(cat, 0) }
            val fixtures = reports.count { (it.categoryCounts[cat] ?: 0) > 0 }
            println("  ${cat.name.padEnd(20)} $total divergences across $fixtures fixtures")
        }
    }
}
