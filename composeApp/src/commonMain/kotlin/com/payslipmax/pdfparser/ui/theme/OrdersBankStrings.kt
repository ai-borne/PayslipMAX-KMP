package com.payslipmax.pdfparser.ui.theme

import com.payslipmax.pdfparser.part2.domain.BankFilter
import com.payslipmax.pdfparser.part2.domain.BankStatus
import com.payslipmax.pdfparser.part2.domain.EntryProblem
import com.payslipmax.pdfparser.part2.domain.HubStatus
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

    // The Part II hub's top card (docs/Plan/13_OrdersBank_PhasePlan.md, Phase 8 item 2). Drafts for the owner to read on the phone.
    const val hubEmptyTitle = "Start your Part II file"
    const val hubEmptyBody = "Nothing stored yet. Learn what each order needs, then note your first casualty."
    const val hubAddFirst = "Add first casualty"
    const val hubLockEmoji = "🔒"
    const val hubPrivacy = "Encrypted and kept on this phone. Nothing is sent anywhere."
    const val hubFileTitle = "Your Part II file"
    const val hubHowTitle = "How it works"

    fun hubNoted(total: Int): String = "${LearnStrings.casualtyCount(total)} noted"

    fun hubAttachedLine(
        attached: Int,
        total: Int,
    ): String = if (attached == total) "Every order copy is attached" else "$attached of $total have their order copy attached"

    fun hubNotAttached(count: Int): String = if (count == 1) "1 order copy not attached" else "$count order copies not attached"

    const val hubTileAllAttached = "all attached"

    /** The one line under "My Part II Orders" on Guide Home once there is a file: the hub's own counts, short enough for one line on a phone. */
    fun hubTileLine(
        total: Int,
        notAttached: Int,
    ): String = "${hubNoted(total)} · ${if (notAttached == 0) hubTileAllAttached else "$notAttached not attached"}"

    /** That line, or the plain line that says what is inside while the file is empty or not yet read (nothing is claimed before it is known). */
    fun homeTileSubtitle(status: HubStatus): String =
        when (status) {
            is HubStatus.Filing -> hubTileLine(status.total, status.notAttached)
            HubStatus.Empty, HubStatus.Unknown -> LearnStrings.hubTileSubtitle
        }

    /** The three steps of "How it works", numbered from 1. */
    fun hubStep(number: Int): String =
        when (number) {
            1 -> "1. Note the casualty: pick its code and add the dates"
            2 -> "2. Attach its order: scan the paper order beside the entry"
            else -> "3. Retire with a full file: in date order, ready to print"
        }

    // Premium catalog.
    const val catalogIcon = "🗂️"
    const val catalogTitle = "Part II Orders Bank"
    const val catalogDescription = "Note your Part II casualties in date order and keep a copy of each order."

    // The Bank list.
    const val title = "Part II Orders Bank"

    /** The shortcut in the header of the entry, attach and order screens: straight to the Bank list. */
    const val toBank = "Orders Bank"
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

    // The entry form.
    const val newTitle = "Add to the Orders Bank"
    const val editTitle = "Your entry"
    const val viewTitle = "Your entry (view only)"
    const val fromLabel = "From"
    const val dateLabel = "Date"

    /** To follows the casualty's own row: plain when the manual requires it, "(optional)" only when it does not. */
    fun toLabel(required: Boolean): String = if (required) "To" else "To (optional)"

    const val noteLabel = "Note (one line, optional)"
    const val add = "Add"
    const val save = "Save"
    const val delete = "Delete"
    const val cancel = "Cancel"
    const val deleteTitle = "Delete this entry?"
    const val deleteBody = "It is removed from this phone. A copy of the order, if you attached one, stays."
    const val saveFailed = "Could not save. Please try again."

    // The snackbar after a save or a delete: the casualty code only, never a date or a note.
    fun noticeAdded(code: String) = "$code added. Order copy not attached yet"

    fun noticeSaved(code: String) = "$code saved"

    fun noticeAddedToOrder(code: String) = "$code added and ticked on this order"

    /** What a screen reader says a tap on a card's status line does. */
    const val openOrderAction = "Open the order"
    const val attachOrderAction = "Attach an order"

    fun noticeDeleted(code: String) = "$code deleted"

    const val noticeAttach = "Attach order"
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
