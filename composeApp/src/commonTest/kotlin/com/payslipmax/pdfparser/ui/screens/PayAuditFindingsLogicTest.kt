package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.insights.Anomaly
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PayAuditFindingsLogicTest {
    private fun anomaly(
        type: String,
        amount: Double = 1000.0,
    ) = Anomaly(type, "field", amount, "02/2026", "$type detail — recover ₹$amount")

    // 2 Pay Audit findings (TPTA_ENTITLEMENT, INCREMENT_MISSED) + 2 other PRO anomalies (DSOP_COMPLIANCE,
    // TAX_PROJECTION) that belong to other auditors, not the Pay Audit screen.
    private val mixed =
        listOf(
            anomaly("TPTA_ENTITLEMENT"),
            anomaly("INCREMENT_MISSED"),
            anomaly("DSOP_COMPLIANCE"),
            anomaly("TAX_PROJECTION"),
        )

    @Test
    fun unlockedShowsPayAuditFindingsOnly() {
        val d = partitionPayAuditFindings(mixed, hasAnomalyDetection = true)
        assertEquals(2, d.unlocked.size)
        assertTrue(d.unlocked.all { it.type == "TPTA_ENTITLEMENT" || it.type == "INCREMENT_MISSED" })
        assertFalse(d.isLocked)
    }

    @Test
    fun lockedExposesCountAndLabelsButNoDetail() {
        val d = partitionPayAuditFindings(mixed, hasAnomalyDetection = false)
        assertTrue(d.isLocked)
        assertEquals(2, d.lockedCount)
        assertTrue(d.unlocked.isEmpty())
        assertEquals(
            listOf(anomalyCategoryLabel("TPTA_ENTITLEMENT"), anomalyCategoryLabel("INCREMENT_MISSED")),
            d.lockedLabels,
        )
        assertTrue(d.lockedLabels.none { it.contains("₹") || it.contains("detail") })
    }

    @Test
    fun otherProAnomaliesAreExcludedEvenWhenUnlocked() {
        val d = partitionPayAuditFindings(mixed, hasAnomalyDetection = true)
        assertTrue(d.unlocked.none { it.type == "DSOP_COMPLIANCE" || it.type == "TAX_PROJECTION" })
    }

    @Test
    fun noPayAuditFindingsProducesZeroTotal() {
        val d = partitionPayAuditFindings(listOf(anomaly("DSOP_COMPLIANCE")), hasAnomalyDetection = false)
        assertEquals(0, d.totalCount)
        assertFalse(d.isLocked)
    }
}
