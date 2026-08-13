package com.payslipmax.pdfparser.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class AppStringsPremiumSettingsTest {
    @Test
    fun upgradeTitleMatchesExpectedCopy() {
        assertEquals("Upgrade to PayslipMax Premium", AppStrings.settingsPremiumPlanUpgradeTitle)
    }

    @Test
    fun subscribedNoteMatchesExpectedCopy() {
        assertEquals("Subscribed (Auto-Renewing Subscription Active)", AppStrings.settingsPremiumPlanSubscribedNote)
    }

    @Test
    fun upgradeSubtitlePrefixMatchesExpectedCopy() {
        assertEquals("Unlock Advanced Insights & Cloud Backup", AppStrings.settingsPremiumPlanUpgradeSubtitlePrefix)
    }

    @Test
    fun premiumPlanPriceFallbackMatchesPricingDecision() {
        assertEquals("₹199 / Year", AppStrings.settingsPremiumPlanPrice)
    }

    @Test
    fun lockIconMatchesExpectedGlyph() {
        assertEquals("🔒", AppStrings.premiumLockIcon)
    }
}
