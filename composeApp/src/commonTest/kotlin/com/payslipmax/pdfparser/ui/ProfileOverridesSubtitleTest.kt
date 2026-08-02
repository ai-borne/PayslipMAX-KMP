package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.ui.screens.formatProfileSubtitle
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
        assertEquals("Officer Profile & Salary Settings", AppStrings.settingsRowProfileLabel)
        assertEquals("Officer Profile & Salary Settings", AppStrings.settingsProfileHeader)
        assertEquals(
            "Set default identity details for dashboard, reports & security verification.",
            AppStrings.settingsProfileDesc,
        )
        assertTrue(AppStrings.settingsProfileInfoBannerHeader.isNotBlank())
        assertTrue(AppStrings.settingsProfileInfoBannerBullet1.isNotBlank())
        assertTrue(AppStrings.settingsProfileInfoBannerBullet2.isNotBlank())
        assertTrue(AppStrings.settingsProfileInfoBannerBullet3.isNotBlank())
    }
}
