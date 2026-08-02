package com.payslipmax.pdfparser.insights

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Locks [AnomalySeverityMapper] to the exact severity map that shipped in
 * `FinancialIntelligenceRepository.mapAnomalyTypeToSeverity()` before this SSOT existed. A mis-mapped
 * type here would either hide a real recovery risk behind an INFO chip, or (on the persisted-string
 * side) corrupt the "CRITICAL"/"WARNING"/"INFO"/"SUCCESS" strings already written into encrypted
 * `FinancialInsightEntity` rows — since severity is stored as a raw string, any drift is a silent
 * data-format break, not a compile error.
 */
class AnomalySeverityMapperTest {
    @Test
    fun `IMPORTANT anomaly types map to IMPORTANT severity`() {
        listOf("SALARY_LOSS", "DSOP_COMPLIANCE", "RENT_RECOVERY_RISK", "DEBIT_RECOVERY").forEach { type ->
            assertEquals(InsightSeverity.IMPORTANT, AnomalySeverityMapper.severityOf(type), "type=$type")
        }
    }

    @Test
    fun `WARNING anomaly types map to WARNING severity`() {
        listOf("MISSING_ALLOWANCE", "TPTA_ENTITLEMENT", "DEDUCTION_SPIKE", "TAX_PROJECTION").forEach { type ->
            assertEquals(InsightSeverity.WARNING, AnomalySeverityMapper.severityOf(type), "type=$type")
        }
    }

    @Test
    fun `remaining known and unknown anomaly types map to INFO severity`() {
        listOf("DSOP_MILESTONE", "ARREARS_AUDIT", "SOME_UNKNOWN_TYPE").forEach { type ->
            assertEquals(InsightSeverity.INFO, AnomalySeverityMapper.severityOf(type), "type=$type")
        }
    }

    @Test
    fun `IMPORTANT severity persists as legacy CRITICAL string so existing encrypted rows stay readable`() {
        assertEquals("CRITICAL", InsightSeverity.IMPORTANT.toPersistedString())
    }

    @Test
    fun `OPPORTUNITY severity persists as legacy SUCCESS string so existing encrypted rows stay readable`() {
        assertEquals("SUCCESS", InsightSeverity.OPPORTUNITY.toPersistedString())
    }

    @Test
    fun `WARNING and INFO severities persist using their own enum name`() {
        assertEquals("WARNING", InsightSeverity.WARNING.toPersistedString())
        assertEquals("INFO", InsightSeverity.INFO.toPersistedString())
    }
}
