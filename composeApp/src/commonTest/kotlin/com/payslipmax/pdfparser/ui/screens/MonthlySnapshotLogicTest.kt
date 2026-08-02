package com.payslipmax.pdfparser.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Locks [snapshotMomPercent], the pure MoM-percent helper behind every delta shown in
 * [MonthlySnapshot] (net pay hero + gross/deductions/tax metrics). A wrong sign or a
 * divide-by-zero here would silently mislabel a pay cut as a raise.
 */
class MonthlySnapshotLogicTest {
    @Test
    fun `no previous value yields no percent`() {
        assertNull(snapshotMomPercent(current = 100_000.0, previous = null))
    }

    @Test
    fun `zero previous value yields no percent rather than dividing by zero`() {
        assertNull(snapshotMomPercent(current = 100_000.0, previous = 0.0))
    }

    @Test
    fun `an increase yields a positive percent`() {
        assertEquals(10.0, snapshotMomPercent(current = 110_000.0, previous = 100_000.0))
    }

    @Test
    fun `a decrease yields a negative percent`() {
        assertEquals(-5.0, snapshotMomPercent(current = 95_000.0, previous = 100_000.0))
    }

    @Test
    fun `no change yields zero`() {
        assertEquals(0.0, snapshotMomPercent(current = 100_000.0, previous = 100_000.0))
    }
}
