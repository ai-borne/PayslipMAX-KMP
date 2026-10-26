package com.payslipmax.pdfparser.part2.reminders

import com.payslipmax.pdfparser.part2.domain.Reminder
import com.payslipmax.pdfparser.part2.domain.ReminderKind
import com.payslipmax.pdfparser.part2.domain.ReminderTestBase
import com.payslipmax.pdfparser.testing.FakeReminderScheduler
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: a reminder walk on a phone cannot wait days for a real date, so debug builds get a switch that brings every reminder forward. The
 * switch must be inert in a release or TestFlight build (a real officer's reminders must never move) and must never change what a reminder
 * is, only when it falls.
 */
class ShortDelayReminderSchedulerTest : ReminderTestBase() {
    private val inner = FakeReminderScheduler()
    private val clock = at(date(2026, 10, 1))
    private val far = Reminder(ReminderKind.RETURN, "d1", at(date(2026, 12, 1)), date(2026, 12, 1))
    private val farther = Reminder(ReminderKind.ADD_ORDER, "d2", at(date(2027, 1, 1)), date(2027, 1, 1), sequence = 2)

    private fun scheduler(debug: Boolean) = ShortDelayReminderScheduler(inner, isDebug = { debug }, now = { clock })

    @Test
    fun withTheSwitchOffEveryReminderGoesThroughUntouched() {
        scheduler(debug = true).replaceAll(listOf(far, farther))

        assertEquals(listOf(far, farther), inner.pending)
    }

    @Test
    fun inADebugBuildTheSwitchBringsEveryReminderForwardAndChangesNothingElse() {
        val scheduler = scheduler(debug = true)
        scheduler.useShortDelay(true)

        scheduler.replaceAll(listOf(far, farther))

        assertEquals(listOf(far, farther).map { it.copy(at = clock + ShortDelayReminderScheduler.DELAY_MILLIS) }, inner.pending)
        assertEquals(listOf(far.key, farther.key), inner.pending.map { it.key }, "the keys are the same, so the system replaces by key")
    }

    @Test
    fun inAReleaseBuildTheSwitchDoesNothing() {
        val scheduler = scheduler(debug = false)
        scheduler.useShortDelay(true)

        scheduler.replaceAll(listOf(far))

        assertEquals(listOf(far), inner.pending, "a real officer's reminders never move")
    }

    @Test
    fun flippingTheSwitchResendsWhatWasLastGivenAndFlippingBackRestoresIt() {
        val scheduler = scheduler(debug = true)
        scheduler.replaceAll(listOf(far))

        scheduler.useShortDelay(true)
        assertEquals(listOf(far.copy(at = clock + ShortDelayReminderScheduler.DELAY_MILLIS)), inner.pending)

        scheduler.useShortDelay(false)
        assertEquals(listOf(far), inner.pending)
    }

    @Test
    fun flippingTheSwitchBeforeAnythingWasGivenSendsNothing() {
        scheduler(debug = true).useShortDelay(true)

        assertEquals(emptyList(), inner.calls)
    }

    @Test
    fun theDelayIsShortEnoughForAWalkAndLongEnoughToLeaveTheApp() {
        assertEquals(true, ShortDelayReminderScheduler.DELAY_MILLIS in 10_000L..60_000L)
    }

    @Test
    fun aBuildThatStopsBeingDebugAfterTheSwitchWasOnSendsRealDates() {
        var debug = true
        val scheduler = ShortDelayReminderScheduler(inner, isDebug = { debug }, now = { clock })
        scheduler.useShortDelay(true)
        debug = false

        scheduler.replaceAll(listOf(far))

        assertEquals(listOf(far), inner.pending, "inert means inert at every call, not only when the switch is flipped")
    }

    @Test
    fun flippingTheSwitchToWhatItAlreadyIsSendsNothingNew() {
        val scheduler = scheduler(debug = true)
        scheduler.replaceAll(listOf(far))
        val calls = inner.calls.size

        scheduler.useShortDelay(false)
        scheduler.useShortDelay(true)
        scheduler.useShortDelay(true)

        assertEquals(calls + 1, inner.calls.size, "only the real change re-sends")
        assertEquals(true, scheduler.isShort)
    }

    @Test
    fun isShortIsFalseOutsideADebugBuildWhateverIsAsked() {
        val scheduler = scheduler(debug = false)
        scheduler.useShortDelay(true)

        assertEquals(false, scheduler.isShort)
    }
}
