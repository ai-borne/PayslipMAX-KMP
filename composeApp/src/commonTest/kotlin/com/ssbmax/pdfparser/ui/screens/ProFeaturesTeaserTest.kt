package com.ssbmax.pdfparser.ui.screens

import com.ssbmax.pdfparser.ui.theme.AppStrings
import com.ssbmax.pdfparser.ui.theme.AppStringsPremium
import com.ssbmax.pdfparser.ui.theme.InsightsStrings
import kotlin.test.Test
import kotlin.test.assertTrue

class ProFeaturesTeaserTest {
    @Test
    fun proTitleConstantNotBlank() {
        assertTrue(AppStrings.settingsProPlanTitle.isNotBlank())
    }

    @Test
    fun proUpgradeBtnConstantNotBlank() {
        assertTrue(AppStrings.settingsProUpgradeBtn.isNotBlank())
    }

    @Test
    fun proPriceConstantNotBlank() {
        assertTrue(AppStrings.settingsProPlanPrice.isNotBlank())
    }

    @Test
    fun caReportTitleConstantNotBlank() {
        assertTrue(AppStrings.settingsAiInsightsLockedTitle.isNotBlank())
    }

    @Test
    fun premiumToolsTitleConstantNotBlank() {
        assertTrue(AppStringsPremium.premiumToolsTitle.isNotBlank())
    }

    @Test
    fun proTeaserAiDetailNotBlank() {
        assertTrue(InsightsStrings.proTeaserAiDetail.isNotBlank())
    }

    @Test
    fun proTeaserToolsDetailNotBlank() {
        assertTrue(InsightsStrings.proTeaserToolsDetail.isNotBlank())
    }
}
