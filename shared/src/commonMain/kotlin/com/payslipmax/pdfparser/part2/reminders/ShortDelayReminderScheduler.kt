package com.payslipmax.pdfparser.part2.reminders

import com.payslipmax.pdfparser.part2.domain.Reminder
import com.payslipmax.pdfparser.subscription.isDebugBuild

/**
 * Debug-only walk aid (the `DevOverride` pattern): with the switch on, every reminder given to [inner] falls [DELAY_MILLIS] from now instead
 * of on its date, so a device walk can see a notification arrive. Keys, kinds and drafts are untouched. The switch is inert unless
 * [isDebug] (a release or TestFlight build never moves a reminder), and it lives in memory only, like `DevOverride`, so it resets on
 * every launch. This is the one place a scheduler touches an instant, and only to replace it with "soon"; it works out no date.
 */
class ShortDelayReminderScheduler(
    private val inner: ReminderScheduler,
    private val isDebug: () -> Boolean = { isDebugBuild() },
    private val now: () -> Long,
) : ReminderScheduler {
    private var short = false
    private var last: List<Reminder>? = null

    /** Whether the switch is on (always false outside a debug build). */
    val isShort: Boolean get() = short

    /** Turns the switch on or off and re-sends the last list, so it takes effect at once. A no-op outside a debug build. */
    fun useShortDelay(on: Boolean) {
        if (!isDebug() || on == short) return
        short = on
        last?.let(::replaceAll)
    }

    override fun replaceAll(reminders: List<Reminder>) {
        last = reminders
        inner.replaceAll(if (short && isDebug()) reminders.map { it.copy(at = now() + DELAY_MILLIS) } else reminders)
    }

    companion object {
        const val DELAY_MILLIS = 20_000L
    }
}
