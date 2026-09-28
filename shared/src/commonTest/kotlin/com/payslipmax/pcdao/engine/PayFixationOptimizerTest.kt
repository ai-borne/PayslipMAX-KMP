package com.payslipmax.pcdao.engine

import com.payslipmax.pcdao.model.PayMatrixData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PayFixationOptimizerTest {
    private val testPayMatrix =
        PayMatrixData(
            regularOfficersPayMatrix =
                mapOf(
                    "10" to
                        listOf(
                            56100, 57800, 59500, 61300, 63100, 65000, 67000, 69000, 71100, 73200,
                            75400, 77700, 80000, 82400, 84900, 87400, 90000, 92700, 95500, 98400,
                            101400, 104400, 107500, 110700, 114000, 117400, 120900, 124500, 128200,
                            132000, 136000, 140100, 144300, 148600, 153100, 157700, 162400, 167300,
                            172300, 177500,
                        ),
                    "11" to
                        listOf(
                            69400, 71500, 73600, 75800, 78100, 80400, 82800, 85300, 87900, 90500,
                            93200, 96000, 98900, 101900, 105000, 108200, 111400, 114700, 118100,
                            121600, 125200, 129000, 132900, 136900, 141000, 145200, 149600, 154100,
                            158700, 163500, 168400, 173500, 178700, 184100, 189600, 195300, 201200,
                            207200,
                        ),
                    "13A" to
                        listOf(
                            139600, 143800, 148100, 152500, 157100, 161800, 166700, 171700, 176900,
                            182200, 187700, 193300, 199100, 205100, 211300, 217600,
                        ),
                    "14" to
                        listOf(
                            144200, 148500, 153000, 157600, 162300, 167200, 172200, 177400, 182700,
                            188200, 193800, 199600, 205600, 211800, 218200,
                        ),
                ),
        )

    private val optimizer = PayFixationOptimizer(testPayMatrix)

    @Test
    fun testLevel10Stage8PromotedMarchDniJulyWinsOption2() {
        // Case A from verified Python simulation
        val request =
            PayFixationRequest(
                fromLevel = "10",
                fromStage = 8,
                toLevel = "11",
                promotionDate = "2026-03-15",
                dniMonth = 7,
            )

        val result = optimizer.optimizePromotion(request)

        assertEquals("10", result.fromLevel)
        assertEquals(8, result.fromStage)
        assertEquals(69000, result.fromBasicPay)
        assertEquals("11", result.toLevel)
        assertEquals(71500, result.opt1FixedPay)
        assertEquals(2, result.opt1InitialStage)
        assertEquals(69400, result.opt2PreDniPay)
        assertEquals(73600, result.opt2PostDniFixedPay)
        assertEquals(3, result.opt2DniStage)

        assertEquals(2664000L, result.opt1Total36Months)
        assertEquals(2726800L, result.opt2Total36Months)
        assertEquals(62800L, result.cumulativeDelta)
        assertEquals(FixationOption.OPTION_2, result.recommendedOption)

        assertEquals(36, result.monthlyTrajectory.size)

        // Month 1: March 2026
        val m1 = result.monthlyTrajectory[0]
        assertEquals(1, m1.monthIndex)
        assertEquals(2026, m1.year)
        assertEquals(3, m1.month)
        assertEquals(71500, m1.option1BasicPay)
        assertEquals(69400, m1.option2BasicPay)
        assertEquals(-2100, m1.delta)

        // Month 5: July 2026 (DNI)
        val m5 = result.monthlyTrajectory[4]
        assertEquals(5, m5.monthIndex)
        assertEquals(2026, m5.year)
        assertEquals(7, m5.month)
        assertEquals(71500, m5.option1BasicPay)
        assertEquals(73600, m5.option2BasicPay)
        assertEquals(2100, m5.delta)

        // Month 11: January 2027 (First increment for both)
        val m11 = result.monthlyTrajectory[10]
        assertEquals(11, m11.monthIndex)
        assertEquals(2027, m11.year)
        assertEquals(1, m11.month)
        assertEquals(73600, m11.option1BasicPay)
        assertEquals(75800, m11.option2BasicPay)
        assertEquals(2200, m11.delta)
    }

    @Test
    fun testPromotionShortlyAfterDniFavorsOption1() {
        // Promoted in August with DNI in July (next DNI is 11 months away)
        val request =
            PayFixationRequest(
                fromLevel = "10",
                fromStage = 5,
                toLevel = "11",
                promotionDate = "2026-08-10",
                dniMonth = 7,
            )

        val result = optimizer.optimizePromotion(request)

        assertEquals(FixationOption.OPTION_1, result.recommendedOption)
        assertTrue(result.cumulativeDelta < 0L)
    }

    @Test
    fun testDniJanuaryCycleTransitionsCorrectly() {
        val request =
            PayFixationRequest(
                fromLevel = "10",
                fromStage = 8,
                toLevel = "11",
                promotionDate = "2026-10-15",
                dniMonth = 1,
            )

        val result = optimizer.optimizePromotion(request)

        assertEquals(36, result.monthlyTrajectory.size)
        // Month 1 (Oct 2026): Pre-DNI
        assertEquals(69400, result.monthlyTrajectory[0].option2BasicPay)
        // Month 4 (Jan 2027): DNI triggers
        val m4 = result.monthlyTrajectory[3]
        assertEquals(2027, m4.year)
        assertEquals(1, m4.month)
        assertEquals(73600, m4.option2BasicPay)
        // Month 10 (Jul 2027): First post-DNI increment after 6 months
        val m10 = result.monthlyTrajectory[9]
        assertEquals(2027, m10.year)
        assertEquals(7, m10.month)
        assertEquals(75800, m10.option2BasicPay)
    }

    @Test
    fun testCeilingAndBunchingAtMaxStage() {
        val request =
            PayFixationRequest(
                fromLevel = "10",
                fromStage = 40,
                toLevel = "11",
                promotionDate = "2026-03-01",
                dniMonth = 7,
            )

        val result = optimizer.optimizePromotion(request)

        assertEquals(177500, result.fromBasicPay)
        assertEquals(178700, result.opt1FixedPay)
        assertEquals(36, result.monthlyTrajectory.size)
    }

    @Test
    fun testMajorGeneralLevel14IncludesMsp() {
        val request =
            PayFixationRequest(
                fromLevel = "13A",
                fromStage = 1,
                toLevel = "14",
                promotionDate = "2026-04-01",
                dniMonth = 7,
                mspMonthly = 15500,
            )

        val result = optimizer.optimizePromotion(request)

        // Level 13A Stage 1 = 139,600. Next increment = 143,800 + MSP 15,500 = 159,300.
        // Cell in Level 14 >= 159,300 is 162,300 (Stage 5).
        assertEquals(162300, result.opt1FixedPay)
        assertEquals(5, result.opt1InitialStage)
    }

    @Test
    fun testSerializationAndStatutoryDeadlines() {
        val request =
            PayFixationRequest(
                fromLevel = "10",
                fromStage = 8,
                toLevel = "11",
                promotionDate = "2026-03-15",
                dniMonth = 7,
            )

        val result = optimizer.optimizePromotion(request)

        assertTrue(result.statutoryWarning.contains("Rule 11"))
        assertTrue(result.statutoryElectionDeadline.contains("2026-04"))

        val jsonStr = Json.encodeToString(result)
        val deserialized = Json.decodeFromString<PayFixationResult>(jsonStr)
        assertEquals(result.cumulativeDelta, deserialized.cumulativeDelta)
        assertEquals(result.recommendedOption, deserialized.recommendedOption)
    }
}
