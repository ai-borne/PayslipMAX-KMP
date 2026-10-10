package com.payslipmax.pdfparser.ui.screens.guide

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideNotesRepository
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideNotesRepository
import com.payslipmax.pdfparser.testing.FakeGuidePinsStorage
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticGuideRuleChange
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Shared set-up for the card-screen notes tests (M7): one card opened on its own over a fake notes store. No `qualifiers`: a
 * `w360dp-...` screen makes any dialog text field spin until the test JVM runs out of memory (see GuideSuggestionUiTest).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
abstract class GuideNotesUiBase {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    protected val dispatcher = StandardTestDispatcher()
    protected val crash = FakeCrashReporter()
    protected val store = FakeGuideNotesRepository(clock = { 5_000L })
    protected val platform = GuidePlatform(copy = {}, share = { _, _ -> })
    protected val old = SyntheticGuideRuleChange.OLD_CARD
    protected val new = SyntheticGuideRuleChange.NEW_CARD

    protected fun bundle(rev: String = REV): GuideBundle = SyntheticGuideBundle.withFigures().let { b -> b.copy(cards = b.cards.map { it.copy(rev = rev) }) }

    protected fun guide(bundle: GuideBundle = bundle()) = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(bundle)), crash, dispatcher, pins = GuidePinsModel(FakeGuidePinsStorage()))

    // A screen subscribes to the notes while it recomposes, after the first pass over the dispatcher, so go round more than once.
    protected fun settle() {
        repeat(3) {
            dispatcher.scheduler.advanceUntilIdle()
            composeRule.waitForIdle()
        }
    }

    protected fun tap(text: String) {
        composeRule.onNodeWithText(text).performClick()
        settle()
    }

    protected fun openCard(
        id: String = "RB-T5",
        unlocked: Boolean = true,
        guide: GuideViewModel = guide(),
        repository: GuideNotesRepository = store,
    ) {
        guide.openCard(id)
        val notes = GuideNotesViewModel(repository, guide, dispatcher)
        composeRule.setContent { GuideCardHost(GuideAccess(unlocked, onUnlock = {}), onBack = {}, viewModel = guide, platform = platform, notesViewModel = notes) }
        settle()
    }

    protected fun scrollTo(text: String) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
        composeRule.waitForIdle()
    }

    protected fun type(text: String) {
        composeRule.onNode(hasSetTextAction()).performTextInput(text)
        composeRule.waitForIdle()
    }

    protected fun stored() = runBlocking { store.observe().first().notes }

    protected fun seed(
        cardId: String,
        text: String,
        rev: String = REV,
    ) = runBlocking { store.save(cardId, text, rev) }

    protected companion object {
        const val REV = "rev00001"
    }
}
