package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.repository.RestoreReport
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: a restore that quietly leaves rows out is lost data (plan Phase 11 item 10). The officer must be told how many, and a restore that
 * left nothing out must read exactly as it always did.
 */
class RestoreStatusTest {
    @Test
    fun aCleanRestoreReadsAsItAlwaysDid() {
        val status = restoreStatus(Result.success(RestoreReport(0)))
        assertEquals(AppStrings.statusRestoreComplete, status.message)
        assertTrue(status.isSuccess)
    }

    @Test
    fun aRestoreThatLeftRowsOutSaysHowManyAndIsNotShownAsPlainSuccess() {
        val status = restoreStatus(Result.success(RestoreReport(3)))
        assertEquals("${AppStrings.statusRestoreComplete} 3 items could not be restored.", status.message)
        assertFalse(status.isSuccess, "it shows in the warning colour so the count is noticed")
    }

    @Test
    fun oneItemIsNotPlural() {
        assertEquals("1 item could not be restored.", Part2VaultStrings.restoreSkipped(1))
        assertEquals("2 items could not be restored.", Part2VaultStrings.restoreSkipped(2))
    }

    @Test
    fun aFailedRestoreKeepsItsReason() {
        val status = restoreStatus(Result.failure(IllegalStateException("wrong password")))
        assertEquals("${AppStrings.statusRestoreFailed}wrong password", status.message)
        assertFalse(status.isSuccess)
    }
}
