package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.ANSWER_HILL
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.ANSWER_RATION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.EXTENSION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.RATION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.STOP
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What the leave form asks. It is derived from the templates the request drafts, so an officer is never asked for a date that
 * no order on their slip prints (an officer off ration strength is not asked when the ration starts). The templates are invented.
 */
class LeaveFormPlannerTest {
    private val rules = SyntheticPart2Templates.registry()
    private val engine = Part2DraftEngine(SyntheticPart2Templates.lookup, rules)
    private val planner = LeaveFormPlanner(engine, rules)

    private fun request(
        code: String = GRANT,
        vararg answers: String,
    ) = OccurrenceRequest(code, emptyMap(), answers.toSet())

    private fun ids(request: OccurrenceRequest) = planner.fields(request).map { it.slot.id }

    @Test
    fun aRequestAloneAsksOnlyForItsOwnSlotsAndNeverForOnesTheAppWorksOut() {
        assertEquals(listOf("leave_from", "leave_to", "year"), ids(request()), "days is counted for the officer, not asked")
    }

    @Test
    fun aLinkedOrderAddsItsSlotsOnlyWhileItsConditionHolds() {
        assertEquals(listOf("leave_from", "leave_to", "year", "away_from"), ids(request(GRANT, ANSWER_RATION)))
        assertEquals(listOf("leave_from", "leave_to", "year", "rejoin"), ids(request(GRANT, ANSWER_HILL)))
        assertEquals(listOf("leave_from", "leave_to", "year", "away_from", "rejoin"), ids(request(GRANT, ANSWER_RATION, ANSWER_HILL)))
    }

    @Test
    fun theTemplatesAreTheRequestedOneThenEachLinkedOneThatApplies() {
        assertEquals(listOf(GRANT), planner.templates(request()).map { it.code })
        assertEquals(listOf(GRANT, RATION), planner.templates(request(GRANT, ANSWER_RATION)).map { it.code })
        assertEquals(listOf(GRANT, RATION, STOP), planner.templates(request(GRANT, ANSWER_RATION, ANSWER_HILL)).map { it.code }, "a link to a code with no template adds none")
        assertEquals(emptyList(), planner.templates(request("NOSUCHCODE")))
    }

    @Test
    fun aSlotTwoTemplatesShareIsAskedOnce() {
        val shared = SyntheticPart2Templates.lookup.templates.map { if (it.code == RATION) it.copy(slots = it.slots + it.slots.first().copy(id = "leave_from")) else it }
        val lookup = SyntheticPart2Templates.lookup.copy(templates = shared)
        val local = Part2DraftEngine(lookup, rules)
        val fields = LeaveFormPlanner(local, rules).fields(request(GRANT, ANSWER_RATION)).map { it.slot.id }
        assertEquals(1, fields.count { it == "leave_from" })
        assertEquals("leave_from", fields.first(), "the first template to declare a slot decides where it sits")
    }

    @Test
    fun aSlotIsRequiredWhenAnOrderCannotBePrintedWithoutIt() {
        val byId = planner.fields(request(EXTENSION)).associate { it.slot.id to it.required }
        assertEquals(true, byId["ext_from"])
        assertEquals(true, byId["new_to"])
        assertEquals(false, byId["grant"], "an order reference is never required: its blank prints blank")
        assertEquals(false, byId["sanction"])
    }

    @Test
    fun aSlotWithAnIfMissingWordOrOnlyForACheckIsOptional() {
        val slots = SyntheticPart2Templates.lookup.templates.first { it.code == GRANT }.slots
        val optional =
            slots.map {
                if (it.id == "year") {
                    it.copy(ifMissing = "Nil")
                } else if (it.id == "leave_to") {
                    it.copy(forRule = "cap_days")
                } else {
                    it
                }
            }
        val lookup = SyntheticPart2Templates.lookup.copy(templates = SyntheticPart2Templates.lookup.templates.map { if (it.code == GRANT) it.copy(slots = optional) else it })
        val byId = LeaveFormPlanner(Part2DraftEngine(lookup, rules), rules).fields(request()).associate { it.slot.id to it.required }
        assertEquals(mapOf("leave_from" to true, "leave_to" to false, "year" to false), byId)
    }

    @Test
    fun theQuestionsAreThoseTheLinksConditionsNeedWhateverWasAnswered() {
        assertEquals(listOf("ration", "hill"), planner.questions(GRANT).map { it.id })
        assertEquals(emptyList(), planner.questions(EXTENSION), "a code with no conditional link asks nothing")
        assertEquals(emptyList(), planner.questions("NOSUCHCODE"))
    }

    @Test
    fun aConditionNoQuestionOwnsAsksNothing() {
        val bare = Part2Rules(rules.ids.mapNotNull { rules.check(it) ?: rules.condition(it) })
        assertEquals(emptyList(), LeaveFormPlanner(Part2DraftEngine(SyntheticPart2Templates.lookup, bare), bare).questions(GRANT))
    }

    @Test
    fun aLinkChainIsFollowedAndAQuestionIsNotAskedTwice() {
        val looped = SyntheticPart2Templates.lookup.templates.map { if (it.code == RATION) it.copy(links = listOf(com.payslipmax.pdfparser.guide.model.Part2Link(GRANT, "practice", null, null, SyntheticPart2Templates.IN_HILL_STATION))) else it }
        val lookup = SyntheticPart2Templates.lookup.copy(templates = looped)
        val local = Part2DraftEngine(lookup, rules)
        assertEquals(listOf("ration", "hill"), LeaveFormPlanner(local, rules).questions(GRANT).map { it.id })
        assertTrue(LeaveFormPlanner(local, rules).fields(request(GRANT, ANSWER_RATION, ANSWER_HILL)).isNotEmpty(), "a link back to the start must not loop")
    }

    @Test
    fun theFormSpeaksOfNoDateTheCalendarCouldHold() {
        assertFalse(com.payslipmax.pdfparser.calendar.CivilCalendar.exists(CalendarDate(0, 0, 0)))
    }

    @Test
    fun aQuestionOnlyALinkedTemplateRaisesIsStillAskedForTheCodeThatLinksToIt() {
        // why: Chapter 5 codes link to others that link on (a grant to its rejoining, which links to the area re-grants), and the
        // officer must be asked what the second hop needs, not only what the first does.
        val templates =
            SyntheticPart2Templates.lookup.templates.map {
                when (it.code) {
                    EXTENSION -> it.copy(links = listOf(com.payslipmax.pdfparser.guide.model.Part2Link(RATION, "practice", null, null, SyntheticPart2Templates.ON_RATION)))
                    GRANT -> it.copy(links = listOf(com.payslipmax.pdfparser.guide.model.Part2Link(EXTENSION, "practice", null, null, null)))
                    else -> it
                }
            }
        val local = Part2DraftEngine(SyntheticPart2Templates.lookup.copy(templates = templates), rules)
        assertEquals(listOf("ration"), LeaveFormPlanner(local, rules).questions(GRANT).map { it.id })
    }
}
