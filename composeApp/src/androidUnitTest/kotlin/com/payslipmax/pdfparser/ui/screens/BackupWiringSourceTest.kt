package com.payslipmax.pdfparser.ui.screens

import org.junit.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY (Phase 11 item 56): the Settings screen must hand the backup card the Part II count as well as the payslip count, and the card must
 * decide everything through [BackupContents]. A composable cannot be built here without a view model and Koin, so this pins the wiring in
 * the source: the one place that decided on the payslip count alone is gone, and nothing else decides in its place.
 */
class BackupWiringSourceTest {
    private val folder = File("src/commonMain/kotlin/com/payslipmax/pdfparser/ui/screens")

    private fun code(name: String): String =
        File(folder, name).also { assertTrue(it.isFile, "run from the composeApp module: ${it.absolutePath}") }
            .readText().lines().joinToString("\n") { it.substringBefore("//") }

    @Test
    fun theSettingsSectionHandsTheCardBothCounts() {
        val section = code("SettingsSectionComponents.kt")
        assertTrue("BackupContents(payslips = uiState.payslips.size, part2Items = viewModel.part2ItemCount())" in section)
        assertFalse("payslipCount" in section)
    }

    @Test
    fun theCardDecidesThroughTheGuideAndNeverOnThePayslipCountAlone() {
        val card = code("BackupRestoreSettingsCard.kt")
        assertFalse("payslipCount" in card, "the old payslip-only count is gone")
        for (call in listOf("backupRefusal(", "backupDoneStatus(", "restoreNeedsChoice(", "restoreChoiceTitle(")) {
            assertTrue(call in code("BackupRestoreSettingsCard.kt") + code("BackupGuard.kt"), "$call is not used")
        }
        assertTrue("restoreNeedsChoice(here)" in card && "restoreChoiceTitle(here)" in card)
        assertTrue("backupRefusal(password, contents)" in card && "backupDoneStatus(contents)" in card)
    }
}
