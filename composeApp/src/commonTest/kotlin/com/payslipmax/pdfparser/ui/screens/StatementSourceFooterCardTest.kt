package com.payslipmax.pdfparser.ui.screens

import kotlin.test.Test
import kotlin.test.assertTrue

class StatementSourceFooterCardTest {
    @Test
    fun statementSourceFooterCardViewPdfCallbackInvokesTarget() {
        var viewPdfInvoked = false
        val onViewPdf = { viewPdfInvoked = true }
        onViewPdf()
        assertTrue(viewPdfInvoked)
    }

    @Test
    fun statementSourceFooterCardShareCallbackInvokesTarget() {
        var shareInvoked = false
        val onShare = { shareInvoked = true }
        onShare()
        assertTrue(shareInvoked)
    }
}
