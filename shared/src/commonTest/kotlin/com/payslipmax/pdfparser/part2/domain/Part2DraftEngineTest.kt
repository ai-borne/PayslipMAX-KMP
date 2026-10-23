package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.guide.model.Part2Link
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.CLAIM
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.EXTENSION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.NO_TEMPLATE
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.RATION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.REJOIN_ALL
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.STOP
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * What the officer is handed: the occurrence rows for a clerk, the checks the manual's notes imply, and the linked orders. The
 * templates here are invented (`SyntheticPart2Templates`), so these tests pin how the engine uses a template, not what the manual says.
 */
class Part2DraftEngineTest {
    private val engine = Part2DraftEngine(SyntheticPart2Templates.lookup, SyntheticPart2Templates.registry())

    private fun day(
        month: Int,
        day: Int,
        year: Int = 2026,
    ) = SlotValue.DateValue(CalendarDate(year, month, day))

    /** A full set of answers for a [GRANT] event: leave 05 Jul - 04 Sep 2026 (62 days), prefix from 01 Jul, back on 05 Sep. */
    private val leave: Map<String, SlotValue> =
        mapOf(
            "leave_from" to day(7, 5),
            "leave_to" to day(9, 4),
            "year" to SlotValue.NumberValue(2026),
            "away_from" to day(7, 1),
            "rejoin" to day(9, 5),
        )

    private fun request(
        code: String = GRANT,
        values: Map<String, SlotValue> = leave,
        vararg answers: String,
    ) = OccurrenceRequest(code, values, answers.toSet())

    private fun draft(vararg requests: OccurrenceRequest) = engine.draft(LeaveEventInput(requests.toList()))

    private fun slip(vararg requests: OccurrenceRequest): Part2Slip = assertIs<DraftResult.Drafted>(draft(*requests)).slip

    private fun refused(vararg requests: OccurrenceRequest): List<DraftIssue> = assertIs<DraftResult.Refused>(draft(*requests)).issues

    private fun rows(slip: Part2Slip) = slip.orders.flatMap { it.occurrences }

    @Test
    fun aRequestAloneBecomesOneOccurrenceWithItsDatesDataAndNarrative() {
        val only = rows(slip(request())).single()
        assertEquals(GRANT, only.code)
        assertEquals("A", only.group)
        assertEquals("Granted the first thing for 62 days in 2026 from 05/07/2026 to 04/09/2026", only.narrative)
        assertEquals(CalendarDate(2026, 7, 5), only.from)
        assertEquals(CalendarDate(2026, 9, 4), only.to)
        assertEquals(listOf("62", "2026", "", ""), only.data, "a column the manual does not require stays empty")
        assertEquals(Part2Origin.Requested, only.origin)
        assertEquals("a0000001", only.templateRev)
    }

    @Test
    fun linkedOrdersJoinInDateOrderEachLabelledWithWhyItIsThere() {
        val orders = slip(request(GRANT, leave, SyntheticPart2Templates.ANSWER_RATION, SyntheticPart2Templates.ANSWER_HILL)).orders
        val order = orders.single()
        assertEquals(OrderFamily.FINANCIAL, order.family)
        assertEquals(listOf(RATION, GRANT, STOP), order.occurrences.map { it.code }, "01 Jul, 05 Jul, 05 Sep: para 6.4 date order, not drafting order")
        assertEquals(Part2Origin.Linked(LinkBasis.MANUAL, 97, "1.1"), order.occurrences[0].origin)
        assertEquals(Part2Origin.Requested, order.occurrences[1].origin)
        assertEquals(Part2Origin.Linked(LinkBasis.PRACTICE, null, null), order.occurrences[2].origin, "practice is never presented as the manual")
    }

    @Test
    fun aLinkWhoseConditionDoesNotHoldIsNotDrafted() {
        assertEquals(listOf(GRANT), rows(slip(request())).map { it.code })
        assertEquals(listOf(RATION, GRANT), rows(slip(request(GRANT, leave, SyntheticPart2Templates.ANSWER_RATION))).map { it.code })
        assertEquals(listOf(GRANT, STOP), rows(slip(request(GRANT, leave, SyntheticPart2Templates.ANSWER_HILL))).map { it.code })
    }

