package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.calendar.CalendarDate

/** Where a draft's screens are: asking the repository, showing it, or the repository no longer has it (the level is then cut). */
enum class DraftStatus { LOADING, READY, GONE }

/** The answer a field holds. The form binds to these; nothing in them is wording, which comes from the bundle (labels) and `Part2Strings`. */
sealed interface FieldValue {
    data class Date(val date: CalendarDate?) : FieldValue

    data class Digits(val text: String) : FieldValue

    data class Words(val text: String) : FieldValue

    /** An order reference: its number as typed and its date (null while not chosen). */
    data class Reference(val number: String, val date: CalendarDate?) : FieldValue
}

/** One input: the bundle's [label] and [help], whether it is [required], what it holds and, when the answer is refused, the [error] in words. */
data class FieldUi(
    val slotId: String,
    val label: String,
    val help: String,
    val required: Boolean,
    val value: FieldValue,
    val error: String?,
)

/** A choice of a one-of question: the manual's code and its name. */
data class OptionUi(val code: String, val name: String)

/** A form question: [options] is empty for a yes/no (then [yes] is the answer) and lists the choices otherwise ([chosen] is the one held, null for none). */
data class QuestionUi(
    val id: String,
    val text: String,
    val options: List<OptionUi>,
    val yes: Boolean,
    val chosen: String?,
)

/** One occurrence the officer asked for: its code and the manual's name, the questions it raises and the inputs it needs now. */
data class RequestSection(
    val index: Int,
    val code: String,
    val title: String,
    val name: String,
    val questions: List<QuestionUi>,
    val fields: List<FieldUi>,
    val removable: Boolean,
)

/** A code the officer can add to the event. */
data class AddOption(val code: String, val name: String)

/** One row of the slip. [source] is empty for a row the officer asked for, and otherwise says where the linked order comes from. */
data class SlipRowUi(
    val code: String,
    val narrative: String,
    val from: String,
    val to: String,
    val data: List<String>,
    val authority: String?,
    val certificate: String?,
    val source: String,
)

data class SlipOrderUi(val title: String, val rows: List<SlipRowUi>)

/** A check that fired: [text] comes from the rule's id, [citation] from its declaration. [ack] findings must be [ticked] before printing. */
data class FindingUi(
    val key: String,
    val ack: Boolean,
    val text: String,
    val citation: String,
    val ticked: Boolean,
)

/** The slip as the preview draws it, every figure already worded. [readyToPrint] is true when no `ack` finding is left unticked. */
data class SlipUi(
    val lastOrder: String,
    val orders: List<SlipOrderUi>,
    val findings: List<FindingUi>,
    val undrafted: List<String>,
    val readyToPrint: Boolean,
)

/** What the preview says of a printed draft: when it was printed, and a [note] when the paper no longer matches the draft (null when it does). */
data class PrintedUi(val line: String, val note: String?)

/**
 * Everything the draft screens draw. [slip] is null until the answers are complete; then [missing] lists the labels still needed.
 * Built by `LeaveDraftStateBuilder` from the event alone, so the same answers always give the same screen.
 */
data class LeaveDraftUiState(
    val status: DraftStatus,
    val sections: List<RequestSection>,
    val addable: List<AddOption>,
    val missing: List<String>,
    val slip: SlipUi?,
    val saveFailed: Boolean,
    val printed: PrintedUi? = null,
    val printing: Boolean = false,
) {
    /** Hides every answer, like the draft itself. */
    override fun toString(): String = "LeaveDraftUiState($status, sections=${sections.size})"

    companion object {
        val Loading = LeaveDraftUiState(DraftStatus.LOADING, emptyList(), emptyList(), emptyList(), null, false)
        val Gone = LeaveDraftUiState(DraftStatus.GONE, emptyList(), emptyList(), emptyList(), null, false)
    }
}
