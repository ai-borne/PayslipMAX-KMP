package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.guide.model.Part2Column
import com.payslipmax.pdfparser.guide.model.Part2Slot
import com.payslipmax.pdfparser.guide.model.Part2Template
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * How the engine uses a `derived` slot (Part II plan, phase 8): its value comes from a derive rule of the registry, reads only the slots
 * the template says it reads, prints like any slot and is never asked of the officer. The template and rules are invented (the engine
 * knows no code), so these pin how the engine works, not what the manual says.
 */
class Part2DerivedSlotsTest {
    private fun number(id: String) = Part2Slot(id, "number", "Label $id", "Help for $id.")

    private val sum = deriveRule("sum_it") { c -> if (c.values["a"] is SlotValue.NumberValue && c.values["b"] is SlotValue.NumberValue) SlotValue.NumberValue(num(c, "a") + num(c, "b")) else null }

    private fun num(
        c: DeriveContext,
        id: String,
    ) = (c.values[id] as? SlotValue.NumberValue)?.number ?: 0

    private val total = Part2Slot("total", "derived", "Total", "Worked out for you.", by = "sum_it", reads = listOf("a", "b"))

    private val template =
        Part2Template(
            code = GRANT,
            rev = "b0000001",
            page = 28,
            narrative = "Figures {a} and {b} make {total}",
            slots = listOf(number("a"), number("b"), total),
            from = Part2Column("none"),
            to = Part2Column("none"),
            data = listOf(Part2Column("required", "total"), Part2Column("none"), Part2Column("none"), Part2Column("none")),
            rules = emptyList(),
        )

    private fun engine(
        derive: DeriveRule? = sum,
        t: Part2Template = template,
    ) = Part2DraftEngine(SyntheticPart2Templates.lookup.copy(templates = listOf(t)), Part2Rules(listOfNotNull(derive)))

    private fun draft(
        engine: Part2DraftEngine,
        vararg values: Pair<String, SlotValue>,
    ) = engine.draft(LeaveEventInput(listOf(OccurrenceRequest(GRANT, values.toMap()))))

    private fun n(value: Int) = SlotValue.NumberValue(value)

    @Test
    fun aDerivedSlotPrintsInTheNarrativeAndAColumnWithTheValueTheRuleWorksOut() {
        val row = assertIs<DraftResult.Drafted>(draft(engine(), "a" to n(300), "b" to n(10))).slip.orders.single().occurrences.single()

        assertEquals("Figures 300 and 10 make 310", row.narrative)
        assertEquals(listOf("310", "", "", ""), row.data)
    }

    @Test
    fun theInputsOfAPrintedDerivedSlotAreRequiredAndAreBlamedByName() {
        val refused = assertIs<DraftResult.Refused>(draft(engine(), "a" to n(300)))
        assertEquals(listOf(DraftIssue(GRANT, "b", DraftIssueReason.NOT_ANSWERED)), refused.issues, "b is required; the derived slot is not blamed for the input that is missing")
    }

    @Test
    fun anInputTheEngineRefusedIsBlamedAloneAndTheRuleIsNotAskedToWorkFromIt() {
        val asked = mutableListOf<DeriveContext>()
        val spy =
            deriveRule("sum_it") { c ->
                asked += c
                SlotValue.NumberValue(1)
            }
        val refused = assertIs<DraftResult.Refused>(draft(engine(derive = spy), "a" to SlotValue.TextValue("not a number"), "b" to n(2)))
        assertEquals(listOf(DraftIssue(GRANT, "a", DraftIssueReason.WRONG_KIND)), refused.issues)
        assertEquals(emptyList(), asked, "no figure is worked out from an answer that was refused")
    }

    @Test
    fun aDerivedSlotNeverUsedInATextOrAColumnNeedsNoInputs() {
        val quiet = template.copy(narrative = "Figures", data = template.data.map { Part2Column("none") })
        assertIs<DraftResult.Drafted>(draft(engine(t = quiet)))
    }

