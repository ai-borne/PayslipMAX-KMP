package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.ui.screens.formatProfileSubtitle
import com.payslipmax.pdfparser.ui.screens.sanitizeProfileInputs
import com.payslipmax.pdfparser.ui.theme.AppStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileOverridesSubtitleTest {
    @Test
    fun testProfileSubtitleUnconfigured() {
        val subtitle = formatProfileSubtitle("", "")
        assertEquals(AppStrings.settingsProfileRowSubtitleUnconfigured, subtitle)
    }

    @Test
    fun testProfileSubtitleNameOnly() {
        val subtitle = formatProfileSubtitle("Col. S. Sharma", "")
        assertEquals("Col. S. Sharma", subtitle)
    }

    @Test
    fun testProfileSubtitleNameAndCda() {
        val subtitle = formatProfileSubtitle("Col. S. Sharma", "123456/A")
        assertEquals("Col. S. Sharma • CDA: 123456/A", subtitle)
    }

    @Test
    fun testAppStringsConstantsNotHardcoded() {
        assertEquals("User Profile", AppStrings.settingsRowProfileLabel)
        assertEquals("User Profile", AppStrings.settingsProfileHeader)
        assertEquals(
            "Set default identity details for dashboard and replica personalization.",
            AppStrings.settingsProfileDesc,
        )
        assertTrue(AppStrings.settingsProfileInfoBannerHeader.isNotBlank())
        assertTrue(AppStrings.settingsProfileInfoBannerBullet1.contains("dashboard", ignoreCase = true))
        assertTrue(AppStrings.settingsProfileInfoBannerBullet1.contains("replica", ignoreCase = true))
        assertTrue(AppStrings.settingsProfileInfoBannerBullet2.isNotBlank())
        assertTrue(AppStrings.settingsProfileInfoBannerBullet3.contains("passcode", ignoreCase = true))
    }

    @Test
    fun testSanitizeProfileInputsTrimsWhitespace() {
        val (name, cda, pan) =
            sanitizeProfileInputs(
                "  Col. S. Sharma  ",
                " 12345/a ",
                " abcde1234f ",
            )
        assertEquals("Col. S. Sharma", name)
        assertEquals("12345/A", cda)
        assertEquals("ABCDE1234F", pan)
    }

    @Test
    fun testSanitizeProfileInputsEmptyAndBlank() {
        val (name, cda, pan) =
            sanitizeProfileInputs(
                "   ",
                "",
                "  ",
            )
        assertEquals("", name)
        assertEquals("", cda)
        assertEquals("", pan)
    }

    @Test
    fun testSanitizeProfileInputsAlreadyClean() {
        val (name, cda, pan) =
            sanitizeProfileInputs(
                "Major General",
                "67890/B",
                "XYZAB5678C",
            )
        assertEquals("Major General", name)
        assertEquals("67890/B", cda)
        assertEquals("XYZAB5678C", pan)
    }

    @Test
    fun testSanitizeProfileInputsUppercaseCdaAndPan() {
        val (name, cda, pan) =
            sanitizeProfileInputs(
                "Capt. Vikram",
                "cda/4567/x",
                "abcde1234z",
            )
        assertEquals("Capt. Vikram", name)
        assertEquals("CDA/4567/X", cda)
        assertEquals("ABCDE1234Z", pan)
    }

    @Test
    fun testSanitizeProfileInputsPreservesInternalWhitespaceInName() {
        val (name, cda, pan) =
            sanitizeProfileInputs(
                "  Brig.  A.  K.   Singh  ",
                " 123 ",
                " ABC ",
            )
        assertEquals("Brig.  A.  K.   Singh", name)
        assertEquals("123", cda)
        assertEquals("ABC", pan)
    }
}
