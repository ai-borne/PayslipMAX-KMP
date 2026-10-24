package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.ui.theme.Part2Strings
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: Apple requires the camera prompt's text in Info.plist, so it is the one Part II string that cannot come from `Part2Strings` at
 * run time. A test keeps the two the same, so the prompt the officer reads is the copy the owner reviewed. The plist must also ask for
 * nothing else: no photo library (pages are never read from or added to Photos), microphone, location or contacts.
 */
class Part2CameraUsageTest {
    private val plist = File("../iosApp/iosApp/Info.plist")

    private fun value(key: String): String? =
        Regex("<key>$key</key>\\s*<string>(.*?)</string>", RegexOption.DOT_MATCHES_ALL).find(plist.readText())?.groupValues?.get(1)?.trim()
            ?.replace("&amp;", "&")?.replace("&lt;", "<")?.replace("&gt;", ">")?.replace("&quot;", "\"")?.replace("&apos;", "'")

    @Test
    fun thePlistPromptIsExactlyThePart2StringsCopy() {
        assertTrue(plist.isFile, "run from the composeApp module: ${plist.absolutePath}")
        assertEquals(Part2Strings.cameraUsageDescription, value("NSCameraUsageDescription"))
    }

    @Test
    fun thePromptSaysWhatItIsForAndThatPagesNeverReachPhotos() {
        val text = Part2Strings.cameraUsageDescription
        assertTrue(text.length in 40..300, "short enough for the system dialog: ${text.length}")
        assertTrue("Photos" in text && "encrypted" in text)
    }

    @Test
    fun thePlistAsksForNoOtherPrivacyPermission() {
        val keys = Regex("<key>(NS\\w+UsageDescription)</key>").findAll(plist.readText()).map { it.groupValues[1] }.toList()

        assertEquals(listOf("NSCameraUsageDescription"), keys, "Part II needs the camera only; no photo library, microphone, location or contacts")
    }
}
