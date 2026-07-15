package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.insights.Anomaly
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdvancedAnomaliesLogicTest {
    private fun anomaly(
        type: String,
        amount: Double = 1000.0,
    ) = Anomaly(type, "field", amount, "02/2026", "$type detail — recover ₹$amount")

    // 2 FREE (SALARY_LOSS, DEDUCTION_SPIKE) + 2 PRO (MISSING_ALLOWANCE, DSOP_COMPLIANCE)
    private val mixed =
        listOf(
            anomaly("SALARY_LOSS"),
            anomaly("DEDUCTION_SPIKE"),
            anomaly("MISSING_ALLOWANCE"),
            anomaly("DSOP_COMPLIANCE"),
        )

    /** Unlocked: PRO anomalies shown in full; FREE ones excluded (they live in the health score). */
    @Test
    fun unlockedShowsProDetailOnly() {
        val d = partitionAdvancedAnomalies(mixed, hasAnomalyDetection = true)
        assertEquals(2, d.unlocked.size)
        assertTrue(d.unlocked.all { it.type == "MISSING_ALLOWANCE" || it.type == "DSOP_COMPLIANCE" })
        assertFalse(d.isLocked)
    }

    /** Locked (free / FORCE_FREE override): count + category labels only — no amounts, no descriptions. */
    @Test
    fun lockedExposesTypesButNoDetail() {
        val d = partitionAdvancedAnomalies(mixed, hasAnomalyDetection = false)
        assertTrue(d.isLocked)
        assertEquals(2, d.lockedCount)
        assertTrue(d.unlocked.isEmpty())
        assertEquals(
            listOf(InsightsLabels.missing, InsightsLabels.dsop),
            d.lockedLabels,
        )
        // No descriptions/amounts must appear in the locked labels.
        assertTrue(d.lockedLabels.none { it.contains("₹") || it.contains("detail") })
    }

    /** Only FREE anomalies → nothing to show on the advanced (PRO) surface. */
    @Test
    fun freeOnlyProducesNoContent() {
        val d = partitionAdvancedAnomalies(listOf(anomaly("SALARY_LOSS")), hasAnomalyDetection = false)
        assertFalse(d.hasContent)
    }

    /** Unknown type is treated as PRO (fail-closed) and labelled generically. */
    @Test
    fun unknownTypeIsLockedAsPro() {
        val d = partitionAdvancedAnomalies(listOf(anomaly("FUTURE_AUDITOR")), hasAnomalyDetection = false)
        assertTrue(d.isLocked)
        assertEquals(1, d.lockedCount)
        assertEquals(anomalyCategoryLabel("FUTURE_AUDITOR"), d.lockedLabels.single())
    }

    private object InsightsLabels {
        val missing = anomalyCategoryLabel("MISSING_ALLOWANCE")
        val dsop = anomalyCategoryLabel("DSOP_COMPLIANCE")
    }
}
