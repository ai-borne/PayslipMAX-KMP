package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.parser.corpus.CorpusFixtures
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Always-on Phase 2 guard for the token IR. It loads the committed, de-identified `<id>.tokens.json`
 * fixtures and asserts they are well-formed: a populated table page, a recognizable table landmark,
 * and finite, non-negative geometry in the single top-down coordinate convention. No device, no real
 * PDFs, no PII.
 *
 * This protects the Phase 2 contract the common grid engine (Phase 3) will consume: if a future
 * extraction change drops the table page, loses the BPAY landmark, or emits garbage coordinates, it
 * fails here rather than silently downstream.
 */
class TokenCorpusRegressionTest {
    @Test
    fun committedTokenFixturesAreWellFormed() {
        val ids = CorpusFixtures.loadIndex()
        assertTrue(ids.isNotEmpty(), "Corpus index is empty — fixtures missing from resources/corpus/")

        val problems = mutableListOf<String>()
        for (id in ids) {
            val tokens = CorpusFixtures.loadTokens(id)

            if (tokens.tableTokens.isEmpty()) {
                problems += "$id: table tokens empty"
                continue
            }

            val hasLandmark =
                tokens.tableTokens.any {
                    val t = it.text.lowercase()
                    t.contains("bpay") || t.contains("basic") || t.contains("pay")
                }
            if (!hasLandmark) problems += "$id: no BPAY/Basic Pay landmark among table tokens"

            val allTokens = tokens.tableTokens + tokens.taxTokens + tokens.dsopTokens
            val badGeometry =
                allTokens.firstOrNull {
                    !it.x.isFinite() || !it.y.isFinite() ||
                        it.width <= 0f || it.height <= 0f ||
                        it.x < 0f || it.y < 0f
                }
            if (badGeometry != null) problems += "$id: bad token geometry -> $badGeometry"
        }

        assertTrue(
            problems.isEmpty(),
            "Token corpus problems (${ids.size} fixtures):\n" + problems.joinToString("\n"),
        )
    }
}