    @Test
    fun aRuleThatCannotWorkTheValueOutRefusesTheDerivedSlotByItsOwnId() {
        val refused = assertIs<DraftResult.Refused>(draft(engine(derive = deriveRule("sum_it") { null }), "a" to n(1), "b" to n(2)))
        assertEquals(listOf(DraftIssue(GRANT, "total", DraftIssueReason.NOT_ANSWERED)), refused.issues)
        val missing = assertIs<DraftResult.Refused>(draft(engine(derive = null), "a" to n(1), "b" to n(2)))
        assertEquals(listOf(DraftIssue(GRANT, "total", DraftIssueReason.NOT_ANSWERED)), missing.issues, "a template naming a rule the registry lacks never prints a guess")
    }

    @Test
    fun theWorkedOutValueIsWhatARuleReadsAboutTheDraft() {
        val seen = mutableListOf<DeriveContext>()
        val spy =
            deriveRule("sum_it") { c ->
                seen += c
                SlotValue.NumberValue(7)
            }
        draft(engine(derive = spy), "a" to n(1), "b" to n(2))
        assertEquals(n(1), seen.single().values["a"])
        assertEquals(n(1), seen.single().request.values["a"])
        assertNull(seen.single().values["total"], "a derived slot's own value is not an input of itself")
    }

    @Test
    fun aDeriveRuleMayReadAValueTheEngineWorkedOutEarlierSuchAsADayCount() {
        val twice = deriveRule("sum_it") { c -> (c.values["span"] as? SlotValue.NumberValue)?.let { SlotValue.NumberValue(it.number * 2) } }
        val dated =
            template.copy(
                narrative = "Twice {twice}",
                slots =
                    listOf(
                        Part2Slot("d1", "date", "Label d1", "Help."),
                        Part2Slot("d2", "date", "Label d2", "Help."),
                        Part2Slot("span", "days", "Label span", "Help.", between = listOf("d1", "d2")),
                        Part2Slot("twice", "derived", "Label twice", "Help.", by = "sum_it", reads = listOf("span")),
                    ),
                data = listOf(Part2Column("none"), Part2Column("none"), Part2Column("none"), Part2Column("none")),
            )
        val dates = arrayOf("d1" to SlotValue.DateValue(CalendarDate(2026, 7, 1)), "d2" to SlotValue.DateValue(CalendarDate(2026, 7, 10)))
        val row = assertIs<DraftResult.Drafted>(draft(engine(derive = twice, t = dated), *dates)).slip.orders.single().occurrences.single()
        assertEquals("Twice 20", row.narrative, "10 days counted for the officer, doubled by the rule")
    }

    @Test
    fun theFormNeverAsksForADerivedSlotButAsksForItsInputs() {
        val rules = Part2Rules(listOf(sum))
        val planner = LeaveFormPlanner(engine(), rules)
        assertEquals(listOf("a", "b"), planner.fields(OccurrenceRequest(GRANT)).map { it.slot.id })
        assertEquals(listOf(true, true), planner.fields(OccurrenceRequest(GRANT)).map { it.required }, "the inputs of a printed derived slot are required")
    }

    @Test
    fun theRegistryNamesADeriveRuleADeriveRuleInTheContractComparison() {
        val rules = Part2Rules(listOf(sum))
        assertEquals(sum, rules.derivation("sum_it"))
        assertNull(rules.derivation("nothing"))
        assertNull(Part2Rules(listOf(checkRule("sum_it") { false })).derivation("sum_it"), "a check is not a derive rule")
        val declared = listOf(com.payslipmax.pdfparser.guide.model.Part2RuleDecl("sum_it", "derive", null, "manual", 133))
        assertEquals(emptyList(), rules.problemsAgainst(declared))
        assertEquals(listOf("rule sum_it is a check in the bundle but a derive in the app"), rules.problemsAgainst(listOf(declared.single().copy(kind = "check", severity = "warn"))))
    }
}
