package com.payslipmax.pdfparser

import com.payslipmax.pdfparser.ui.PayslipUiState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MainActivitySplashConditionTest {
    @Test
    fun `keeps splash on screen while the initial payslip load is in flight`() {
        assertTrue(keepSplashOnScreen(PayslipUiState(isLoading = true)))
    }

    @Test
    fun `releases splash once the initial payslip load has completed`() {
        assertFalse(keepSplashOnScreen(PayslipUiState(isLoading = false)))
    }
}
