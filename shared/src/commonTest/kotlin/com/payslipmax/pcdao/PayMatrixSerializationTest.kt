package com.payslipmax.pcdao

import com.payslipmax.pcdao.model.IndexOfRationalisation
import com.payslipmax.pcdao.model.MilitaryServicePayConfig
import com.payslipmax.pcdao.model.MspEntry
import com.payslipmax.pcdao.model.PayMatrixData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PayMatrixSerializationTest {
    @Test
    fun testPayMatrixDefaultsAndStructure() {
        val matrix =
            PayMatrixData(
                commission = "7th Central Pay Commission",
                indexOfRationalisation =
                    IndexOfRationalisation(
                        level10To11 = 2.57,
                        level12AAnd13 = 2.67,
                        level13AAndAbove = 2.72,
                    ),
                regularOfficersPayMatrix =
                    mapOf(
                        "10" to listOf(56100, 57800, 59500),
                        "11" to listOf(67700, 69700, 71800),
                        "12A" to listOf(121200, 124800, 128500),
                    ),
                militaryServicePay =
                    MilitaryServicePayConfig(
                        regularOfficers = MspEntry(rateMonthly = 15500),
                        mnsOfficers = MspEntry(rateMonthly = 10800),
                    ),
            )

        assertEquals("7th Central Pay Commission", matrix.commission)
        assertEquals(2.67, matrix.indexOfRationalisation.level12AAnd13)
        assertEquals(15500, matrix.militaryServicePay.regularOfficers.rateMonthly)
        assertEquals(10800, matrix.militaryServicePay.mnsOfficers.rateMonthly)

        val level10 = matrix.regularOfficersPayMatrix["10"]
        assertTrue(level10 != null && level10.size == 3)
        assertEquals(56100, level10.first())
    }
}
