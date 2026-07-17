package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.database.LedgerRecordEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MomChangesCalculationTest {
    private fun record(
        dateStr: String = "01/2024",
        basicPay: Double = 50_000.0,
        da: Double = 20_000.0,
        msp: Double = 15_000.0,
        ta: Double = 7_200.0,
        hra: Double = 0.0,
        dsop: Double = 5_000.0,
        tax: Double = 8_000.0,
    ) = LedgerRecordEntity(
        dateStr = dateStr, year = 2024, monthNum = 1,
        basicPay = basicPay, dearnessAllowance = da, militaryServicePay = msp,
        transportAllowance = ta, transportAllowanceDa = 0.0, houseRentAllowance = hra,
        grossPay = basicPay + da + msp + ta + hra,
        dsopSubscription = dsop, incomeTax = tax,
        netPay = basicPay + da + msp + ta + hra - dsop - tax,
    )

    @Test
    fun testNullPreviousReturnsEmpty() {
        assertTrue(calculateMomChanges(record(), null).isEmpty())
    }

    @Test
    fun testIdenticalRecordsReturnEmpty() {
        val r = record()
        assertTrue(calculateMomChanges(r, r).isEmpty())
    }

    @Test
    fun testBasicPayChangeDetected() {
        val changes = calculateMomChanges(record(basicPay = 55_000.0), record(basicPay = 50_000.0))
        val change = changes.single()
        assertEquals(50_000.0, change.prevValue)
        assertEquals(55_000.0, change.currValue)
        assertTrue(change.isEarning)
    }

    @Test
    fun testIncomeTaxChangeIsNotEarning() {
        val change = calculateMomChanges(record(tax = 9_000.0), record(tax = 8_000.0)).single()
        assertFalse(change.isEarning)
    }

    @Test
    fun testDsopChangeIsNotEarning() {
        val change = calculateMomChanges(record(dsop = 6_000.0), record(dsop = 5_000.0)).single()
        assertFalse(change.isEarning)
    }

    @Test
    fun testMultipleChangesAllDetected() {
        val changes =
            calculateMomChanges(
                record(basicPay = 55_000.0, da = 22_000.0),
                record(basicPay = 50_000.0, da = 20_000.0),
            )
        assertEquals(2, changes.size)
    }

    @Test
    fun testNoChangeWhenSameValues() {
        assertTrue(calculateMomChanges(record(basicPay = 50_000.0), record(basicPay = 50_000.0)).isEmpty())
    }

    @Test
    fun testHraChangeDetected() {
        val change = calculateMomChanges(record(hra = 5_000.0), record(hra = 0.0)).single()
        assertTrue(change.isEarning)
    }

    @Test
    fun testTransportAllowanceChangeDetected() {
        val change = calculateMomChanges(record(ta = 7_500.0), record(ta = 7_200.0)).single()
        assertTrue(change.isEarning)
    }

    @Test
    fun testMspChangeDetected() {
        val change = calculateMomChanges(record(msp = 16_000.0), record(msp = 15_000.0)).single()
        assertTrue(change.isEarning)
    }
}
