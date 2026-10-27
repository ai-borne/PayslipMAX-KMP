package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2DraftEntry

/** What a case screen offers beside its codes: the entry state decides how a "Draft this order" row looks, [onDraft] starts (or refuses) one. */
class Part2DraftActions(
    val entry: Part2DraftEntry,
    val onDraft: (code: String) -> Unit,
)

/** What the Part II home shows above the ribbons: the saved drafts and what a tap on one, or on its delete button, does. */
class Part2DraftList(
    val state: DraftsUiState,
    val onOpen: (id: String) -> Unit,
    val onDelete: (id: String) -> Unit,
)

/** What the slip preview can do: edit, tick a check, delete, print, check the HRMS draft and open the draft this one was made with. One function each, so the stateless screen never holds the model. */
class SlipPreviewActions(
    val onEdit: () -> Unit,
    val onTick: (key: String, ticked: Boolean) -> Unit,
    val onDelete: () -> Unit,
    val onPrint: () -> Unit,
    val onHrms: () -> Unit,
    val onOpenPair: (draftId: String) -> Unit = {},
)

/** Everything the form can change, one function each, so a stateless screen never holds the model itself. */
class LeaveDraftActions(
    val onDate: (index: Int, slot: String, date: CalendarDate?) -> Unit,
    val onNumber: (index: Int, slot: String, raw: String) -> Unit,
    val onText: (index: Int, slot: String, raw: String) -> Unit,
    val onYears: (index: Int, slot: String, raw: String) -> Unit,
    val onReference: (index: Int, slot: String, number: String, date: CalendarDate?) -> Unit,
    val onYesNo: (index: Int, question: String, yes: Boolean) -> Unit,
    val onChoose: (index: Int, question: String, option: String?) -> Unit,
    val onAdd: (code: String) -> Unit,
    val onRemove: (index: Int) -> Unit,
    val onFill: (index: Int, replace: Boolean) -> Unit,
)

internal fun LeaveDraftViewModel.actions() =
    LeaveDraftActions(::setDate, ::setNumber, ::setText, ::setYears, ::setReference, ::setYesNo, ::choose, ::addOccurrence, ::removeOccurrence, ::fillFromLeaveBank)
