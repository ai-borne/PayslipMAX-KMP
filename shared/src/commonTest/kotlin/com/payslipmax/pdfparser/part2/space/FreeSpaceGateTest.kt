package com.payslipmax.pdfparser.part2.space

import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.testing.FakeFreeSpaceReader
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: a scan that fills the phone's last megabytes corrupts the database write that follows it, so the Bank must stop before the
 * scanner opens (below 100 MB), and warn while there is still room to act (below 500 MB). The lines are the plan's, and a reader that
 * cannot answer must not lock an officer out of their own orders.
 */
class FreeSpaceGateTest {
    private fun verdict(free: Long?) = FreeSpaceGate(FakeFreeSpaceReader(free)).verdict()

    @Test
    fun theLinesAreFiveHundredAndOneHundredMegabytes() {
        assertEquals(500L * 1024 * 1024, Part2Limits.FREE_SPACE_WARN_BYTES)
        assertEquals(100L * 1024 * 1024, Part2Limits.FREE_SPACE_REFUSE_BYTES)
    }

    @Test
    fun belowTheRefusalLineNothingOpens() {
        assertEquals(SpaceVerdict.REFUSED, verdict(0))
        assertEquals(SpaceVerdict.REFUSED, verdict(Part2Limits.FREE_SPACE_REFUSE_BYTES - 1))
    }

    @Test
    fun atTheRefusalLineTheScanOpensWithAWarning() {
        assertEquals(SpaceVerdict.LOW, verdict(Part2Limits.FREE_SPACE_REFUSE_BYTES))
        assertEquals(SpaceVerdict.LOW, verdict(Part2Limits.FREE_SPACE_WARN_BYTES - 1))
    }

    @Test
    fun atTheWarningLineAndAboveThereIsNothingToSay() {
        assertEquals(SpaceVerdict.ENOUGH, verdict(Part2Limits.FREE_SPACE_WARN_BYTES))
        assertEquals(SpaceVerdict.ENOUGH, verdict(Long.MAX_VALUE))
    }

    @Test
    fun aReaderThatCannotAnswerNeverBlocksTheOfficer() {
        assertEquals(SpaceVerdict.ENOUGH, verdict(null))
        assertEquals(SpaceVerdict.ENOUGH, runCatching { FreeSpaceGate(FakeFreeSpaceReader(fails = true)).verdict() }.getOrNull())
        assertEquals(SpaceVerdict.ENOUGH, FreeSpaceGate.Unknown.verdict())
    }
}
