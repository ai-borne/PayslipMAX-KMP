package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.parser.corpus.CorpusFixtures
import com.ssbmax.pdfparser.parser.corpus.CorpusQuarantine
import com.ssbmax.pdfparser.parser.diff.DivergenceCategory
import com.ssbmax.pdfparser.parser.diff.TokenDiff
import com.ssbmax.pdfparser.parser.diff.TokenDiffReport
import org.junit.Test
import kotlin.test.assertTrue

/**
 * CI-enforced corpus parity gate: compares the committed Android token corpus
 * (`androidUnitTest/resources/corpus/`) against the committed iOS PDFKit token dumps
 * (`androidUnitTest/resources/corpus_ios_tokens/`, captured via [IosTokenCaptureTest] on a
 * real device/simulator against the full local PDF corpus, then scrubbed and committed) for the
 * same 52 fixture ids. Runs by default under `:shared:testDebugUnitTest` — no opt-in env vars,
 * both fixture sets are committed.
 *
 * The real assertion is *token-content* parity per section (tableTokens/taxTokens/dsopTokens),
 * comparing text multisets so the check is insensitive to platform-specific reading order.
 * Per-token geometry (dx/dy/dHeight) is reported for visibility only, not asserted: real-device
 * capture shows PDFBox and PDFKit apply a small but highly consistent ~6-7pt y-offset and
 * ~3-4pt height difference from differing font-metrics conventions on every matched token — this
 * is expected, already-accepted platform variance (see the parser pipeline doc's Tier-1 token
 * extraction note), not a correctness signal, so asserting an absolute geometry threshold here
 * would fail on 100% of fixtures for a difference the project has already decided is fine.
 */
class TokenParityDiffTest {
    /**
     * Max tolerated per-section combined (onlyAndroid + onlyIos) token-text-count mismatch for
     * non-quarantined ids. Real capture shows a consistent combined diff of 0-4 across the original
     * 52-fixture corpus — e.g. 2 occurrences of "kuula" only-Android plus 2 occurrences of "kula"
     * only-iOS — from known benign tokenization quirks: a Hindi-transliteration spelling variant
     * ("kuula"/"kula"), a stamped watermark word ("ATTENTION") that PDFBox splits into single-letter
     * tokens while PDFKit keeps merged, and one iOS token with a trailing null byte.
     *
     * The 2015-2021 backfill (`corpus_ios_tokens/` capture) surfaced one more consistent benign
     * source, up to 15 combined: a "Note: This is a system generated document." footer line that
     * PDFKit tokenizes into ~7 words (captured as onlyIos) while PDFBox doesn't capture at all in
     * that era, plus the same "AO/SAO(TW)" parenthesis-boundary tokenization variance already seen
     * elsewhere. Anything beyond this is a real regression, not known noise.
     */
    private val maxBenignContentDiff = 15

    @Test
    fun androidAndIosTokensMatchAcrossCorpus() {
        val ids = CorpusFixtures.loadIndex()
        assertTrue(ids.isNotEmpty(), "Corpus index is empty — no fixtures to compare.")

        val geometryReports = mutableListOf<Pair<String, TokenDiffReport>>()
        val failures = mutableListOf<String>()

        for (id in ids) {
            val androidTokens = CorpusFixtures.loadTokens(id)
            val iosTokens =
                CorpusFixtures.loadIosTokens(id)
                    ?: error("Missing committed iOS token dump for '$id' — recapture with IosTokenCaptureTest and commit it.")

            geometryReports += id to TokenDiff.compare(androidTokens.tableTokens, iosTokens.tableTokens)

            if (CorpusQuarantine.isQuarantined(id)) continue

            failures += contentMismatches(id, "tableTokens", androidTokens.tableTokens, iosTokens.tableTokens)
            failures += contentMismatches(id, "taxTokens", androidTokens.taxTokens, iosTokens.taxTokens)
            failures += contentMismatches(id, "dsopTokens", androidTokens.dsopTokens, iosTokens.dsopTokens)
        }

        printGeometryReport(geometryReports)

        assertTrue(
            failures.isEmpty(),
            "Token content parity failures beyond the known-benign tolerance ($maxBenignContentDiff):\n" +
                failures.joinToString("\n"),
        )
    }

