package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.Part2Case
import com.payslipmax.pdfparser.guide.model.Part2CodePage
import com.payslipmax.pdfparser.guide.model.Part2Column
import com.payslipmax.pdfparser.guide.model.Part2Link
import com.payslipmax.pdfparser.guide.model.Part2Lookup
import com.payslipmax.pdfparser.guide.model.Part2Ribbon
import com.payslipmax.pdfparser.guide.model.Part2RuleDecl
import com.payslipmax.pdfparser.guide.model.Part2Slot
import com.payslipmax.pdfparser.guide.model.Part2Template
import com.payslipmax.pdfparser.guide.model.Part2TemplateLimits
import com.payslipmax.pdfparser.part2.domain.AnswerQuestion
import com.payslipmax.pdfparser.part2.domain.CheckRule
import com.payslipmax.pdfparser.part2.domain.ConditionRule
import com.payslipmax.pdfparser.part2.domain.Part2Rules
import com.payslipmax.pdfparser.part2.domain.RuleContext
import com.payslipmax.pdfparser.part2.domain.SlotValue

/**
 * Invented Part II rules and templates for engine, validator and UI tests: made-up codes and made-up wording, so no manual text sits in
 * a fixture and an edit to the real content never breaks a test. It mirrors `tools/part2_synthetic.py` (which the real Python tools
 * turn into `synthetic_section.json`); `Part2EngineIntegrationTest` proves the two stay equal. Fixed points the tests lean on:
 * [GRANT] is a Group A request with two linked orders and one link to a code that has no template ([NO_TEMPLATE]); [CLAIM] is Group B;
 * [REJOIN_ALL] prints as REJOIN; [UNGROUPED] is a serial the manual prints no group for.
 */
object SyntheticPart2Templates {
    const val GRANT = "ALPHAGRANT"
    const val EXTENSION = "ALPHAEXT"
    const val RATION = "BETARATION"
    const val STOP = "BETASTOP"
    const val CLAIM = "GAMMACLAIM"
    const val NOTE = "DELTANOTE"
    const val REJOIN_ALL = "REJOINAL"
    const val UNGROUPED = "STOPNOGROUP"
    const val NO_TEMPLATE = "ZETAPLAIN"

    const val CAP_DAYS = "cap_days"
    const val GRANT_QUOTED = "grant_quoted"
    const val ON_RATION = "on_ration"
    const val IN_HILL_STATION = "in_hill_station"

    /** The answers (not slots) the conditions read. */
    const val ANSWER_RATION = "ration"
    const val ANSWER_HILL = "hill"

    val limits = Part2TemplateLimits(narrative = 300, label = 60, help = 200, slots = 8, links = 6, rules = 4, note = 20)

    val rules: List<Part2RuleDecl> =
        listOf(
            Part2RuleDecl(CAP_DAYS, "check", "warn", "manual", 115, "2"),
            Part2RuleDecl(GRANT_QUOTED, "check", "ack", "manual", 131),
            Part2RuleDecl(ON_RATION, "condition", null, "manual", 97, "1.1"),
            Part2RuleDecl(IN_HILL_STATION, "condition", null, "practice"),
        )

    private fun slot(
        id: String,
        kind: String,
        label: String,
        help: String,
        vararg between: String,
    ) = Part2Slot(id, kind, label, help, between.toList())

    private fun need(slot: String) = Part2Column("required", slot)

    private val none = Part2Column("none")
    private val noColumns = List(4) { none }

    private val leaveFrom = slot("leave_from", "date", "First day", "The first day of the invented thing.")
    private val leaveTo = slot("leave_to", "date", "Last day", "The last day of the invented thing.")
    private val grant = slot("grant", "ref", "Grant order", "The order that granted it, once published.")

