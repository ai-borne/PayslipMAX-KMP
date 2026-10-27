package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.domain.LeaveEntry
import com.payslipmax.pdfparser.part2.leavebank.CrossCheck

/** A fact the form states under a figure it can explain. The words are app copy (`Part2LeaveBankStrings`); a note carries numbers only. */
sealed interface FillNote {
    /** (c) is limited: [used] days of annual leave were used in the year, so at most [allowed] days can be accumulated (p.134 notes 5 and 6). */
    data class AccrualLimit(
        val used: Int,
        val allowed: Int,
    ) : FillNote

    /** The break-up has no line for these [years] between its first and its last, for the officer to type if they accumulated leave in them. */
    data class MissingYears(val years: List<Int>) : FillNote

    /** The statement's latest total is [statement] days but the break-up adds to [breakup]: shown, never changed. */
    data class StatementTotal(
        val statement: Int,
        val breakup: Int,
    ) : FillNote
}

/** A drafted total compared with the Leave Bank's figure for [year]: the year is what the line the officer reads names. */
data class YearCheck(
    val year: Int,
    val result: CrossCheck,
)

/** What asking a code's fill gave: figures to put in the form, or why there are none. */
sealed interface FillOutcome {
    /** The figures to put in, by slot id. Never empty: with nothing to put in the answer is [NothingKnown]. Hides them in [toString]. */
    class Values(val values: Map<String, SlotValue>) : FillOutcome {
        override fun toString(): String = "Values(${values.size})"
    }

    /** The fill reads the leave year and it has not been typed. */
    data object NeedsYear : FillOutcome

    /** Neither the Leave Bank nor the officer's drafts know anything for this request. */
    data object NothingKnown : FillOutcome
}

/**
 * Everything a fill may read besides the request: the Leave Bank [rows] (`LeaveEntry` only, never a payslip) and the requests of the officer's
 * other live drafts, newest edit first: [printed] those handed to the clerk and [drafted] all of them. A void draft (its order cancelled in
 * full) is in neither. Holds the officer's figures, so it prints nothing.
 */
class LeaveBankInputs(
    val rows: List<LeaveEntry>,
    val printed: List<OccurrenceRequest>,
    val drafted: List<OccurrenceRequest>,
) {
    companion object {
        /** The inputs for the draft [selfId], from the stored [drafts] and [orders]: that draft is left out, and so is every void one. */
        fun of(
            rows: List<LeaveEntry>,
            drafts: List<Part2Draft>,
            orders: List<VaultOrder>,
            selfId: String,
        ): LeaveBankInputs {
            val live = CancelStatus.liveDrafts(drafts, orders).filter { it.id != selfId }
            return LeaveBankInputs(rows, live.filter { it.printed != null }.flatMap { it.event.requests }, live.flatMap { it.event.requests })
        }
    }
}

/**
 * What a rule family can fill from the Leave Bank for one code (Part II plan, phase 8b). A family registers it for its code and the screens
 * read it through [Part2Rules.fill], so no screen or view model names a code. Pure: the rows and drafts are handed in, so the same inputs
 * always give the same answer, and the slip engine still sees no payslip.
 */
interface LeaveBankFill {
    val code: String

    /** The slots [fill] writes: the form offers a refill (and asks first) once any of them holds an answer. */
    val slots: Set<String>

    fun fill(
        request: OccurrenceRequest,
        inputs: LeaveBankInputs,
    ): FillOutcome

    /** What the form says about the figures [request] holds now, worked out from them, so a typed figure is explained as well as a filled one. */
    fun notes(
        request: OccurrenceRequest,
        rows: List<LeaveEntry>,
    ): List<FillNote>

    /** The drafted total against the statement, when this code has one to compare (null otherwise, and while its figures are incomplete). */
    fun crossCheck(
        request: OccurrenceRequest,
        rows: List<LeaveEntry>,
    ): YearCheck? = null
}