    /** Returns a failure message if the text-multiset diff between [android] and [ios] exceeds tolerance, else empty. */
    private fun contentMismatches(
        id: String,
        section: String,
        android: List<PositionedToken>,
        ios: List<PositionedToken>,
    ): List<String> {
        val androidCounts = android.groupingBy { it.text }.eachCount()
        val iosCounts = ios.groupingBy { it.text }.eachCount()
        val onlyAndroid = surplus(androidCounts, iosCounts)
        val onlyIos = surplus(iosCounts, androidCounts)
        val totalDiff = onlyAndroid.values.sum() + onlyIos.values.sum()
        if (totalDiff <= maxBenignContentDiff) return emptyList()
        return listOf(
            "$id.$section: content diff=$totalDiff exceeds tolerance $maxBenignContentDiff " +
                "(onlyAndroid=$onlyAndroid, onlyIos=$onlyIos)",
        )
    }

    /** Per-key counts present in [a] beyond what [b] has of the same key. */
    private fun surplus(
        a: Map<String, Int>,
        b: Map<String, Int>,
    ): Map<String, Int> = a.mapValues { (key, count) -> count - (b[key] ?: 0) }.filterValues { it > 0 }

    private fun printGeometryReport(results: List<Pair<String, TokenDiffReport>>) {
        val separator = "═".repeat(72)
        println("\n$separator")
        println("TOKEN GEOMETRY REPORT  (Android corpus vs committed iOS tokens — informational only)")
        println(separator)

        val perfect = results.filter { it.second.deltas.isEmpty() }
        val diverged = results.filter { it.second.deltas.isNotEmpty() }
        println("Fixtures: ${results.size} | Perfect: ${perfect.size} | With geometry/order deltas: ${diverged.size}")
        println()

        val quarantined = diverged.filter { (id, _) -> CorpusQuarantine.isQuarantined(id) }
        val asserted = diverged.filter { (id, _) -> !CorpusQuarantine.isQuarantined(id) }

        if (quarantined.isNotEmpty()) {
            println("── QUARANTINED (legacy DSOP era, content not asserted) (${quarantined.size}) ──")
            quarantined.forEach { (id, report) -> printFixtureRow(id, report) }
            println()
        }
        if (asserted.isNotEmpty()) {
            println("── ASSERTED (content parity enforced; geometry informational) (${asserted.size}) ──")
            asserted.forEach { (id, report) -> printFixtureRow(id, report) }
            println()
        }

        printCategorySummary(results.map { it.second })
        println(separator)
    }

    private fun printFixtureRow(
        id: String,
        report: TokenDiffReport,
    ) {
        val dominant = report.dominantCategory?.name ?: "NONE"
        println("  $id  [android=${report.androidCount} ios=${report.iosCount} deltas=${report.deltas.size} dominant=$dominant]")
        DivergenceCategory.entries.filter { (report.categoryCounts[it] ?: 0) > 0 }.forEach { cat ->
            println("       ${cat.name}: ${report.categoryCounts[cat]}")
        }
    }

    private fun printCategorySummary(reports: List<TokenDiffReport>) {
        println("── CATEGORY TOTALS ACROSS ALL FIXTURES (informational) ──────────────")
        DivergenceCategory.entries.forEach { cat ->
            val total = reports.sumOf { it.categoryCounts.getOrDefault(cat, 0) }
            val fixtures = reports.count { (it.categoryCounts[cat] ?: 0) > 0 }
            println("  ${cat.name.padEnd(20)} $total divergences across $fixtures fixtures")
        }
    }
}
