package com.payslipmax.pdfparser.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

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
        assertEquals("Unlock Advanced Insights & Pay Audit", AppStrings.settingsPremiumPlanUpgradeSubtitlePrefix)
    }

    // Guards the Phase 8 removal of the hardcoded "₹199 / Year" fallback: price copy must never
    // name an amount, because any amount baked into the binary can contradict what the store
    // actually charges (and renders a dead product as a healthy paywall).
    @Test
    fun priceUnavailableCopyNamesNoAmount() {
        assertEquals("Pricing unavailable", AppStrings.settingsPremiumPlanPriceUnavailable)
        assertFalse(AppStrings.settingsPremiumPlanPriceUnavailable.contains("₹"))
        assertFalse(AppStrings.settingsPremiumPlanBillingNote.contains("₹"))
        assertFalse(AppStrings.settingsPremiumPlanUpgradeSubtitlePrefix.contains("₹"))
    }

    @Test
    fun lockIconMatchesExpectedGlyph() {
        assertEquals("🔒", AppStrings.premiumLockIcon)
    }
}
