package com.payslipmax.pdfparser.ui.screens.guide.part2.due

import com.payslipmax.pdfparser.part2.domain.ReminderPlanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** A second past the hour, so the clock read after the wait is on the new hour and not just before it. */
private const val HOUR_MARGIN_MS = 1_000L

/**
 * One tick just after every local hour, for ever: a reminder falls due on the hour and "overdue" turns over at midnight, so a Due list that stays open
 * is rebuilt when something on it can change, not only when a draft or an order does. The hour is the planner's, in the officer's zone; this does no
 * date arithmetic. It ends when nobody collects it (the list is gone from the screen).
 */
internal fun hourlyTicks(
    planner: ReminderPlanner,
    now: () -> Long,
): Flow<Unit> =
    flow {
        while (true) {
            delay(planner.millisUntilNextHour(now()) + HOUR_MARGIN_MS)
            emit(Unit)
        }
    }
