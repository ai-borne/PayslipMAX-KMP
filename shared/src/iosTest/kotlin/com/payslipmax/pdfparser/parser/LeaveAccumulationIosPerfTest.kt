package com.payslipmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/**
 * [LeaveAccumulationExtractor] runs on every import, over the whole document text. The JVM corpus suite cannot catch a
 * Kotlin/Native-only slowdown (see ParserUtilsIosPerfTest and docs/AI_INSIGHTS_PIPELINE.md §11), so this pins it to the
 * same bounds on Native: fast, and roughly linear in text length. The section sits at the end so the scan covers all of it.
 */
class LeaveAccumulationIosPerfTest {
    @Test
    fun extractIsFastAndLinearOnNative() {
        val small = syntheticText(SMALL_SIZE)
        val large = syntheticText(LARGE_SIZE)

        val smallElapsedMs = elapsedMillis { assertNotNull(LeaveAccumulationExtractor.extract(small)) }
        val largeElapsedMs = elapsedMillis { assertNotNull(LeaveAccumulationExtractor.extract(large)) }

        assertTrue(smallElapsedMs < BOUND_MS, "extract on ${SMALL_SIZE}B took ${smallElapsedMs}ms, expected < ${BOUND_MS}ms")
        assertTrue(largeElapsedMs < BOUND_MS, "extract on ${LARGE_SIZE}B took ${largeElapsedMs}ms, expected < ${BOUND_MS}ms")
        assertTrue(
            largeElapsedMs <= smallElapsedMs * LINEARITY_FACTOR + LINEARITY_SLACK_MS,
            "extract scaled non-linearly: ${SMALL_SIZE}B=${smallElapsedMs}ms, ${LARGE_SIZE}B=${largeElapsedMs}ms",
        )
    }

    private fun elapsedMillis(block: () -> Unit): Long {
        val mark = TimeSource.Monotonic.markNow()
        block()
        return mark.elapsedNow().inWholeMilliseconds
    }

    private fun syntheticText(size: Int): String {
        val section =
            buildString {
                append("LEAVE ACCUMULATION DETAILS\nCas Date Accu AL Total Accu AL Cas code Do2 No Do2 Dt\n")
                repeat(SECTION_ROWS) { append("31/12/2010 05 ${it + 1} ACCLVE 43 18/03/2011\n") }
                append("1. As per GoI, MoD letter dated 12-04-2010\n")
            }
        val filler = StringBuilder()
        while (filler.length + section.length < size) filler.append(FILLER_LINE)
        return filler.append(section).toString()
    }

    companion object {
        private const val SMALL_SIZE = 12_033
        private const val LARGE_SIZE = 17_203
        private const val SECTION_ROWS = 40
        private const val BOUND_MS = 2_000L
        private const val LINEARITY_FACTOR = 2L

        // Timer-granularity guard, as in ParserUtilsIosPerfTest: sub-millisecond runs round to 0-1ms.
        private const val LINEARITY_SLACK_MS = 250L

        // Real payslip noise: date-led arrears rows and labels that must not be mistaken for leave rows.
        private const val FILLER_LINE = "29/07/2024 30/11/2024 RH11 -1 Basic Pay 45000 DA 12000 kuula Aaya Page 5 of 7\n"
    }
}
