package com.payslipmax.pdfparser.di

import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.RoomPart2Repository
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2LookupViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.BankViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Part II bindings. Included only where `isPart2Enabled()` (the dark-launch flag and the Guide kill switch), so with the flag off the
 * Learn model is not bound and R8 drops it with the rest of Part II. It needs the Guide's own bindings, which it reuses. A function, so every
 * Koin start gets its own definitions (Koin keeps a `single`'s instance inside the module object, which tests that start Koin twice would share).
 */
fun part2Module(): Module =
    module {
        // One lookup model, app-scoped like the Guide's own search, so a typed query survives opening a casualty page and a tab switch.
        single { Part2LookupViewModel(guide = get()) }

        // The encrypted Bank store and its list model, app-scoped: an entry form held open survives a tab switch, never a lock or process death.
        single<Part2Repository> { RoomPart2Repository(get()) }
        single {
            val learn = get<Part2LookupViewModel>()
            BankViewModel(repository = get(), formFor = learn::entryForm, nameOf = { learn.code(it)?.name }, lastBackup = get())
        }
    }
