package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.InMemoryGuideWhatsNewStorage
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuidePinsStorage
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideRuleChange
import com.payslipmax.pdfparser.ui.theme.GuideMaintenanceStrings
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: the banner exists to say "something changed" once. Through the real tab: a user who has seen nothing gets the banner; dismissing it
 * (or opening the change log) saves the entry's date, the banner turns into the quiet row, and the change log stays reachable. The saved
 * date is what a restart reads back.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h2400dp")
class GuideWhatsNewSeenEndToEndTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val dispatcher = StandardTestDispatcher()
    private val seenStorage = InMemoryGuideWhatsNewStorage()
    private val nav = GuideNavState()

    private fun show() {
        val guide =
            GuideViewModel(
                FakeGuideRepository(GuideLoadResult.Loaded(SyntheticGuideRuleChange.bundle())),
                FakeCrashReporter(),
                dispatcher,
                pins = GuidePinsModel(FakeGuidePinsStorage()),
                whatsNewSeen = GuideWhatsNewSeenModel(seenStorage),
            )
        composeRule.setContent {
            GuideTab(
                navState = nav,
                access = GuideAccess(isUnlocked = true, onUnlock = {}),
                viewModel = guide,
                searchViewModel = GuideSearchViewModel(guide, dispatcher),
                platform = GuidePlatform(copy = {}, share = { _, _ -> }),
            )
        }
        settle()
    }

    private fun settle() =
        repeat(3) {
            dispatcher.scheduler.advanceUntilIdle()
            composeRule.waitForIdle()
        }

    private val row = GuideMaintenanceStrings.whatsNewRow(SyntheticGuideRuleChange.LATEST)

    @Test
    fun aFirstTimeUserSeesTheBannerAndDismissingItSavesTheDateAndLeavesTheQuietRow() {
        show()
        composeRule.onNodeWithText(GuideMaintenanceStrings.whatsNewChip).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(GuideMaintenanceStrings.whatsNewDismiss).performClick()
        settle()

        assertEquals(SyntheticGuideRuleChange.LATEST, seenStorage.load())
        assertEquals(0, composeRule.onAllNodesWithText(GuideMaintenanceStrings.whatsNewChip).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithContentDescription(GuideMaintenanceStrings.whatsNewDismiss).fetchSemanticsNodes().size)
        composeRule.onNodeWithText(row).assertIsDisplayed()
        assertEquals(listOf<GuideDestination>(GuideDestination.Home), nav.stack, "dismissing does not navigate")
    }

    @Test
    fun openingTheChangeLogCountsAsSeeingItAndTheRowStillOpensIt() {
        show()

        composeRule.onNodeWithText(row).performClick()
        settle()
        assertEquals(SyntheticGuideRuleChange.LATEST, seenStorage.load())
        assertEquals(GuideDestination.Changes, nav.current)

        nav.pop()
        settle()
        assertEquals(0, composeRule.onAllNodesWithText(GuideMaintenanceStrings.whatsNewChip).fetchSemanticsNodes().size)
        composeRule.onNodeWithText(row).performClick()
        settle()
        assertEquals(GuideDestination.Changes, nav.current, "the change log is still reachable once seen")
    }
}
