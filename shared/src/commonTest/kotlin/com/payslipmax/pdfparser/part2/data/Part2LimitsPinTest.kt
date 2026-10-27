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

    @Test
    fun thePrintSnapshotLimitsFollowFromWhatADraftCanHoldAndHowLongTheAppWaits() {
        // 10 requests, each with at most 31 linked orders (the bundle's link limit, raised from 16 in phase 9) and itself.
        assertEquals(Part2Limits.REQUESTS_MAX * (31 + 1), Part2Limits.PRINTED_ROWS_MAX)
        assertEquals(256 * 1024, Part2Limits.PAYLOAD_MAX_BYTES, "the largest draft the form can make (10 requests, every area, every box at 200 characters) is 204 KB; raised from 128 KB in phase 9")
        assertEquals(28, Part2Limits.VALUES_PER_REQUEST_MAX, "the typed boxes of the longest real request (GTDAL with every area), raised from 16 in phase 9")
        // A column is a template narrative or certificate (at most 1,300) with typed remarks (at most 200) in it, with room to spare.
        assertTrue(Part2Limits.PRINTED_TEXT_MAX >= 1300 + 10 * Part2Limits.TEXT_MAX)
        assertEquals(4, Part2Limits.PRINTED_DATA_MAX, "Data 1 to 4")
        assertEquals(30L * 60 * 1000, Part2Limits.PRINT_WATCH_MS, "the app stops watching a print job after half an hour")
    }

    @Test
    fun thePageEncodingNumbersAreTheOnesTheOwnerDecidedAndLeaveRoomToStepDown() {
        assertEquals(1600, Part2Limits.PAGE_LONG_EDGE_PX, "the long edge is at most 1600 px (DECISIONS Round 10)")
        assertTrue(Part2Limits.PAGE_JPEG_QUALITY_START in Part2Limits.PAGE_JPEG_QUALITY_FLOOR..100)
        assertTrue(Part2Limits.PAGE_JPEG_QUALITY_STEP in 1..20, "a step small enough to land close under the size cap")
        assertTrue(Part2Limits.PAGE_JPEG_QUALITY_FLOOR >= 30, "a page below this is unreadable text: refuse it instead")
        assertTrue(Part2Limits.PAGE_RAW_MAX_BYTES > Part2Limits.PAGE_MAX_BYTES, "a scanner's page may be larger than the stored one")
        assertTrue(Part2Limits.PAGE_RAW_MAX_BYTES < 64 * 1024 * 1024, "and must fit the 256 MB test heap and a phone's with room to decode")
        assertTrue(Part2Limits.PAGE_PIXELS_MAX >= 48_000_000L, "a 48 MP phone photo is accepted")
    }
}
