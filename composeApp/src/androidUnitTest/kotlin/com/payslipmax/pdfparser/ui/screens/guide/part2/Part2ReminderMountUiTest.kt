package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.hasText
import com.payslipmax.pdfparser.testing.FakeReminderPromptStore
import com.payslipmax.pdfparser.ui.screens.guide.part2.reminders.ReminderPromptModel
import com.payslipmax.pdfparser.ui.theme.Part2ReminderStrings
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY (DECISIONS Round 28): the one-time ask is shown where the officer is, in Part II, the first time a draft has a reminder, and nowhere
 * else. The model and the dialog are tested alone; only the Guide can prove it is mounted: that entering Part II with a reminder and no
 * earlier ask shows our explainer, that an officer already asked is left alone, and that "Not now" is remembered.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Part2ReminderMountUiTest : Part2UiBase() {
    private val has = MutableStateFlow(true)
    private val store = FakeReminderPromptStore()

    private fun explainer() = composeRule.onAllNodes(hasText(Part2ReminderStrings.promptTitle)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun enteringPartTwoWithAReminderAndNoEarlierAskShowsTheExplainer() {
        prompt = ReminderPromptModel(has, store)
        show()
        assertFalse(explainer(), "the Guide home is not Part II")

        tap(Part2Strings.entryTitle)
        settle()

        assertTrue(explainer())
    }

    @Test
    fun notNowIsRememberedAndTheExplainerDoesNotComeBack() {
        prompt = ReminderPromptModel(has, store)
        show()
        tap(Part2Strings.entryTitle)
        settle()

        tap(Part2ReminderStrings.promptNotNow)
        settle()

        assertFalse(explainer())
        assertTrue(store.wasAsked())
        assertEquals(false, prompt.visible.value)
    }

    @Test
    fun anOfficerAlreadyAskedIsLeftAlone() {
        store.markAsked()
        prompt = ReminderPromptModel(has, store)
        show()

        tap(Part2Strings.entryTitle)
        settle()

        assertFalse(explainer())
    }

    @Test
    fun withNoReminderNothingIsShown() {
        has.value = false
        prompt = ReminderPromptModel(has, store)
        show()

        tap(Part2Strings.entryTitle)
        settle()

        assertFalse(explainer())
    }
}
