package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.parser.corpus.CorpusFixtures
import com.ssbmax.pdfparser.parser.corpus.CorpusQuarantine
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Phase 3 structured-field parity gate: drives the **production engine** [GrammarAwareParser] over the
 * committed iOS PDFKit token dumps (`androidUnitTest/resources/corpus_ios_tokens/`, captured in Phase 2)
 * instead of the Android/PDFBox tokens, and diffs the result against the same human-verified
 * `<id>.expected.json` ground truth used by [TokenParseCorpusRegressionTest]. Both suites exercise the
 * identical `GrammarAwareParser`/`SharedParsingPipeline` code path — the only variable is which
 * platform's Tier-1 token extraction fed it — so a pass on both is the strongest available proof that
 * iOS and Android parse a given payslip to the same salary numbers, without a live device run in CI.
 *
 * [CorpusQuarantine.legacyDsopQuarantine] ids are skipped: their DSOP-page *token content* is already
 * known and accepted to diverge (Phase 2), so a structured-field mismatch there is expected, not a
 * regression. Every other id must match Android's result within [CorpusFixtures.TOLERANCE].
 */
class IosTokenParseCorpusRegressionTest {
    @Test
    fun iosTokenEngineMatchesGroundTruth() {
        val ids = CorpusFixtures.loadIndex()
        assertTrue(ids.isNotEmpty(), "Corpus index is empty — fixtures missing from resources/corpus/")

        val regressions = mutableListOf<String>()

        for (id in ids) {
            val iosTokens =
                CorpusFixtures.loadIosTokens(id)
                    ?: error("Missing committed iOS token dump for '$id' — recapture with IosTokenCaptureTest and commit it.")
            val input = CorpusFixtures.loadInput(id)
            val tokenized =
                TokenizedPayslip(
                    tableTokens = iosTokens.tableTokens,
                    taxTokens = iosTokens.taxTokens,
                    dsopTokens = iosTokens.dsopTokens,
                    fullText = input.fullText,
                )

            val result = GrammarAwareParser.parse(tokenized, input.filename)
            val parsed = result.getOrNull()
            val expected = CorpusFixtures.loadExpected(id)
            val mismatches =
                if (parsed == null) {
                    listOf("parse failed: ${result.exceptionOrNull()?.message}")
                } else {
                    CorpusFixtures.diff(expected, parsed)
                }

            if (mismatches.isNotEmpty() && !CorpusQuarantine.isQuarantined(id)) {
                regressions += "$id: ${mismatches.size} mismatch(es) -> ${mismatches.take(6)}"
            }
        }

        assertTrue(
            regressions.isEmpty(),
            "iOS token-path structured-field parity (${ids.size} fixtures, " +
                "${CorpusQuarantine.legacyDsopQuarantine.size} quarantine pattern(s)):\n" +
                regressions.joinToString("\n"),
        )
    }
}
