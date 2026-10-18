package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideProfileProvider
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticPreClaimWorksheet
import com.payslipmax.pdfparser.ui.screens.guide.GuideAccess
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.GuidePlatform
import com.payslipmax.pdfparser.ui.screens.guide.GuideSearchViewModel
import com.payslipmax.pdfparser.ui.screens.guide.GuideTab
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import com.payslipmax.pdfparser.ui.theme.PreClaimStrings
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule

/**
 * Set-up shared by the pre-claim screen tests: the real Guide tab with a Guide loaded from the synthetic worksheet bundle (the owner's
 * claim A profile: level 13A, basic Rs 1,49,000, DA 46%), a clipboard that records what is copied, and a factory that records every
 * worksheet the tab makes. A tall screen keeps every row composed, so "does not exist" means left out, not scrolled off.
 */
abstract class PreClaimUiBase {
    @get:Rule
    val composeRule: AndroidComposeTestRule<*, ComponentActivity> = createAndroidComposeRule<ComponentActivity>()

    protected val dispatcher = StandardTestDispatcher()
    protected val crash = FakeCrashReporter()
    protected val payslips = FakeGuideProfileProvider(SyntheticPreClaimWorksheet.profile)
    protected val copied = mutableListOf<String>()
    protected val worksheets = mutableListOf<PreClaimViewModel>()
    protected var unlockTaps = 0

    protected open fun bundle(): GuideBundle = SyntheticPreClaimWorksheet.bundle()

    protected val guide by lazy { GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(bundle())), crash, dispatcher, profiles = payslips) }
    protected val search by lazy { GuideSearchViewModel(guide, dispatcher) }
    protected val platform = GuidePlatform(copy = { copied += it }, share = { _, _ -> error("the worksheet must never share") })
    protected val factory: () -> PreClaimViewModel = { PreClaimViewModel(guide, crash, dispatcher).also { worksheets += it } }

    protected fun show(
        nav: GuideNavState = GuideNavState(),
        unlocked: Boolean = true,
        preClaim: (() -> PreClaimViewModel)? = factory,
    ): GuideNavState {
        composeRule.setContent { GuideTab(nav, GuideAccess(unlocked) { unlockTaps++ }, guide, search, platform, null, preClaim) }
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

    /** Taps the first node with [text]: a tick's label is shown again in the portal field list further down, and the question comes first. */
    protected fun tap(text: String) {
        composeRule.onAllNodesWithText(text).onFirst().performClick()
        settle()
    }

    protected fun tapDescribed(description: String) {
        composeRule.onNodeWithContentDescription(description).performClick()
        settle()
    }

    protected fun box(label: String): SemanticsNodeInteraction = composeRule.onNode(hasSetTextAction() and hasText(label))

    protected fun type(
        label: String,
        text: String,
    ) {
        box(label).performTextClearance()
        box(label).performTextInput(text)
        settle()
    }

    protected fun hasDescriptionStartingWith(prefix: String): SemanticsMatcher =
        SemanticsMatcher("content description starts with '$prefix'") { node ->
            node.config.getOrNull(SemanticsProperties.ContentDescription)?.any { it.startsWith(prefix) } == true
        }

    /** Sets both dates on the open worksheet the way the picker's "Set date" does. */
    protected fun setDates(
        from: CalendarDate?,
        to: CalendarDate?,
    ) {
        worksheets.last().setFromDate(from)
        worksheets.last().setToDate(to)
        settle()
    }

    protected fun openFromHome(move: String) {
        tap(PreClaimStrings.entryTitle)
        tap(move)
    }

    private companion object {
        const val SETTLE_ROUNDS = 3
    }
}
