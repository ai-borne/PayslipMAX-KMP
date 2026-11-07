package com.payslipmax.pdfparser.part2.pages

import android.net.Uri
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * WHY: ML Kit 16.0.0 reports each page as a `file://` URI into the app's cache (seen on the Pixel 9). Only that form is turned into a path
 * we then validate; anything else (a content URI, another app's file, a remote URL) is not read at all.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MlKitPagePathsTest {
    @Test
    fun aFileUriBecomesItsPath() {
        assertEquals(listOf("/data/user/0/x/cache/mlkit_docscan_ui_client/1.jpg"), MlKitScanSource.pathsOf(listOf(Uri.parse("file:///data/user/0/x/cache/mlkit_docscan_ui_client/1.jpg"))))
    }

    @Test
    fun everyOtherKindOfUriIsLeftOut() {
        val uris = listOf("content://media/external/images/1", "https://example.invalid/a.jpg", "/just/a/path", "").map(Uri::parse)

        assertEquals(emptyList(), MlKitScanSource.pathsOf(uris))
    }

    @Test
    fun theOrderIsKept() {
        assertEquals(listOf("/a/1.jpg", "/a/2.jpg"), MlKitScanSource.pathsOf(listOf(Uri.parse("file:///a/1.jpg"), Uri.parse("content://x"), Uri.parse("file:///a/2.jpg"))))
    }
}