    @Test
    fun aLinkToACodeWithNoTemplateIsListedNotSilentlyDropped() {
        // why: the officer must still learn a linked order exists even before its template is written (later content batches).
        val result = slip(request(GRANT, leave, SyntheticPart2Templates.ANSWER_HILL))
        assertEquals(listOf(UndraftedLink(NO_TEMPLATE, Part2Origin.Linked(LinkBasis.PRACTICE, null, null))), result.undrafted)
        assertEquals(emptyList(), slip(request()).undrafted)
    }

    @Test
    fun aCertificateAndAnAuthorityLineAreFilledFromTheTemplate() {
        val ration = rows(slip(request(GRANT, leave, SyntheticPart2Templates.ANSWER_RATION))).first { it.code == RATION }
        assertEquals("Certified that the second thing is in order", ration.certificate)
        assertEquals(null, ration.authority)
        val sanction = SlotValue.RefValue(OrderRef("SIG/77", CalendarDate(2026, 6, 20)))
        val extension = rows(slip(request(EXTENSION, extensionAnswers + ("sanction" to sanction)))).single()
        assertEquals("Sanction SIG/77 dated 20/06/2026", extension.authority)
    }

    private val extensionAnswers: Map<String, SlotValue> =
        mapOf("ext_from" to day(9, 5), "new_to" to day(9, 30), "grant" to SlotValue.RefValue(OrderRef("41/2026", CalendarDate(2026, 6, 30))))

    @Test
    fun aMissingPublishedOrderPrintsABlankAndIsNotAnError() {
        val without = extensionAnswers - "grant"
        val only = rows(slip(request(EXTENSION, without))).single()
        val blank = OccurrenceRenderer.BLANK
        assertEquals("Extended the first thing vide $blank dated $blank up to 30/09/2026", only.narrative)
        assertEquals(listOf(blank, blank, "", ""), only.data)
        assertEquals("Sanction $blank dated $blank", only.authority)
    }

    @Test
    fun aCodeThePageSaysPrintsDifferentlyIsPrintedThatWay() {
        val rejoin = mapOf("rejoin_on" to day(9, 5), "grant" to SlotValue.RefValue(OrderRef("41/2026", CalendarDate(2026, 6, 30))))
        val only = rows(slip(request(REJOIN_ALL, rejoin))).single()
        assertEquals("REJOIN", only.code, "HRMS publishes REJOIN; the manual lists REJOINAL (Round 9)")
        assertEquals("CM", only.group)
    }

    @Test
    fun theDayCountIncludesBothDaysAndAChecksFindingCarriesItsSeverityAndCitation() {
        val result = slip(request())
        val finding = result.findings.single()
        assertEquals(RuleFinding(SyntheticPart2Templates.CAP_DAYS, RuleSeverity.WARN, GRANT, LinkBasis.MANUAL, 115, "2"), finding)
        val sixty = leave + ("leave_to" to day(9, 2))
        assertEquals(emptyList(), slip(request(GRANT, sixty)).findings, "05 Jul - 02 Sep is exactly 60 days, which the cap allows")
    }

    @Test
    fun anAckCheckIsAnAckAndOnlyFiresWhenItsConditionIsBroken() {
        val noGrant = slip(request(EXTENSION, extensionAnswers - "grant")).findings.single()
        assertEquals(RuleSeverity.ACK, noGrant.severity, "the manual says it will be rejected, so the officer must acknowledge it")
        assertEquals(emptyList(), slip(request(EXTENSION, extensionAnswers)).findings)
    }

