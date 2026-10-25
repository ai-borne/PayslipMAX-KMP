package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.testing.FakePart2Repository
import com.payslipmax.pdfparser.ui.screens.guide.part2.vault.VaultSizeSettings
import com.payslipmax.pdfparser.ui.screens.guide.part2.vault.VaultSizeSettingsLine
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings
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
 * WHY: Settings must warn an officer whose vault has grown past 60 MB (Round 10 and 13), and say nothing otherwise, so a small vault never
 * nags. The line reads the repository only when Part II is on: with it off nothing is bound and nothing is drawn.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class VaultSizeSettingsUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    @After
    fun noKoinLeftOver() {
        stopKoin() // KoinApplication starts the global Koin, which a finished test must not leave for the next one
    }

    private fun vaultOf(bytes: Long): Part2Repository =
        object : Part2Repository by FakePart2Repository() {
            override suspend fun vaultBytes(): Long = bytes
        }

    private fun warnings() = composeRule.onAllNodesWithText("Your vault of published orders", substring = true).fetchSemanticsNodes().size

    @Test
    fun aVaultOverTheWarningSizeShowsTheFiguresInSettings() {
        composeRule.setContent { VaultSizeSettingsLine(vaultOf(Part2Limits.VAULT_WARN_BYTES + 1)) }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText(Part2VaultStrings.sizeWarning(61, 100))[0].assertExists()
    }

    @Test
    fun aVaultAtTheWarningSizeOrUnderShowsNothing() {
        composeRule.setContent { VaultSizeSettingsLine(vaultOf(Part2Limits.VAULT_WARN_BYTES)) }
        composeRule.waitForIdle()
        assertEquals(0, warnings())
    }

    @Test
    fun noRepositoryShowsNothing() {
        composeRule.setContent { VaultSizeSettingsLine(null) }
        composeRule.waitForIdle()
        assertEquals(0, warnings())
    }

    @Test
    fun theSettingsLineTakesTheRepositoryFromKoinWhenPartTwoBindsOne() {
        composeRule.setContent { KoinApplication(application = { modules(module { single<Part2Repository> { vaultOf(70L * 1024 * 1024) } }) }) { VaultSizeSettings() } }
        composeRule.waitForIdle()
        assertEquals(1, warnings())
    }

    @Test
    fun withPartTwoOffNothingIsBoundAndNothingIsDrawn() {
        composeRule.setContent { KoinApplication(application = { modules(module { }) }) { VaultSizeSettings() } }
        composeRule.waitForIdle()
        assertEquals(0, warnings())
    }
}
