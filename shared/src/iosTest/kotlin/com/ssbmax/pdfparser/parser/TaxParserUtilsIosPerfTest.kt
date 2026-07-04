package com.ssbmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/**
 * Proves [parseTaxAndSavings] stays fast on Kotlin/Native's actual regex engine, not just on the
 * JVM-backed corpus suite. Unlike the lookaround-per-word loops fixed in ParserUtilsIosPerfTest,
 * this function runs a fixed count of `Regex.find()` calls per document (not one per candidate
 * word), but two of its patterns use a `(?<!/)` lookbehind — the same risky construct class — so
 * it gets the same iOS timing guard per CLAUDE.md's rule for perf-sensitive commonMain regex code.
 * See docs/AI_INSIGHTS_PIPELINE.md §11 Changelog.
 */
class TaxParserUtilsIosPerfTest {
    @Test
    fun parseTaxAndSavingsIsFastAndLinearOnNative() {
        val small = syntheticTaxPageText(SMALL_SIZE)
        val large = syntheticTaxPageText(LARGE_SIZE)

        val smallElapsedMs = elapsedMillis { parseTaxAndSavings(small, small, small) }
        val largeElapsedMs = elapsedMillis { parseTaxAndSavings(large, large, large) }

        assertTrue(
            smallElapsedMs < BOUND_MS,
            "parseTaxAndSavings on ${SMALL_SIZE}B text took ${smallElapsedMs}ms, expected < ${BOUND_MS}ms",
        )
        assertTrue(
            largeElapsedMs < BOUND_MS,
            "parseTaxAndSavings on ${LARGE_SIZE}B text took ${largeElapsedMs}ms, expected < ${BOUND_MS}ms",
        )
        // Coarse linearity check: a regression to catastrophic backtracking on the lookbehind
        // patterns would blow this bound even if both absolute times still clear BOUND_MS.
        assertTrue(
            largeElapsedMs <= smallElapsedMs * LINEARITY_FACTOR + LINEARITY_SLACK_MS,
            "parseTaxAndSavings scaled non-linearly: ${SMALL_SIZE}B=${smallElapsedMs}ms, ${LARGE_SIZE}B=${largeElapsedMs}ms",
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
        private const val LINEARITY_SLACK_MS = 50L

        // Filler mimics real tax/DSOP page text surrounding the labelled fields the regexes target.
        private const val FILLER_UNIT =
            "Officer Rank Unit Station Basic Pay 45000 DA 12000 HRA 8000 MSP 15500 AGIF DSOPF " +
                "Educ Cess Grade Pay Kalyan Fund kuula Aaya laona dona ivavarNa raiSa laoKa "

        // Exercises every regex.find() in parseTaxAndSavings (both the primary and fallback branch
        // of grossSalMatch/taxableIncMatch/netTaxableMatch/taxPayableMatch/cessDeductedMatch) plus
        // the DSOP block match, so timing reflects real match-then-backtrack cost, not early-exit
        // no-match scans.
        private const val TAX_BLOCK =
            " Gross Salary upto 31/03/2024 852000" +
                " Total Taxable Income 742000" +
                " Standard Deduction 50000" +
                " Net Taxable Income 692000" +
                " Total Tax Payable 78000" +
                " Income Tax Deducted 6500" +
                " Ed. Cess Deducted 260" +
                " Opening Balance 100000 Subscription 60000 Refund 0 Misc Adj 0 Withdrawal 0 Closing Balance 160000"

        private fun syntheticTaxPageText(targetLength: Int): String {
            val sb = StringBuilder(targetLength + FILLER_UNIT.length)
            while (sb.length < targetLength - TAX_BLOCK.length) sb.append(FILLER_UNIT)
            return sb.substring(0, targetLength - TAX_BLOCK.length) + TAX_BLOCK
        }
    }
}
