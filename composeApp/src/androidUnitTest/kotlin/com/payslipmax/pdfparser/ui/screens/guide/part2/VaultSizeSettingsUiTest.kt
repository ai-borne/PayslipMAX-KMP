package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.testing.FakePart2Repository
import com.payslipmax.pdfparser.testing.SyntheticBank
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.BackupCloudNote
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.VaultSize
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.VaultSizeSettings
import com.payslipmax.pdfparser.ui.theme.OrderStrings
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.koin.compose.KoinApplication
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: the officer must see how full the order vault is in Settings, in the warning wording once past the line, and nothing at all when
 * Part II is off (no repository is bound then). The backup screen must say a backup carries the scanned pages, and only when Part II is on.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VaultSizeSettingsUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun vaultOf(bytes: Int): Part2Repository =
        FakePart2Repository(vaultBytesMax = Long.MAX_VALUE).also { repository ->
            runBlocking {
                repository.saveOrder(SyntheticBank.order("o1"))
                repository.addPage("o1", ByteArray(bytes).also { it[0] = 0xFF.toByte() })
            }
        }

    @Before
    @After
    fun noGlobalKoin() = stopKoin()

    @Test
    fun theSettingsLineShowsTheFigureWhileThereIsRoom() {
        composeRule.setContent { KoinApplication(application = { modules(module { single<Part2Repository> { vaultOf(300 * 1024) } }) }) { VaultSizeSettings() } }

        composeRule.onNodeWithText(VaultSize.usage(300L * 1024)).assertIsDisplayed()
    }

    @Test
    fun withPartTwoOffNothingIsShown() {
        composeRule.setContent { KoinApplication(application = { modules(module { }) }) { VaultSizeSettings() } }

        assertEquals(0, composeRule.onAllNodesWithText("MB", substring = true).fetchSemanticsNodes().size)
    }

    @Test
    fun theBackupNoteIsHiddenWhilePartTwoIsOff() {
        composeRule.setContent { BackupCloudNote(visible = false) }

        assertEquals(0, composeRule.onAllNodesWithText(OrderStrings.backupCloudWarning).fetchSemanticsNodes().size)
    }

    @Test
    fun theBackupNoteIsShownWhilePartTwoIsOn() {
        composeRule.setContent { BackupCloudNote(visible = true) }

        composeRule.onNodeWithText(OrderStrings.backupCloudWarning).assertIsDisplayed()
    }
}
