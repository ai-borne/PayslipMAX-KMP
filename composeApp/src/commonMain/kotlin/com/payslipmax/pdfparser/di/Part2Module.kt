package com.payslipmax.pdfparser.di

import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.RoomPart2Repository
import com.payslipmax.pdfparser.part2.pages.PageEncoder
import com.payslipmax.pdfparser.part2.pages.providePageEncoder
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2LookupViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2Drafting
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2DraftsViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2Models
import com.payslipmax.pdfparser.ui.screens.guide.part2.due.DueCardModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.due.DueViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.vault.VaultViewModel
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
        single { Part2Drafting(guide = get(), repository = get()) }
        // The vault list and the maker of one order's model; the scanner is not bound (it needs a screen) and is handed in by the screens.
        single { VaultViewModel(repository = get(), encoder = get()) }
        // The Due list (the planner's plan, worded) and the Dashboard card's memory of "Not now"; both app-scoped so the card outlives a tab switch.
        single { DueViewModel(repository = get(), drafts = get()) }
        single { DueCardModel(due = get()) }
        single { Part2Models(lookup = get(), drafts = get(), drafting = get(), vault = get(), due = get()) }
    }
