package com.payslipmax.pdfparser.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.Reminder
import com.payslipmax.pdfparser.part2.domain.ReminderKind
import com.payslipmax.pdfparser.part2.reminders.ShortDelayReminderScheduler
import com.payslipmax.pdfparser.testing.FakeReminderScheduler
import com.payslipmax.pdfparser.testing.WithTestKoin
import com.payslipmax.pdfparser.ui.theme.Part2ReminderStrings
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: the walk switch is the only way to see a reminder arrive on a phone without waiting for its date, so it must really bring the
 * schedule forward and put it back, and it must not exist at all in a build that is not debug or while Part II is off.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReminderDelaySwitchUiTest {
    @get:Rule
    val composeRule: AndroidComposeTestRule<*, ComponentActivity> = createAndroidComposeRule<ComponentActivity>()

    private val inner = FakeReminderScheduler()
    private val far = Reminder(ReminderKind.RETURN, "d1", 9_000_000_000L, CalendarDate(2026, 12, 1))
    private val scheduler = ShortDelayReminderScheduler(inner, isDebug = { true }, now = { 1_000L }).also { it.replaceAll(listOf(far)) }

    private fun show(
        enabled: Boolean = true,
        debug: Boolean = true,
    ) = composeRule.setContent {
        WithTestKoin(module { single { scheduler } }) { ReminderDelaySwitch(enabled, debug, Dispatchers.Unconfined) }
    }

    private fun shown() = composeRule.onAllNodes(hasText(Part2ReminderStrings.debugDelayTitle)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun flippingItBringsTheScheduleForwardAndFlippingBackRestoresIt() {
        show()
        composeRule.onNodeWithText(Part2ReminderStrings.debugDelayTitle).assertExists()
        composeRule.onNodeWithText(Part2ReminderStrings.debugDelayDesc).assertExists()

        composeRule.onNodeWithText(Part2ReminderStrings.debugDelayTitle).performClick()
        composeRule.waitForIdle()
        assertEquals(1_000L + ShortDelayReminderScheduler.DELAY_MILLIS, inner.pending.single().at)
        assertTrue(scheduler.isShort)

        composeRule.onNodeWithText(Part2ReminderStrings.debugDelayTitle).performClick()
        composeRule.waitForIdle()
        assertEquals(far.at, inner.pending.single().at)
    }

    @Test
    fun itIsNotThereOutsideADebugBuildOrWhilePartTwoIsOff() {
        show(debug = false)
        composeRule.waitForIdle()
        assertTrue(!shown())
    }

    @Test
    fun itIsNotThereWhilePartTwoIsOff() {
        show(enabled = false)
        composeRule.waitForIdle()
        assertTrue(!shown())
    }

    @Test
    fun aSwitchThatIsAlreadyOnIsShownOn() {
        scheduler.useShortDelay(true)
        show()

        composeRule.onNodeWithText(Part2ReminderStrings.debugDelayTitle).assertIsOn()
    }

    @Test
    fun aSwitchThatIsOffIsShownOff() {
        show()

        composeRule.onNodeWithText(Part2ReminderStrings.debugDelayTitle).assertIsOff()
    }
}
