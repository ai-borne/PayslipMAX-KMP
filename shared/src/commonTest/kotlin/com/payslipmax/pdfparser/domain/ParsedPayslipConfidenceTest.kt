package com.payslipmax.pdfparser.domain

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ParsedPayslipConfidenceTest {
    private fun sample(
        fieldConfidence: Map<String, Float> = emptyMap(),
        needsReview: Boolean = false,
    ) = ParsedPayslip(
        file = "f",
        year = 2024,
        monthNum = 1,
        monthName = "January",
        dateStr = "01/2024",
        officer = Officer(name = "n", accountNo = "a", pan = "p"),
        earnings = Earnings(),
        deductions = Deductions(),
        ledgerBalances = LedgerBalances(),
        summary = PayslipSummary(grossPay = 0.0, totalDeductions = 0.0, netRemittance = 0.0),
        taxAndSavings = null,
        fieldConfidence = fieldConfidence,
        needsReview = needsReview,
    )

    @Test
    fun confidenceSidecarDefaultsAreNonInvasive() {
        val p = sample()
        assertTrue(p.fieldConfidence.isEmpty())
        assertFalse(p.needsReview)
    }

    @Test
    fun confidenceSidecarSurvivesSerializationRoundTrip() {
        val original = sample(fieldConfidence = mapOf("basicPay" to 0.42f), needsReview = true)
        val json = Json.encodeToString(ParsedPayslip.serializer(), original)
        val restored = Json.decodeFromString(ParsedPayslip.serializer(), json)

        assertEquals(0.42f, restored.fieldConfidence["basicPay"])
        assertTrue(restored.needsReview)
    }

    @Test
    fun legacyJsonWithoutSidecarStillDecodes() {
        val legacy =
            """
            {"file":"f","year":2024,"monthNum":1,"monthName":"January","dateStr":"01/2024",
            "officer":{"name":"n","accountNo":"a","pan":"p"},
            "earnings":{},"deductions":{},"ledgerBalances":{},
            "summary":{"grossPay":0.0,"totalDeductions":0.0,"netRemittance":0.0},
            "taxAndSavings":null}
            """.trimIndent()
        val restored = Json.decodeFromString(ParsedPayslip.serializer(), legacy)

        assertTrue(restored.fieldConfidence.isEmpty())
        assertFalse(restored.needsReview)
    }
}
