package com.ssbmax.pdfparser.ui.screens

import com.ssbmax.pdfparser.database.LedgerRecordEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HistoricalLedgerCardTest {
    @Test
    fun testLedgerRecordsSorting() {
        val r1 = createMockLedgerRecord("05/2026", 2026, 5)
        val r2 = createMockLedgerRecord("06/2026", 2026, 6)
        val r3 = createMockLedgerRecord("12/2025", 2025, 12)

        val records = listOf(r1, r3, r2)
        val sorted = records.sortedWith(compareByDescending<LedgerRecordEntity> { it.year }.thenByDescending { it.monthNum })

        assertEquals("06/2026", sorted[0].dateStr)
        assertEquals("05/2026", sorted[1].dateStr)
        assertEquals("12/2025", sorted[2].dateStr)
    }

    @Test
    fun testFormatAmountReused() {
        assertEquals("1.0L", formatAmount(100000.0))
        assertEquals("5,500", formatAmount(5500.0))
    }

    private fun createMockLedgerRecord(dateStr: String, year: Int, monthNum: Int): LedgerRecordEntity {
        return LedgerRecordEntity(
            dateStr = dateStr,
            year = year,
            monthNum = monthNum,
            basicPay = 100000.0,
            dearnessAllowance = 50000.0,
            militaryServicePay = 15500.0,
            transportAllowance = 7200.0,
            transportAllowanceDa = 3600.0,
            houseRentAllowance = 24000.0,
            grossPay = 200300.0,
            dsopSubscription = 20000.0,
            incomeTax = 25000.0,
            netPay = 155300.0
        )
    }
}
