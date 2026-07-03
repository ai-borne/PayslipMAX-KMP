package com.ssbmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/**
 * Proves the Phase 2 indexOf-based rewrite of [negateHindiTransliterations]/[parseTotals] is fast
 * and linear on Kotlin/Native's actual regex engine — not just correct on the JVM-backed corpus
 * suite. Input sizes (12,033 / 17,203 chars) match the two real pre-Nov-2023 documents whose
 * physical-device stalls (4m23.5s / 10m48.5s) root-caused this fix; see
 * docs/AI_INSIGHTS_PIPELINE.md §11 Changelog.
 */
class ParserUtilsIosPerfTest {
    @Test
    fun negateHindiTransliterationsIsFastAndLinearOnNative() {
        val small = syntheticPayslipText(SMALL_SIZE)
        val large = syntheticPayslipText(LARGE_SIZE)

        val smallElapsedMs = elapsedMillis { negateHindiTransliterations(small) }
        val largeElapsedMs = elapsedMillis { negateHindiTransliterations(large) }

        assertFastAndLinear("negateHindiTransliterations", smallElapsedMs, largeElapsedMs)
    }

    @Test
    fun parseTotalsIsFastAndLinearOnNative() {
        val small = syntheticTotalsText(SMALL_SIZE)
        val large = syntheticTotalsText(LARGE_SIZE)

        val smallElapsedMs = elapsedMillis { parseTotals(small) }
        val largeElapsedMs = elapsedMillis { parseTotals(large) }

        assertFastAndLinear("parseTotals", smallElapsedMs, largeElapsedMs)
    }

    private fun assertFastAndLinear(
        label: String,
        smallElapsedMs: Long,
        largeElapsedMs: Long,
    ) {
        assertTrue(
            smallElapsedMs < BOUND_MS,
            "$label on ${SMALL_SIZE}B text took ${smallElapsedMs}ms, expected < ${BOUND_MS}ms",
        )
        assertTrue(
            largeElapsedMs < BOUND_MS,
            "$label on ${LARGE_SIZE}B text took ${largeElapsedMs}ms, expected < ${BOUND_MS}ms",
        )
        // Coarse linearity check: a regression back to quadratic-shaped matching would blow this
        // bound even if both absolute times still individually clear BOUND_MS.
        assertTrue(
            largeElapsedMs <= smallElapsedMs * LINEARITY_FACTOR + LINEARITY_SLACK_MS,
            "$label scaled non-linearly: ${SMALL_SIZE}B=${smallElapsedMs}ms, ${LARGE_SIZE}B=${largeElapsedMs}ms",
        )
    }

    private fun elapsedMillis(block: () -> Unit): Long {
        val mark = TimeSource.Monotonic.markNow()
        block()
        return mark.elapsedNow().inWholeMilliseconds
    }

    companion object {
        private const val SMALL_SIZE = 12_033
        private const val LARGE_SIZE = 17_203
        private const val BOUND_MS = 2_000L
        private const val LINEARITY_FACTOR = 2L

        // Timer-granularity guard: at these sizes elapsed time can round to 0-1ms, which would
        // make the linearity check spuriously tight. Not a correctness escape hatch — quadratic
        // scaling at this text length would blow well past this slack.
        private const val LINEARITY_SLACK_MS = 50L

        // Filler mimics real payslip text: plain English labels/amounts interleaved with scattered
        // Hindi-transliteration words, so negateHindiTransliterations does real replacement work
        // rather than scanning text with zero matches.
        private const val FILLER_UNIT =
            "Basic Pay 45000 DA 12000 HRA 8000 MSP 15500 Kalyan Fund kuula Aaya laona dona " +
                "Officer Rank Unit Station AGIF DSOPF Educ Cess ivavarNa raiSa laoKa Grade Pay "

        private fun syntheticPayslipText(targetLength: Int): String {
            val sb = StringBuilder(targetLength + FILLER_UNIT.length)
            while (sb.length < targetLength) sb.append(FILLER_UNIT)
            return sb.substring(0, targetLength)
        }

        // Worst-case parseTotals shape per the diagnosis (see plan doc): totals only match on the
        // last, Hindi-transliterated candidate key for each field, forcing every earlier candidate
        // scan in ParserUtils.kt's totalsMapping to run and fail first.
        private const val TOTALS_BLOCK = " kuula Aaya 233016 kuula kTaOtI 93412 inavala p`oiYat Qana 139604"

        private fun syntheticTotalsText(targetLength: Int): String =
            syntheticPayslipText(targetLength - TOTALS_BLOCK.length) + TOTALS_BLOCK
    }
}
