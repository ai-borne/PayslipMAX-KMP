package com.payslipmax.pdfparser.utils

import com.payslipmax.pdfparser.crypto.toByteArray
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class)
class ShareUtilsIosTest {
    @Test
    fun testPrepareShareFileCreatesAccessibleFileWithExpectedBytes() {
        val payload = "PAYSLIPMAX_BACKUP_TEST_CONTENT".encodeToByteArray()
        val fileName = "test_export_backup.pcda"

        val fileUrl = prepareShareFile(payload, fileName)

        assertNotNull(fileUrl.path, "File path must not be null")
        assertTrue(fileUrl.isFileURL(), "URL must be a file URL")
        assertTrue(fileUrl.path!!.endsWith(fileName), "Path must end with the given file name")

        val fileManager = NSFileManager.defaultManager
        assertTrue(fileManager.fileExistsAtPath(fileUrl.path!!), "File must physically exist on disk")

        val readData = NSData.dataWithContentsOfURL(fileUrl)
        assertNotNull(readData, "Read NSData must not be null")
        val readBytes = readData.toByteArray()
        assertTrue(payload.contentEquals(readBytes), "File content must match original payload bytes")
    }

    @Test
    fun testPrepareShareFilePathSanitizationNoDoubleSlashes() {
        val payload = byteArrayOf(1, 2, 3, 4)
        val fileName = "sanitize_test.pcda"
        val baseDirWithSlash = NSTemporaryDirectory().trimEnd('/') + "/"

        val fileUrl = prepareShareFile(payload, fileName, baseDir = baseDirWithSlash)
        val path = fileUrl.path!!

        assertFalse(path.contains("//"), "Sanitized path must not contain consecutive slashes")
        assertEquals("sanitize_test.pcda", fileUrl.lastPathComponent)
    }

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
