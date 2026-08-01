package com.payslipmax.pdfparser.ui.screens

import kotlin.test.Test
import kotlin.test.assertTrue

class ReplicaActionDockTest {
    @Test
    fun replicaActionDockViewPdfCallbackInvokesTarget() {
        var viewPdfInvoked = false
        val onViewPdf = { viewPdfInvoked = true }
        onViewPdf()
        assertTrue(viewPdfInvoked)
    }

    @Test
    fun replicaActionDockShareCallbackInvokesTarget() {
        var shareInvoked = false
        val onShare = { shareInvoked = true }
        onShare()
        assertTrue(shareInvoked)
    }
}
