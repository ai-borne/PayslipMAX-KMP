package com.ssbmax.pdfparser.insights

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AiInsightReportTest {
    @Test
    fun testParseValidJson() {
        val jsonStr =
            """
            {
              "salaryChanges": [
                { "item": "Basic Pay", "change": "unchanged", "amount": 0.0 },
                { "item": "Dearness Allowance", "change": "increased", "amount": 3600.0 }
              ],
              "missingAllowances": ["Transport Allowance"],
              "newDeductions": [
                { "item": "Water Charges", "change": "increased", "amount": 120.0 }
              ],
              "riskAlerts": [
                { "observation": "Net salary decreased", "action": "Check deductions" }
              ],
              "opportunities": [
                { "opportunity": "Invest in NPS", "action": "Save tax under 80CCD(1B)" }
              ],
              "actionRequired": [
                "Verify Transport Allowance"
              ]
            }
            """.trimIndent()

        val report = AiInsightReportParser.parse(jsonStr)
        assertNotNull(report)
        assertEquals(2, report.salaryChanges.size)
        assertEquals("Basic Pay", report.salaryChanges[0].item)
        assertEquals("unchanged", report.salaryChanges[0].change)
        assertEquals(0.0, report.salaryChanges[0].amount)
        assertEquals(3600.0, report.salaryChanges[1].amount)

        assertEquals(1, report.missingAllowances.size)
        assertEquals("Transport Allowance", report.missingAllowances[0])

        assertEquals(1, report.newDeductions.size)
        assertEquals("Water Charges", report.newDeductions[0].item)
        assertEquals(120.0, report.newDeductions[0].amount)

        assertEquals(1, report.riskAlerts.size)
        assertEquals("Net salary decreased", report.riskAlerts[0].observation)
        assertEquals("Check deductions", report.riskAlerts[0].action)

        assertEquals(1, report.opportunities.size)
        assertEquals("Invest in NPS", report.opportunities[0].opportunity)

        assertEquals(1, report.actionRequired.size)
        assertEquals("Verify Transport Allowance", report.actionRequired[0])
    }

    @Test
    fun testParseMissingKeysUsesDefaults() {
        val jsonStr = "{}"
        val report = AiInsightReportParser.parse(jsonStr)
        assertNotNull(report)
        assertTrue(report.salaryChanges.isEmpty())
        assertTrue(report.missingAllowances.isEmpty())
        assertTrue(report.newDeductions.isEmpty())
        assertTrue(report.riskAlerts.isEmpty())
        assertTrue(report.opportunities.isEmpty())
        assertTrue(report.actionRequired.isEmpty())
    }

    @Test
    fun testParseInvalidJsonReturnsNull() {
        val jsonStr = "not a json string"
        val report = AiInsightReportParser.parse(jsonStr)
        assertNull(report)
    }
}
