package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.part2.pages.PageScanner
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuidePart2
import com.payslipmax.pdfparser.ui.screens.guide.BankAccess
import com.payslipmax.pdfparser.ui.screens.guide.GuideAccess
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.GuidePlatform
import com.payslipmax.pdfparser.ui.screens.guide.GuideSearchViewModel
import com.payslipmax.pdfparser.ui.screens.guide.GuideTab
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.BankViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.LocalPageImageDecoder
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.LocalPageScanner
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.PageImageDecoder
import com.payslipmax.pdfparser.ui.theme.AppStrings
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule

/**
 * Set-up shared by the Learn screen tests: the real Guide tab with a Guide loaded from a bundle that carries the invented Part II
 * section, and a factory that records every Learn model the tab makes. A tall screen keeps every row composed, so "does not exist"
 * means left out, not scrolled off.
 */
abstract class Part2UiBase {
    @get:Rule
    val composeRule: AndroidComposeTestRule<*, ComponentActivity> = createAndroidComposeRule<ComponentActivity>()

    protected val dispatcher = StandardTestDispatcher()
    protected val crash = FakeCrashReporter()
    protected val models = mutableListOf<Part2LookupViewModel>()
    protected var unlockTaps = 0

    /** The phone's font scale for the next [show]: 1 by default; the accessibility tests raise it. */
    protected var fontScale = 1f

    protected open fun bundle(): GuideBundle = SyntheticGuidePart2.bundle()

    protected val guide by lazy { GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(bundle())), crash, dispatcher) }
    protected val search by lazy { GuideSearchViewModel(guide, dispatcher) }
    protected val platform = GuidePlatform(copy = {}, share = { _, _ -> error("Learn must never share") })

    /** One model for the whole test, like the app-scoped Koin single, so a query outlives the tab being left. */
    protected val model by lazy { Part2LookupViewModel(guide).also { models += it } }
    protected val factory: () -> Part2LookupViewModel = { model }

    internal fun show(
        nav: GuideNavState = GuideNavState(),
        unlocked: Boolean = true,
        part2: (() -> Part2LookupViewModel)? = factory,
        part2Bank: (() -> BankViewModel)? = null,
        bankAccess: BankAccess = BankAccess.Locked,
        scanner: PageScanner? = null,
        decoder: PageImageDecoder? = null,
    ): GuideNavState {
        composeRule.setContent {
            // The font scale is the one thing a test may change about the phone it pretends to be; at 1 nothing is wrapped.
            val density = LocalDensity.current
            val scaled = if (fontScale == 1f) density else Density(density.density, fontScale)
            CompositionLocalProvider(LocalDensity provides scaled, LocalPageScanner provides scanner, LocalPageImageDecoder provides decoder) { GuideTab(nav, GuideAccess(unlocked) { unlockTaps++ }, guide, search, platform, null, null, part2, part2Bank, bankAccess) }
        }
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
