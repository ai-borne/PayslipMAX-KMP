package com.payslipmax.pdfparser.utils

import kotlin.test.Test

class ShareUtilsTest {
    @Test
    fun testShareTextRunsWithoutCrash() {
        shareText("Mock draft content", "Dispute Subject")
    }

    @Test
    fun testShareTextViaEmailRunsWithoutCrash() {
        shareTextViaEmail("founder@ai-borne.in", "Dispute Subject", "Mock draft content")
    }
}
