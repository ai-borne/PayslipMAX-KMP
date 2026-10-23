package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate

/** Para 7.1 splits orders by what they do: A and AM are financial, C and CM are not, B is a claim; a serial with no printed group is [UNGROUPED]. */
enum class OrderFamily { FINANCIAL, NON_FINANCIAL, CLAIM, UNGROUPED }

/** Whether a link or a rule comes from the manual (with its page) or from practice, which is never presented as the manual. */
enum class LinkBasis { MANUAL, PRACTICE }

/** `ACK`: the manual says the order will be rejected, so the officer must acknowledge it before printing; `WARN` is shown only. */
enum class RuleSeverity { WARN, ACK }

/** Why an occurrence is on the slip: the officer asked for it, or it is linked to one they asked for. */
sealed interface Part2Origin {
    data object Requested : Part2Origin

    data class Linked(val basis: LinkBasis, val page: Int?, val note: String?) : Part2Origin
}

/**
 * One row for the clerk: the printed [code] (REJOIN for REJOINAL), the [narrative], the From and To dates, Data 1-4 as printed (an
 * empty string where the manual leaves the column empty), an optional [authority] line and [certificate], and the [templateRev] it was
 * drafted from. Nothing here can hold who the officer is.
 */
data class Part2Occurrence(
    val code: String,
    val group: String?,
    val narrative: String,
    val from: CalendarDate?,
    val to: CalendarDate?,
    val data: List<String>,
    val authority: String?,
    val certificate: String?,
    val origin: Part2Origin,
    val templateRev: String,
)

/** The occurrences that go in one order, in date order (para 6.4). */
data class Part2Order(
    val family: OrderFamily,
    val occurrences: List<Part2Occurrence>,
)

/** A check that fired, with the citation the officer is shown (the manual's page and note, or practice). */
data class RuleFinding(
    val ruleId: String,
    val severity: RuleSeverity,
    val code: String,
    val basis: LinkBasis,
    val page: Int?,
    val note: String?,
)

/** An order the officer needs that has no template yet: listed so it is never silently dropped. */
data class UndraftedLink(
    val code: String,
    val origin: Part2Origin.Linked,
)

/**
 * What the officer hands the clerk: the officer's last Part II order ([lastOrder], blank when none), the [orders], the [findings] of the
 * manual's computable checks, and the linked orders that could not be drafted. It is built from the officer's own answers and the
 * templates only, never from a payslip.
 */
data class Part2Slip(
    val lastOrder: OrderRef?,
    val orders: List<Part2Order>,
    val findings: List<RuleFinding>,
    val undrafted: List<UndraftedLink>,
)

/** Why a draft was refused. Names the template and slot only, never what was typed. */
enum class DraftIssueReason { NOTHING_REQUESTED, TOO_MANY, UNKNOWN_CODE, NOT_ANSWERED, WRONG_KIND, TOO_LONG, MARKUP, OUT_OF_RANGE, BAD_DATE, DATES_REVERSED }

/** One refused answer: the template [code] ("" for the event), the [slot] id ("" when it is not about a slot) and why. */
data class DraftIssue(
    val code: String,
    val slot: String,
    val reason: DraftIssueReason,
)

/** The engine's answer: a slip, or every issue that stopped one. */
sealed interface DraftResult {
    data class Drafted(val slip: Part2Slip) : DraftResult

    data class Refused(val issues: List<DraftIssue>) : DraftResult
}
