package com.payslipmax.pdfparser.utils

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class)
class ShareUtilsIosTest {
    @Test
    fun testMailtoUrlEscapesQueryDelimitersSoBodyIsNotTruncated() {
        val url = buildMailtoUrlString("founder@ai-borne.in", "v1.0 & beta", "a=b&c=d+e#f\nline2")

        assertEquals(
            "mailto:founder%40ai-borne.in?subject=v1.0%20%26%20beta&body=a%3Db%26c%3Dd%2Be%23f%0Aline2",
            url,
        )
        assertEquals(1, url.count { it == '&' }, "Only the subject/body separator may remain unescaped")
    }

    @Test
    fun testMailtoUrlEncodesNonAsciiAsUtf8AndIsAcceptedByNSURL() {
        val url = buildMailtoUrlString("founder@ai-borne.in", "रिपोर्ट", "₹1,234")

        assertTrue(url.contains("%E0%A4%B0"), "Devanagari must be UTF-8 percent-encoded")
        assertTrue(url.contains("%E2%82%B9"), "Rupee sign must be UTF-8 percent-encoded")
        assertNotNull(NSURL.URLWithString(url), "Encoded output must be a valid URL for NSURL")
    }

    @Test
    fun testShareTextViaEmailRunsWithoutCrash() {
        shareTextViaEmail("founder@ai-borne.in", "Dispute Subject", "Mock draft content")
    }
}
