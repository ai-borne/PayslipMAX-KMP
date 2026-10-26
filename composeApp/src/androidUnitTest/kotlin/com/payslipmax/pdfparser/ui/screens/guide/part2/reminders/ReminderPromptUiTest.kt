package com.payslipmax.pdfparser.ui.screens.guide.part2.reminders

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.payslipmax.pdfparser.testing.FakeNotificationPermission
import com.payslipmax.pdfparser.testing.FakeReminderPromptStore
import com.payslipmax.pdfparser.ui.theme.Part2ReminderStrings
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY (DECISIONS Round 28): the officer sees our own sentence first, and only "Allow" reaches the system dialog; "Not now" and tapping
 * outside are a final decline. Nothing is shown before there is a reminder, and nothing twice.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReminderPromptUiTest {
    @get:Rule
    val composeRule: AndroidComposeTestRule<*, ComponentActivity> = createAndroidComposeRule<ComponentActivity>()

    private val has = MutableStateFlow(false)
    private val store = FakeReminderPromptStore()
    private val permission = FakeNotificationPermission()
    private val model = ReminderPromptModel(has, store)

    private fun show() = composeRule.setContent { ReminderPromptHost(model, permission) }

    private fun shown() = composeRule.onAllNodes(androidx.compose.ui.test.hasText(Part2ReminderStrings.promptTitle)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun nothingIsShownUntilADraftSetsAReminder() {
        show()
        composeRule.waitForIdle()
        assertFalse(shown())

        has.value = true
        composeRule.waitForIdle()

        assertTrue(shown())
        composeRule.onNodeWithText(Part2ReminderStrings.promptBody).assertExists()
    }

    @Test
    fun allowShowsTheSystemDialogOnceAndClosesTheExplainer() {
        has.value = true
        show()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(Part2ReminderStrings.promptAllow).performClick()
        composeRule.waitForIdle()

        assertEquals(1, permission.requests)
        assertFalse(shown())
        assertTrue(store.wasAsked())
    }

    @Test
    fun notNowClosesTheExplainerWithoutTheSystemDialogAndNeverReturns() {
        has.value = true
        show()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(Part2ReminderStrings.promptNotNow).performClick()
        composeRule.waitForIdle()
        has.value = false
        has.value = true
        composeRule.waitForIdle()

        assertEquals(0, permission.requests)
        assertFalse(shown())
        assertTrue(store.wasAsked())
    }

    @Test
    fun aDeviceWithNothingToAskNeverShowsTheExplainer() {
        permission.askable = false
        has.value = true
        show()
        composeRule.waitForIdle()

        assertFalse(shown())
        assertTrue(store.wasAsked())
    }

    @Test
    fun goingBackFromTheExplainerIsAFinalDecline() {
        has.value = true
        show()
        composeRule.waitForIdle()

        composeRule.runOnUiThread { org.robolectric.shadows.ShadowDialog.getLatestDialog().onBackPressed() }
        composeRule.waitForIdle()

        assertFalse(shown())
        assertTrue(store.wasAsked())
        assertEquals(0, permission.requests)
    }
}
