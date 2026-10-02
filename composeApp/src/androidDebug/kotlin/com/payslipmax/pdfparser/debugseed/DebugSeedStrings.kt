package com.payslipmax.pdfparser.debugseed

/** Copy for the debug-only seed section; kept out of AppStrings (release copy) on purpose. */
object DebugSeedStrings {
    const val TITLE = "Debug · Synthetic Pay Audit data"
    const val DESCRIPTION =
        "Adds invented payslips for 2018-2023 (never real data) so Pay Audit can show each verdict. " +
            "Nothing is seeded until you press a button. A month that already exists is never touched."
    const val REMOVE = "Remove seed data"

    fun label(step: SeedStep): String =
        when (step) {
            SeedStep.PROVEN_TPTA -> "Issue: TPTA missing, proven (2023)"
            SeedStep.HELD_TPTA -> "Waiting: TPTA missing, held (2018)"
            SeedStep.HELD_FOR_RESOLVE -> "Held, to be resolved: Jan-Mar 2019"
            SeedStep.RESOLVE_BY_RELOCATION -> "Then import Apr 2019 (relocation: resolves)"
            SeedStep.HELD_FOR_SURFACE -> "Held, to be surfaced: Jan-Mar 2020"
            SeedStep.SURFACE_BY_SAME_CITY -> "Then import Apr 2020 (same city: surfaces)"
            SeedStep.INCREMENT_MISS -> "Issue: increment missed (Jan 2021 - Jul 2022)"
        }

    fun seeded(months: List<String>): String = "Seeded ${months.joinToString(", ")}"

    fun collision(months: List<String>): String = "Not seeded: ${months.joinToString(", ")} already exists"

    fun missingPrerequisite(step: SeedStep): String = "Seed first: ${label(step)}"

    fun removed(count: Int): String = "Removed $count seeded months"
}
