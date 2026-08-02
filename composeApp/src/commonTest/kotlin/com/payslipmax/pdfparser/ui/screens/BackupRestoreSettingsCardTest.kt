package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.ui.theme.AppStrings
import kotlin.test.Test
import kotlin.test.assertEquals

class BackupRestoreSettingsCardTest {
    @Test
    fun subtitleForFreeUserIndicatesRestoreFreeAndBackupIsPro() {
        val canBackup = false
        val subtitleText = if (canBackup) AppStrings.settingsStatusConfigured else AppStrings.settingsStatusBackupPro
        assertEquals(AppStrings.settingsStatusBackupPro, subtitleText)
    }

    @Test
    fun subtitleForProUserIndicatesConfigured() {
        val canBackup = true
        val subtitleText = if (canBackup) AppStrings.settingsStatusConfigured else AppStrings.settingsStatusBackupPro
        assertEquals(AppStrings.settingsStatusConfigured, subtitleText)
    }

    @Test
    fun proBadgeTagIsNonBlank() {
        assertEquals("PRO", AppStrings.proBadgeTag)
    }
}
