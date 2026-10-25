package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.ImageBitmap
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
import com.payslipmax.pdfparser.part2.domain.Part2Rules
import com.payslipmax.pdfparser.part2.pages.PageEncoder
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.FakePageScanner
import com.payslipmax.pdfparser.testing.FakePart2Repository
import com.payslipmax.pdfparser.testing.FakeSlipPrinter
import com.payslipmax.pdfparser.testing.SyntheticPart2Lookup
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.ui.screens.guide.GuideAccess
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.GuidePlatform
import com.payslipmax.pdfparser.ui.screens.guide.GuideSearchViewModel
import com.payslipmax.pdfparser.ui.screens.guide.GuideTab
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.LocalSlipPrinter
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2Drafting
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2DraftsViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2Models
import com.payslipmax.pdfparser.ui.screens.guide.part2.vault.LocalPageImageDecoder
import com.payslipmax.pdfparser.ui.screens.guide.part2.vault.LocalPageScanner
import com.payslipmax.pdfparser.ui.screens.guide.part2.vault.PageImageDecoder
import com.payslipmax.pdfparser.ui.screens.guide.part2.vault.VaultViewModel
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
    protected val repository = FakePart2Repository()

    /** What a tap on Print reaches: recorded, never a real print dialog. */
    protected val printer = FakeSlipPrinter()
    protected var unlockTaps = 0

    /** What a tap on a scan button reaches: answers [FakePageScanner.next], never a camera. */
    protected val scanner = FakePageScanner()

    /** The picture decoder the page viewer gets; it records the bytes it was handed, so a test can check they were zeroed afterwards. */
    protected var handed: ByteArray? = null
    protected var decodes = 0
    protected var decodable = true
    internal val decoder =
        PageImageDecoder { bytes ->
            handed = bytes
            decodes++
            if (decodable) ImageBitmap(1, 1) else null
        }
    private var ordered = 1
    protected val vault by lazy {
        VaultViewModel(
            repository,
            object : PageEncoder {
                override fun encode(raw: ByteArray): ByteArray = raw.copyOf()
            },
            dispatcher,
            now = { 1_000L },
            newId = { "order-${ordered++}" },
        )
    }

    protected open fun bundle(): GuideBundle = SyntheticPart2Lookup.bundle()

    protected val guide by lazy { GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(bundle())), crash, dispatcher) }
    protected val search by lazy { GuideSearchViewModel(guide, dispatcher) }
    protected val platform = GuidePlatform(copy = {}, share = { _, _ -> error("the lookup must never share") })

    /** The rules the drafting service runs: the invented ones by default, the shipped ones for a test on the real bundle. */
    protected open fun rules(): Part2Rules = SyntheticPart2Templates.registry()

    protected val drafting by lazy { Part2Drafting(guide, repository, rules(), { 1_000L }, { "draft-${drafted++}" }, dispatcher) }
    protected var drafted = 1
    protected val factory: () -> Part2Models by lazy { { Part2Models(Part2LookupViewModel(guide).also { models += it }, Part2DraftsViewModel(guide, repository, dispatcher), drafting, vault) } }

    protected fun show(
        nav: GuideNavState = GuideNavState(),
        unlocked: Boolean = true,
        part2Unlocked: Boolean = unlocked,
        part2: (() -> Part2Models)? = factory,
    ): GuideNavState {
        composeRule.setContent {
            CompositionLocalProvider(LocalSlipPrinter provides printer, LocalPageScanner provides scanner, LocalPageImageDecoder provides decoder) { GuideTab(nav, GuideAccess(unlocked, part2Unlocked) { unlockTaps++ }, guide, search, platform, null, null, part2) }
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
