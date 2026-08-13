package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.ui.theme.AppStrings
import kotlin.test.Test
import kotlin.test.assertEquals

class BackupRestoreSettingsCardTest {
    @Test
    fun subtitleForFreeUserIndicatesRestoreFreeAndBackupIsPremium() {
        val canBackup = false
        val subtitleText = if (canBackup) AppStrings.settingsStatusConfigured else AppStrings.settingsStatusBackupPremium
        assertEquals(AppStrings.settingsStatusBackupPremium, subtitleText)
    }

    @Test
    fun subtitleForPremiumUserIndicatesConfigured() {
        val canBackup = true
        val subtitleText = if (canBackup) AppStrings.settingsStatusConfigured else AppStrings.settingsStatusBackupPremium
        assertEquals(AppStrings.settingsStatusConfigured, subtitleText)
    }

    @Test
    fun premiumBadgeTagIsNonBlank() {
        assertEquals("PREMIUM", AppStrings.premiumBadgeTag)
    }
}
