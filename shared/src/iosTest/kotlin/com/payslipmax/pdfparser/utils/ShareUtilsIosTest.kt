package com.payslipmax.pdfparser.utils

import com.payslipmax.pdfparser.crypto.toByteArray
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
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
}
