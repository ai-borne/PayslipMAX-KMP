package com.payslipmax.pdfparser.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class AppStringsPremiumSettingsTest {
    @Test
    fun upgradeTitleMatchesExpectedCopy() {
        assertEquals("Upgrade to PayslipMax Pro", AppStrings.settingsProPlanUpgradeTitle)
    }

    @Test
    fun subscribedNoteMatchesExpectedCopy() {
        assertEquals("Subscribed (Auto-Renewing Subscription Active)", AppStrings.settingsProPlanSubscribedNote)
    }

    @Test
    fun upgradeSubtitleMatchesExpectedCopy() {
        assertEquals("Unlock Advanced Insights & Cloud Backup (₹99 / Year)", AppStrings.settingsProPlanUpgradeSubtitle)
    }

    @Test
    fun lockIconMatchesExpectedGlyph() {
        assertEquals("🔒", AppStrings.proLockIcon)
    }
}
