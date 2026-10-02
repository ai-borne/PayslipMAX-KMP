package com.payslipmax.pdfparser.insights

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MspAuditorTest {
    private val auditor = MspAuditor()

    private fun run(
        msp: Double,
        basic: Double = 85300.0,
    ): List<Anomaly> {
        val current = payAuditPayslip(2018, 2, basic, msp = msp)
        return auditor.audit(current, null, listOf(payAuditPayslip(2018, 1, basic)))
    }

    @Test
    fun partPaidMspIsUnderpaymentWithExpectedAndActual() {
        val finding = run(msp = 10500.0).single()
        assertEquals("MSP_SHORTFALL", finding.type)
        assertEquals(5000.0, finding.amount)
        assertEquals(15500.0, finding.expected)
        assertEquals(10500.0, finding.actual)
        assertEquals(PayAuthorities.MILITARY_SERVICE_PAY, finding.authority)
    }

    @Test
    fun fullMspAndArrearsLevelMspAreNotFlagged() {
        assertTrue(run(msp = 15500.0).isEmpty())
        assertTrue(run(msp = 21500.0).isEmpty())
    }

    @Test
    fun noMspAtAllIsTheMissingAllowanceCase() {
        assertTrue(run(msp = 0.0).isEmpty())
    }

    @Test
    fun levelFourteenIsNotEntitledToMsp() {
        assertTrue(run(msp = 10500.0, basic = 144200.0).isEmpty())
    }
}
