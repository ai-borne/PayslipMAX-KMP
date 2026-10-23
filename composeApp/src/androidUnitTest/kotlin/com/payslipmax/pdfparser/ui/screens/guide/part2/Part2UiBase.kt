package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticPart2Lookup
import com.payslipmax.pdfparser.ui.screens.guide.GuideAccess
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.GuidePlatform
import com.payslipmax.pdfparser.ui.screens.guide.GuideSearchViewModel
import com.payslipmax.pdfparser.ui.screens.guide.GuideTab
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import com.payslipmax.pdfparser.ui.theme.AppStrings
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule

/**
 * Set-up shared by the Part II browse screen tests: the real Guide tab with a Guide loaded from a bundle that carries the invented
 * Part II lookup, and a factory that records every lookup model the tab makes. A tall screen keeps every row composed, so "does not
 * exist" means left out, not scrolled off.
 */
abstract class Part2UiBase {
    @get:Rule
    val composeRule: AndroidComposeTestRule<*, ComponentActivity> = createAndroidComposeRule<ComponentActivity>()

    protected val dispatcher = StandardTestDispatcher()
    protected val crash = FakeCrashReporter()
    protected val models = mutableListOf<Part2LookupViewModel>()
    protected var unlockTaps = 0

    protected open fun bundle(): GuideBundle = SyntheticPart2Lookup.bundle()

    protected val guide by lazy { GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(bundle())), crash, dispatcher) }
    protected val search by lazy { GuideSearchViewModel(guide, dispatcher) }
    protected val platform = GuidePlatform(copy = {}, share = { _, _ -> error("the lookup must never share") })
    protected val factory: () -> Part2LookupViewModel by lazy { { Part2LookupViewModel(guide).also { models += it } } }

    protected fun show(
        nav: GuideNavState = GuideNavState(),
        unlocked: Boolean = true,
        part2: (() -> Part2LookupViewModel)? = factory,
    ): GuideNavState {
        composeRule.setContent { GuideTab(nav, GuideAccess(unlocked) { unlockTaps++ }, guide, search, platform, null, null, part2) }
        settle()
        return nav
    }

    /** Runs the work the screen started, lets Compose draw what that produced, and repeats once for the work that drawing started. */
    protected fun settle() {
        repeat(SETTLE_ROUNDS) {
            dispatcher.scheduler.advanceUntilIdle()
            composeRule.waitForIdle()
        }
    }

    protected fun tap(text: String) {
        composeRule.onAllNodesWithText(text).onFirst().performClick()
        settle()
    }

    protected fun back() {
        composeRule.onNodeWithContentDescription(AppStrings.btnBack).performClick()
        settle()
    }

    protected fun type(text: String) {
        composeRule.onNode(hasSetTextAction()).performTextInput(text)
        settle()
    }

    protected fun see(text: String) = composeRule.onNodeWithText(text, substring = true)

    protected fun count(text: String) = composeRule.onAllNodesWithText(text, substring = true)

    private companion object {
        const val SETTLE_ROUNDS = 3
    }
}
