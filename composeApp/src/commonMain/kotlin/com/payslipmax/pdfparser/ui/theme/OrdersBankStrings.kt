package com.payslipmax.pdfparser.ui.theme

import com.payslipmax.pdfparser.part2.domain.BankFilter
import com.payslipmax.pdfparser.part2.domain.BankStatus
import com.payslipmax.pdfparser.part2.domain.EntryProblem
import com.payslipmax.pdfparser.part2.domain.WaitingAge

/**
 * App chrome copy for the Part II Orders Bank (docs/Plan/13_OrdersBank_PhasePlan.md, Phase 3). Casualty names and the manual's column
 * captions are content from the Guide bundle, never copy here. The status words, the chips and the age wording are the owner's
 * (decision 14, 2026-10-30); the empty-state and last-backup wording are drafts for the owner to read on the phone.
 */
object OrdersBankStrings {
    // Guide Home: the Bank's own row, after the Learn row.
    const val entryEmoji = "🗂️"
    const val entryTitle = "Part II Orders Bank"
    const val entrySubtitle = "Note each casualty and keep its order copy"

    // Premium catalog.
    const val catalogIcon = "🗂️"
    const val catalogTitle = "Part II Orders Bank"
    const val catalogDescription = "Note your Part II casualties in date order and keep a copy of each order."

    // The Bank list.
    const val title = "Part II Orders Bank"
    const val addButton = "Add a casualty"
    const val emptyTitle = "Nothing noted yet"
    const val emptyBody = "When a Part II casualty happens to you, add it here. Later you can keep the copy of its order beside it."
    const val emptyFilter = "No casualty in this list."
    const val lockedNote = "Adding to the Orders Bank is part of Premium. What you have noted stays here for you to view, delete and back up."
    const val upgrade = "See Premium"
    const val backupNever = "Last backup: never. Back up from Settings."

    fun backupLine(date: String): String = "Last backup: $date"

    fun unreadable(count: Int): String = if (count == 1) "1 item could not be read and is kept untouched." else "$count items could not be read and are kept untouched."

    fun chip(filter: BankFilter): String =
        when (filter) {
            BankFilter.ALL -> "All"
            BankFilter.NOT_ATTACHED -> "Not attached"
            BankFilter.ATTACHED -> "Attached"
        }

    fun chipWithCount(
        filter: BankFilter,
        count: Int,
    ): String = "${chip(filter)} ($count)"

    fun status(status: BankStatus): String =
        when (status) {
            BankStatus.NOT_ATTACHED -> "Part II Order Copy not Attached"
            BankStatus.ATTACHED -> "Part II Order Copy Attached"
        }

    /** "Waiting 6 months": days under 30, then whole months, then years and months. */
    fun waiting(age: WaitingAge): String =
        when (age) {
            is WaitingAge.Days -> "Waiting ${plural(age.count, "day")}"
            is WaitingAge.Months -> "Waiting ${plural(age.count, "month")}"
            is WaitingAge.YearsMonths ->
                "Waiting ${plural(age.years, "year")}" + if (age.months == 0) "" else " ${plural(age.months, "month")}"
        }

    private fun plural(
        count: Int,
        unit: String,
    ): String = if (count == 1) "1 $unit" else "$count ${unit}s"

    fun yearHeading(year: Int): String = year.toString()

    // Backup and restore (Settings).
    const val restoreExistingTitle = "Orders Bank items already on this device"

    fun backedUpItems(count: Int): String = if (count == 1) "1 Orders Bank item backed up" else "$count Orders Bank items backed up"

    fun restoreSkipped(count: Int): String = if (count == 1) "1 item could not be restored." else "$count items could not be restored."

    // The entry form.
    const val newTitle = "Add to the Orders Bank"
    const val editTitle = "Your entry"
    const val viewTitle = "Your entry (view only)"
    const val fromLabel = "From"
    const val dateLabel = "Date"
    const val toLabel = "To (optional)"
    const val noteLabel = "Note (one line, optional)"
    const val add = "Add"
    const val save = "Save"
    const val delete = "Delete"
    const val cancel = "Cancel"
    const val deleteTitle = "Delete this entry?"
    const val deleteBody = "It is removed from this phone. A copy of the order, if you attached one, stays."
    const val saveFailed = "Could not save. Please try again."
    const val bankFull = "The Orders Bank is full. Delete an entry first."

    /** A column's label: its caption from the manual, or a pointer to the manual's page for a required column that prints none. */
    fun columnLabel(
        caption: String,
        required: Boolean,
        manualPage: Int,
    ): String =
        when {
            caption.isNotEmpty() -> caption
            required -> LearnStrings.requiredNoCaption(manualPage)
            else -> ""
        }

    fun problem(problem: EntryProblem): String =
        when (problem) {
            EntryProblem.MISSING -> "Required"
            EntryProblem.TOO_LONG -> "Too long"
            EntryProblem.INVALID_DATE -> "Not a real date"
            EntryProblem.AFTER_TO -> "From is after To"
        }
}
