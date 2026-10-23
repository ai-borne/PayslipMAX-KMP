package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.part2.domain.Part2Limits
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: every other storage test states a limit through `Part2Limits`, so a wrong edit to a constant would move the code and its tests
 * together and nothing would fail. These pin the numbers the owner decided (plan 12 "Storage", DECISIONS Round 10): a change here must
 * be a decision, not an accident.
 */
class Part2LimitsPinTest {
    @Test
    fun theOwnersStorageNumbersAreTheOnesInTheCode() {
        assertEquals(500 * 1024, Part2Limits.PAGE_MAX_BYTES, "pages are at most 500 KB")
        assertEquals(10, Part2Limits.PAGES_PER_ORDER_MAX, "at most 10 pages per order")
        assertEquals(60L * 1024 * 1024, Part2Limits.VAULT_WARN_BYTES, "Settings warns above 60 MB of vault")
        assertEquals(10, Part2Limits.REQUESTS_MAX)
        assertEquals(200, Part2Limits.TEXT_MAX)
        assertEquals(40, Part2Limits.ORDER_NO_MAX)
    }

    @Test
    fun theCeilingsAreOrderedSoAWarningCanComeBeforeARefusal() {
        assertTrue(Part2Limits.VAULT_WARN_BYTES < Part2Limits.VAULT_BYTES_MAX)
        assertTrue(Part2Limits.PAYLOAD_MAX_BYTES < Part2Limits.PAGE_MAX_BYTES)
        assertTrue(Part2Limits.PAGE_MAX_BYTES * Part2Limits.PAGES_PER_ORDER_MAX < Part2Limits.VAULT_BYTES_MAX)
    }
}
