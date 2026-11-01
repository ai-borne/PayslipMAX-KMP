package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * WHY: the count an order prints and the last order it quotes are what the counter and the chain flag run on. They must survive the
 * encrypted store and the backup, stay out of the database file, and an order saved before they existed (same `v: 1`) must still open.
 */
class Part2OrderFieldsTest {
    private val key = "device-key-for-tests"
    private val order = SyntheticBank.order().copy(expectedCount = 7, lastOrderNo = "SYN/QUOTED/55")

    @Test
    fun theCountAndTheQuotedOrderComeBackAndStayOutOfTheFile() {
        val row = order.toEntity(key)

        assertEquals(order, row.toOrder(key))
        assertFalse(row.ciphertext.decodeToString().contains("SYN/QUOTED/55"))
    }

    @Test
    fun anOrderRowSavedBeforeTheFieldsExistedStillOpensWithBothUnset() {
        val before = """{"v":1,"id":"order-1","orderNo":"N/1","date":{"y":2026,"m":2,"d":3},"createdAt":1,"updatedAt":2}"""
        val read = Part2OrderEntity("order-1", CryptoHelper.encrypt(before.encodeToByteArray(), key).getOrThrow()).toOrder(key)

        assertNull(read.expectedCount)
        assertNull(read.lastOrderNo)
        assertEquals("N/1", read.orderNo)
    }

    @Test
    fun theQuotedNumberAndTheCountAreBoundedLikeTheOrderNumber() {
        assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(order.copy(lastOrderNo = "a".repeat(Part2Limits.ORDER_NO_MAX + 1))))
        assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(order.copy(expectedCount = 0)))
        assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(order.copy(expectedCount = Part2Limits.EXPECTED_COUNT_MAX + 1)))
        assertNull(Part2Bounds.check(order.copy(expectedCount = Part2Limits.EXPECTED_COUNT_MAX)))
        assertNull(Part2Bounds.check(order.copy(expectedCount = null, lastOrderNo = null)))
    }
}
