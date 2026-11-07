package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.ui.theme.OrderStrings
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: Apple requires the camera prompt's text in Info.plist, so it is the one Orders Bank string that cannot come from [OrderStrings] at
 * run time. A test keeps the two the same, so the prompt the officer reads is the copy the owner reviewed. The plist must also ask for
 * nothing else: no photo library (pages are never read from or added to Photos), microphone, location or contacts.
 */
class OrdersBankCameraUsageTest {
    private val plist = File("../iosApp/iosApp/Info.plist")

    private fun value(key: String): String? =
        Regex("<key>$key</key>\\s*<string>(.*?)</string>", RegexOption.DOT_MATCHES_ALL).find(plist.readText())?.groupValues?.get(1)?.trim()
            ?.replace("&amp;", "&")?.replace("&lt;", "<")?.replace("&gt;", ">")?.replace("&quot;", "\"")?.replace("&apos;", "'")

    @Test
    fun thePlistPromptIsExactlyTheStringObjectsCopy() {
        assertTrue(plist.isFile, "run from the composeApp module: ${plist.absolutePath}")
        assertEquals(OrderStrings.cameraUsageDescription, value("NSCameraUsageDescription"))
    }

    @Test
    fun thePromptSaysWhatItIsForAndThatPagesNeverReachPhotos() {
        val text = OrderStrings.cameraUsageDescription
        assertTrue(text.length in 40..300, "short enough for the system dialog: ${text.length}")
        assertTrue("Photos" in text && "encrypted" in text)
    }

    @Test
    fun thePlistAsksForNoOtherPrivacyPermission() {
        val keys = Regex("<key>(NS\\w+UsageDescription)</key>").findAll(plist.readText()).map { it.groupValues[1] }.toList()

        assertEquals(listOf("NSCameraUsageDescription"), keys, "the Orders Bank needs the camera only; no photo library, microphone, location or contacts")
    }
}
