package com.payslipmax.pdfparser.di

import com.payslipmax.pdfparser.nav.PendingPart2Link
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.RoomPart2Repository
import com.payslipmax.pdfparser.part2.domain.ReminderPlanner
import com.payslipmax.pdfparser.part2.leavebank.LeaveBankFeed
import com.payslipmax.pdfparser.part2.leavebank.LeaveBankSource
import com.payslipmax.pdfparser.part2.pages.PageEncoder
import com.payslipmax.pdfparser.part2.pages.providePageEncoder
import com.payslipmax.pdfparser.part2.reminders.ReminderFireCheck
import com.payslipmax.pdfparser.part2.reminders.ReminderScheduler
import com.payslipmax.pdfparser.part2.reminders.ReminderSync
import com.payslipmax.pdfparser.part2.reminders.ShortDelayReminderScheduler
import com.payslipmax.pdfparser.part2.reminders.provideReminderPromptStore
import com.payslipmax.pdfparser.rating.currentTimeMillis
import com.payslipmax.pdfparser.ui.platform.provideReminderScheduler
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2LookupViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2Drafting
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2DraftsViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2Models
import com.payslipmax.pdfparser.ui.screens.guide.part2.due.DueCardModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.due.DueViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.reminders.ReminderPromptModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.vault.VaultViewModel
import com.payslipmax.pdfparser.ui.theme.Part2ReminderStrings
import com.payslipmax.pdfparser.utils.localUtcOffsetMillis
import org.koin.dsl.module

/**
 * Part II bindings. Included only where `isPart2Enabled()` (the dark-launch flag and the Guide kill switch), so with the flag off the
 * repository is not bound and R8 drops it with the rest of Part II. The tables, their DAO and the backup of their rows are bound by
 * `sharedModule` whatever the flag says: the schema always has them, and a backup must never skip rows the officer made.
 */
val part2Module =
    module {
        // One repository, so every screen sees the same drafts and orders.
        single<Part2Repository> { RoomPart2Repository(dao = get()) }
        // The platform's page encoder (stateless; the vault screens scan through it). The scanner is not bound here: it needs a screen.
        single<PageEncoder> { providePageEncoder() }
        // One lookup model, app-scoped like the Guide's own search, so a typed query survives opening a code page and a tab switch.
        single { Part2LookupViewModel(guide = get()) }
        // The drafts list and the maker of one draft share the repository and the loaded Guide; `Part2Models` is what the Guide tab is handed.
        single { Part2DraftsViewModel(guide = get(), repository = get()) }
        // The Leave Bank's rows for the year-end forms: one feed, attached to the app's one Leave Bank summary by the Guide tab (`attachPart2LeaveBank`);
        // the kill switch decides whether the draft maker is given it.
        single { LeaveBankFeed() }
        single<LeaveBankSource> { get<LeaveBankFeed>() }
        single { Part2Drafting(guide = get(), repository = get(), leaveBank = leaveBankFor(get())) }
        // The vault list and the maker of one order's model; the scanner is not bound (it needs a screen) and is handed in by the screens.
        single { VaultViewModel(repository = get(), encoder = get()) }
        // The Due list (the planner's plan, worded) and the Dashboard card's memory of "Not now"; both app-scoped so the card outlives a tab switch.
        single { DueViewModel(repository = get(), drafts = get()) }
        single { DueCardModel(due = get()) }
        // Reminders (Phase 7b). One planner for the sync and the worker's check; one scheduler, which is the debug walk decorator around the
        // platform's (inert outside a debug build); one sync that keeps the system equal to the drafts (started by the platform entry points
        // with the entitlement, never here); one pending tap, outliving the lock screen; one-time ask whose flag is a device-local preference.
        single { ReminderPlanner(offsetAt = ::localUtcOffsetMillis) }
        single { Part2ReminderStrings.notificationText() }
        single { ShortDelayReminderScheduler(inner = provideReminderScheduler(get()), now = ::currentTimeMillis) }
        single<ReminderScheduler> { get<ShortDelayReminderScheduler>() }
        single { ReminderSync(repository = get(), scheduler = get(), planner = get(), now = ::currentTimeMillis) }
        single { ReminderFireCheck(repository = get(), planner = get(), now = ::currentTimeMillis) }
        single { PendingPart2Link(repository = get()) }
        single { ReminderPromptModel(hasReminders = get<ReminderSync>().hasReminders, store = provideReminderPromptStore()) }
        single { Part2Models(lookup = get(), drafts = get(), drafting = get(), vault = get(), due = get(), reminders = get()) }
    }