    val templates: List<Part2Template> =
        listOf(
            Part2Template(
                code = GRANT,
                rev = "a0000001",
                page = 28,
                narrative = "Granted the first thing for {days} days in {year} from {leave_from} to {leave_to}",
                slots =
                    listOf(
                        leaveFrom,
                        leaveTo,
                        slot("days", "days", "Days", "Counted for you, both days included.", "leave_from", "leave_to"),
                        slot("year", "number", "Year", "The year it belongs to."),
                    ),
                from = need("leave_from"),
                to = need("leave_to"),
                data = listOf(need("days"), need("year"), none, none),
                rules = listOf(CAP_DAYS),
                links =
                    listOf(
                        Part2Link(RATION, "manual", 97, "1.1", ON_RATION),
                        Part2Link(STOP, "practice", null, null, IN_HILL_STATION),
                        Part2Link(NO_TEMPLATE, "practice", null, null, IN_HILL_STATION),
                    ),
            ),
            Part2Template(
                code = EXTENSION,
                rev = "a0000002",
                page = 29,
                narrative = "Extended the first thing vide {grant.no} dated {grant.date} up to {new_to}",
                slots =
                    listOf(
                        grant,
                        slot("ext_from", "date", "From", "The day the extension starts."),
                        slot("new_to", "date", "Up to", "The new last day."),
                        slot("sanction", "ref", "Sanction", "The signal that allowed it."),
                    ),
                from = need("ext_from"),
                to = need("new_to"),
                data = listOf(need("grant.no"), need("grant.date"), none, none),
                authority = "Sanction {sanction.no} dated {sanction.date}",
                rules = listOf(GRANT_QUOTED),
            ),
            Part2Template(
                code = RATION,
                rev = "a0000003",
                page = 29,
                narrative = "Granted the second thing w.e.f. {away_from}",
                slots = listOf(slot("away_from", "date", "First day away", "Counts any days before the leave starts.")),
                from = need("away_from"),
                to = none,
                data = noColumns,
                certificate = "Certified that the second thing is in order",
            ),
            Part2Template(
                code = STOP,
                rev = "a0000004",
                page = 30,
                narrative = "Stopped the second thing w.e.f. {rejoin}",
                slots = listOf(slot("rejoin", "date", "Back on", "The day you are back.")),
                from = need("rejoin"),
                to = none,
                data = noColumns,
            ),
            Part2Template(
                code = CLAIM,
                rev = "a0000005",
                page = 39,
                narrative = "Claimed the third thing for {claim_on}",
                slots = listOf(slot("claim_on", "date", "Claim date", "The day the claim is for.")),
                from = need("claim_on"),
                to = none,
                data = noColumns,
            ),
            Part2Template(
                code = NOTE,
                rev = "a0000006",
                page = 40,
                narrative = "Noted the fourth thing on {noted_on} as {remark}",
                slots =
                    listOf(
                        slot("noted_on", "date", "Noted on", "The day it was noted."),
                        slot("remark", "text", "Remark", "A few plain words."),
                    ),
                from = need("noted_on"),
                to = none,
                data = listOf(need("remark"), none, none, none),
            ),
            Part2Template(
                code = REJOIN_ALL,
                rev = "a0000007",
                print = "REJOIN",
                page = 46,
                narrative = "Rejoined w.e.f. {rejoin_on} vide {grant.no} dated {grant.date}",
                slots = listOf(slot("rejoin_on", "date", "Back on", "The day you are back."), grant),
                from = need("rejoin_on"),
                to = none,
                data = listOf(need("grant.no"), need("grant.date"), none, none),
                rules = listOf(GRANT_QUOTED),
            ),
            Part2Template(
                code = UNGROUPED,
                rev = "a0000008",
                page = 57,
                narrative = "Stopped the fifth thing w.e.f. {stop_on}",
                slots = listOf(slot("stop_on", "date", "Stopped on", "The day it stopped.")),
                from = need("stop_on"),
                to = none,
                data = noColumns,
            ),
        )

    private val pages: List<Part2CodePage> =
        listOf(
            Part2CodePage("1.1", 1, listOf(GRANT), "A", "Invented thing $GRANT", 28),
            Part2CodePage("1.2", 1, listOf(EXTENSION), "A", "Invented thing $EXTENSION", 28),
            Part2CodePage("1.3", 1, listOf(RATION), "A", "Invented thing $RATION", 29),
            Part2CodePage("1.4", 1, listOf(STOP), "AM", "Invented thing $STOP", 29),
            Part2CodePage("2.1", 2, listOf(CLAIM), "B", "Invented thing $CLAIM", 39),
            Part2CodePage("2.2", 2, listOf(NOTE), "C", "Invented thing $NOTE", 40),
            Part2CodePage("3.1", 3, listOf(REJOIN_ALL), "CM", "Invented thing $REJOIN_ALL", 46),
            Part2CodePage("3.28", 3, listOf(UNGROUPED), null, "Invented thing $UNGROUPED", 57),
            Part2CodePage("4.1", 4, listOf(NO_TEMPLATE), "C", "Invented thing $NO_TEMPLATE", 50),
        )

    /** The synthetic lookup (`SyntheticGuidePart2`'s shape and limits) widened to these codes, with [rules] and [templates] attached. */
    val lookup: Part2Lookup =
        SyntheticGuidePart2.lookup.copy(
            limits = SyntheticGuidePart2.lookup.limits.copy(caseSerialsMax = CASE_SERIALS_MAX),
            serials = pages,
            ribbons =
                listOf(
                    Part2Ribbon(
                        "leave",
                        "The first ribbon",
                        true,
                        listOf(
                            Part2Case("leave_one", "First case", listOf("1.1", "1.2", "1.3", "1.4")),
                            Part2Case("leave_two", "Second case", listOf("2.1", "2.2", "3.1", "3.28", "4.1"), lookup = listOf("3.28")),
                        ),
                    ),
                ),
            templateLimits = limits,
            rules = rules,
            templates = templates,
        )

    fun bundle(): GuideBundle = SyntheticGuidePart2.bundle().copy(part2 = lookup)

    /** The rule implementations the synthetic declarations name, so the contract between bundle and registry can be tested. */
    fun registry(): Part2Rules =
        Part2Rules(
            questions = questions,
            rules =
                listOf(
                    object : CheckRule {
                        override val id = CAP_DAYS

                        override fun violated(context: RuleContext) = (context.occurrence.days("days") ?: 0) > ANNUAL_CAP
                    },
                    object : CheckRule {
                        override val id = GRANT_QUOTED

                        override fun violated(context: RuleContext) = context.request.values["grant"] !is SlotValue.RefValue
                    },
                    object : ConditionRule {
                        override val id = ON_RATION

                        override fun holds(context: RuleContext) = ANSWER_RATION in context.request.answers
                    },
                    object : ConditionRule {
                        override val id = IN_HILL_STATION

                        override fun holds(context: RuleContext) = ANSWER_HILL in context.request.answers
                    },
                ),
        )

    /** The questions the synthetic conditions are answered by: one yes/no each, so a form over these templates asks two. */
    val questions: List<AnswerQuestion> =
        listOf(
            AnswerQuestion.YesNo("ration", ANSWER_RATION, setOf(ON_RATION)),
            AnswerQuestion.YesNo("hill", ANSWER_HILL, setOf(IN_HILL_STATION)),
        )

    private const val ANNUAL_CAP = 60
    private const val CASE_SERIALS_MAX = 8
}