    @Test
    fun aClaimIsSplitIntoItsOwnOrderWhateverTheDates() {
        val claim = request(CLAIM, mapOf("claim_on" to day(6, 1)))
        val orders = slip(request(), claim).orders
        assertEquals(listOf(OrderFamily.FINANCIAL, OrderFamily.CLAIM), orders.map { it.family })
        assertEquals(listOf(GRANT), orders[0].occurrences.map { it.code })
        assertEquals(listOf(CLAIM), orders[1].occurrences.map { it.code })
    }

    @Test
    fun twoRequestsForOneCodeAreTwoOccurrencesAndTheirSharedLinkIsOneRow() {
        // why: two leave years granted together are two occurrences (note 10), but both link the same ration order, typed once.
        val second = leave + mapOf("leave_from" to day(1, 5, 2027), "leave_to" to day(2, 3, 2027), "year" to SlotValue.NumberValue(2027))
        val result = slip(request(GRANT, leave, SyntheticPart2Templates.ANSWER_RATION), request(GRANT, second, SyntheticPart2Templates.ANSWER_RATION))
        assertEquals(listOf(RATION, GRANT, GRANT), rows(result).map { it.code }, "both requests link BETARATION from the same 01 Jul date")
        assertEquals(listOf("2026", "2027"), rows(result).filter { it.code == GRANT }.map { it.data[1] })
    }

    @Test
    fun linksThatPointBackAreDraftedOnceAndNeverLoop() {
        val pointingBack = SyntheticPart2Templates.templates.map { if (it.code == RATION) it.copy(links = listOf(Part2Link(GRANT, "practice"))) else it }
        val looping = SyntheticPart2Templates.lookup.copy(templates = pointingBack)
        val ration = request(GRANT, leave, SyntheticPart2Templates.ANSWER_RATION)
        val result = Part2DraftEngine(looping, SyntheticPart2Templates.registry()).draft(LeaveEventInput(listOf(ration)))
        assertEquals(listOf(RATION, GRANT), rows(assertIs<DraftResult.Drafted>(result).slip).map { it.code })
    }

    @Test
    fun aRuleTheAppDoesNotKnowIsSkippedNeverCrashed() {
        // why: the contract test stops this shipping; at run time an unknown check must not take the draft down with it.
        val bare = Part2DraftEngine(SyntheticPart2Templates.lookup, Part2Rules.shipped())
        val result = assertIs<DraftResult.Drafted>(bare.draft(LeaveEventInput(listOf(request(GRANT, leave, SyntheticPart2Templates.ANSWER_RATION)))))
        assertEquals(emptyList(), result.slip.findings)
        assertEquals(listOf(GRANT), rows(result.slip).map { it.code }, "an unknown condition does not hold, so its link is not drafted")
    }

    @Test
    fun theLastOrderTheOfficerTypedIsCarriedAndABlankStaysBlank() {
        val last = OrderRef("88/2026", CalendarDate(2026, 6, 1))
        assertEquals(last, assertIs<DraftResult.Drafted>(engine.draft(LeaveEventInput(listOf(request()), last))).slip.lastOrder)
        assertEquals(null, slip(request()).lastOrder)
    }

    @Test
    fun theSameInputGivesTheSameSlipHoweverTheAnswersWereEntered() {
        val reversed = leave.entries.reversed().associate { it.key to it.value }
        val first = slip(request(GRANT, leave, SyntheticPart2Templates.ANSWER_RATION, SyntheticPart2Templates.ANSWER_HILL))
        repeat(20) {
            assertEquals(first, slip(request(GRANT, reversed, SyntheticPart2Templates.ANSWER_HILL, SyntheticPart2Templates.ANSWER_RATION)))
        }
        assertTrue(first.orders.isNotEmpty())
    }

    @Test
    fun anUnknownCodeOrNothingAtAllIsRefused() {
        assertEquals(listOf(DraftIssue("NOSUCHCODE", "", DraftIssueReason.UNKNOWN_CODE)), refused(request("NOSUCHCODE")))
        assertEquals(listOf(DraftIssue("", "", DraftIssueReason.NOTHING_REQUESTED)), assertIs<DraftResult.Refused>(engine.draft(LeaveEventInput(emptyList()))).issues)
    }
}
