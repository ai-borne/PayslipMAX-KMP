package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.parser.corpus.CorpusFixtures
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Always-on regression net (Phase 0). It loads the committed, de-identified corpus fixtures,
 * runs the platform-independent [PayslipTextParser.parse] over them, and diffs the result against
 * human-verified ground truth. No device, no real PDFs, no PII — it is fully CI-safe.
 *
 * This is the primary guard against "fix one month, break another": any future parser change that
 * regresses a committed payslip fails here.
 *
 * Baseline (captured 2026-06-27, payslips 2022–2026, 52 fixtures): the current parser matches
 * ground truth on ALL fixtures, so [QUARANTINE] is empty. Should a parser change ever require
 * temporarily accepting a known-bad month, add its fixture id here with a documented reason; the
 * test then asserts it still fails (and flags it if it starts passing, so quarantine never rots).
 */
class PayslipCorpusRegressionTest {
    /** Fixture ids the parser is known to get wrong, each with a documented reason. Empty at baseline. */
    private val quarantine: Set<String> = emptySet()

    @Test
    fun committedCorpusMatchesGroundTruth() {
        val ids = CorpusFixtures.loadIndex()
        assertTrue(ids.isNotEmpty(), "Corpus index is empty — fixtures missing from resources/corpus/")

        val regressions = mutableListOf<String>()
        val staleQuarantine = mutableListOf<String>()

        for (id in ids) {
            val input = CorpusFixtures.loadInput(id)
            val expected = CorpusFixtures.loadExpected(id)

            val result =
                PayslipTextParser.parse(
                    leftColumnText = input.leftColumnText,
                    middleColumnText = input.middleColumnText,
                    fullText = input.fullText,
                    taxPageText = input.taxPageText,
                    dsopPageText = input.dsopPageText,
                    filename = input.filename,
                )

            val isQuarantined = id in quarantine
            val parsed = result.getOrNull()
            val mismatches =
                if (parsed == null) {
                    listOf("parse failed: ${result.exceptionOrNull()?.message}")
                } else {
                    CorpusFixtures.diff(expected, parsed)
                }

            when {
                mismatches.isEmpty() && isQuarantined ->
                    staleQuarantine += "$id now PASSES — remove it from quarantine."
                mismatches.isNotEmpty() && !isQuarantined ->
                    regressions += "$id: ${mismatches.size} mismatch(es) -> ${mismatches.take(5)}"
            }
        }

        val problems = regressions + staleQuarantine
        assertTrue(
            problems.isEmpty(),
            "Corpus regression detected (${ids.size} fixtures, ${quarantine.size} quarantined):\n" + problems.joinToString("\n"),
        )
    }
}
