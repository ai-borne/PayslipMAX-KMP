package com.payslipmax.pdfparser.di

import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.RoomPart2Repository
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
    }
