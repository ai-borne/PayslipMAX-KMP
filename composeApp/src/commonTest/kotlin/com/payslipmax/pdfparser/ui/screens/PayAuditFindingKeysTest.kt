package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.insights.Anomaly
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Lazy-list keys for Pay Audit findings. A duplicate key crashes Compose (Crashlytics, build 1.0.0 (16):
 * `Key "ARREARS_AUDIT_04/2026" was already used`), so keys must be unique; they must also follow the
 * finding rather than its position, or row state (the "Why?" expander) lands on the wrong card.
 */
class PayAuditFindingKeysTest {
    private fun finding(
        field: String,
        type: String = "ARREARS_AUDIT",
        month: String = "04/2026",
    ) = Anomaly(type, field, 100.0, month, "detail")

    @Test
    fun basicDaAndTptaDaArrearsInTheSameMonthGetDifferentKeys() {
        val keys = payAuditFindingKeys(listOf(finding("arrearsDa"), finding("arrearsTptaDa")))

        assertEquals(2, keys.toSet().size, "the two arrears checks share type and month; the field must tell them apart")
    }

    @Test
    fun aFindingKeepsItsKeyWhenAnotherFindingIsAddedBeforeIt() {
        val tptaDa = finding("arrearsTptaDa")
        val before = payAuditFindingKeys(listOf(tptaDa)).single()
        val after = payAuditFindingKeys(listOf(finding("basicPay", type = "SALARY_LOSS"), tptaDa)).last()

        assertEquals(before, after, "an index-based key would shift and move the expander state to another card")
    }

    @Test
    fun identicalFindingsStillGetUniqueKeysSoTheListCanNeverCrash() {
        val keys = payAuditFindingKeys(listOf(finding("arrearsDa"), finding("arrearsDa"), finding("arrearsDa")))

        assertEquals(3, keys.toSet().size)
        assertNotEquals(keys[0], keys[1])
    }
}
