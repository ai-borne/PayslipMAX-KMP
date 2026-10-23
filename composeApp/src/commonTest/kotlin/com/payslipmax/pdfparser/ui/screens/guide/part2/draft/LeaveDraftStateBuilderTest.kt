package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.DraftIssueReason
import com.payslipmax.pdfparser.part2.domain.LeaveEventEditor
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.LinkBasis
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.OrderRef
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.domain.RuleFinding
import com.payslipmax.pdfparser.part2.domain.RuleSeverity
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.ANSWER_HILL
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.ANSWER_RATION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.CLAIM
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.EXTENSION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.RATION
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What the draft screens draw for an event. Every sentence here comes from `Part2Strings` or the bundle, never from a composable, and
 * the same answers always give the same screen. The templates and rules are invented, so these tests pin how the form is built.
 */
class LeaveDraftStateBuilderTest {
    private val builder = LeaveDraftStateBuilder(SyntheticPart2Templates.lookup, SyntheticPart2Templates.registry())

    private fun day(
        m: Int,
        d: Int,
    ) = SlotValue.DateValue(CalendarDate(2026, m, d))

    private fun event(
        vararg requests: OccurrenceRequest,
        last: OrderRef? = null,
    ) = LeaveEventInput(requests.toList(), last)

    private val complete = mapOf("leave_from" to day(7, 5), "leave_to" to day(9, 4), "year" to SlotValue.NumberValue(2026))

    private fun build(
        event: LeaveEventInput,
        acked: Set<String> = emptySet(),
        failed: Boolean = false,
    ) = builder.build(event, acked, failed)

    private fun field(
        state: LeaveDraftUiState,
        slot: String,
        section: Int = 0,
    ) = state.sections[section].fields.first { it.slotId == slot }

    @Test
    fun aFreshRequestShowsItsFieldsEmptyAndRequiredWithoutScoldingForWhatIsNotAnsweredYet() {
        val state = build(event(OccurrenceRequest(GRANT)))
        assertEquals(DraftStatus.READY, state.status)
        val section = state.sections.single()
        assertEquals(Part2Strings.sectionTitle(1, GRANT), section.title)
        assertEquals("Invented thing $GRANT", section.name)
        assertEquals(listOf("leave_from", "leave_to", "year"), section.fields.map { it.slotId })
        assertEquals(listOf("First day", "Last day", "Year"), section.fields.map { it.label })
        assertTrue(section.fields.all { it.required && it.error == null && it.help.isNotBlank() })
        assertEquals(FieldValue.Date(null), field(state, "leave_from").value)
        assertEquals(FieldValue.Digits(""), field(state, "year").value)
        assertEquals(listOf("First day", "Last day", "Year"), state.missing)
        assertNull(state.slip)
        assertFalse(section.removable)
    }

    @Test
    fun onlyRequiredAnswersAreListedAsStillNeeded() {
        // why: an order reference prints blank without its number, so telling the officer it is "still needed" would send them hunting for an order that is not published yet.
        val state = build(event(OccurrenceRequest(EXTENSION)))
        assertEquals(listOf("From", "Up to"), state.missing)
        assertEquals(listOf(false, true, true, false), state.sections[0].fields.map { it.required }, "grant order, from, up to, sanction: the two references are optional")
    }

    @Test
    fun answersAppearInTheirFieldsAsTheyAreHeld() {
        val state = build(event(OccurrenceRequest(GRANT, complete)))
        assertEquals(FieldValue.Date(CalendarDate(2026, 7, 5)), field(state, "leave_from").value)
        assertEquals(FieldValue.Digits("2026"), field(state, "year").value)
        assertEquals(emptyList(), state.missing)
    }

    @Test
    fun aBadAnswerIsNamedOnItsFieldInWordsFromStringsAndAMissingOneIsOnlyListed() {
        val reversed = complete + ("leave_to" to day(7, 1)) - "year"
        val state = build(event(OccurrenceRequest(GRANT, reversed)))
        assertEquals(Part2Strings.fieldIssue("Last day", DraftIssueReason.DATES_REVERSED), field(state, "leave_to").error)
        assertNull(field(state, "year").error)
        assertEquals(listOf("Year"), state.missing)
        assertNull(state.slip)
    }

    @Test
    fun theQuestionsAreAskedInTheWordsOfStringsAndTheirAnswersToggleTheFields() {
        val plain = build(event(OccurrenceRequest(GRANT)))
        assertEquals(listOf("ration", "hill"), plain.sections[0].questions.map { it.id })
        assertEquals(Part2Strings.question("ration"), plain.sections[0].questions[0].text)
        assertTrue(plain.sections[0].questions.none { it.yes })
        val rationed = build(event(OccurrenceRequest(GRANT, answers = setOf(ANSWER_RATION))))
        assertTrue(rationed.sections[0].questions.first { it.id == "ration" }.yes)
        assertEquals(listOf("leave_from", "leave_to", "year", "away_from"), rationed.sections[0].fields.map { it.slotId })
    }

    @Test
    fun aCompleteRequestGivesTheSlipWithItsRowsDatesAndBlankLastOrder() {
        val slip = assertNotNull(build(event(OccurrenceRequest(GRANT, complete))).slip)
        assertEquals(Part2Strings.lastOrderBlank, slip.lastOrder)
        val order = slip.orders.single()
        assertEquals(Part2Strings.orderTitle(1, listOf("A")), order.title)
        val row = order.rows.single()
        assertEquals(GRANT, row.code)
        assertEquals("05/07/2026", row.from)
        assertEquals("04/09/2026", row.to)
        assertEquals("", row.source, "a row the officer asked for names no source")
        assertEquals("Granted the first thing for 62 days in 2026 from 05/07/2026 to 04/09/2026", row.narrative)
    }

    @Test
    fun theLastOrderIsQuotedWhenThereIsOne() {
        val last = OrderRef("SYN/1/26", CalendarDate(2026, 1, 2))
        assertEquals(Part2Strings.lastOrderLine("SYN/1/26", "02/01/2026"), build(event(OccurrenceRequest(GRANT, complete), last = last)).slip?.lastOrder)
    }

    @Test
    fun aLinkedRowSaysWhereItComesFromAndNothingMoreThanThat() {
        val values = complete + ("away_from" to day(7, 1)) + ("rejoin" to day(9, 5))
        val slip = assertNotNull(build(event(OccurrenceRequest(GRANT, values, setOf(ANSWER_RATION, ANSWER_HILL)))).slip)
        val rows = slip.orders.flatMap { it.rows }.associateBy { it.code }
        assertEquals(Part2Strings.citation(LinkBasis.MANUAL, 97, "1.1"), rows.getValue(RATION).source)
        assertEquals(Part2Strings.seenInPractice, rows.getValue(SyntheticPart2Templates.STOP).source)
        assertEquals(listOf("${SyntheticPart2Templates.NO_TEMPLATE} (${Part2Strings.seenInPractice})"), slip.undrafted)
    }

    @Test
    fun aClaimIsItsOwnOrderWithItsOwnGroupInTheTitle() {
        val claim = OccurrenceRequest(CLAIM, mapOf("claim_on" to day(7, 9)))
        val slip = assertNotNull(build(event(OccurrenceRequest(GRANT, complete), claim)).slip)
        assertEquals(listOf(Part2Strings.orderTitle(1, listOf("A")), Part2Strings.orderTitle(2, listOf("B"))), slip.orders.map { it.title })
    }

    @Test
    fun aNumberedOrderReferenceNeedsItsDateAndAnEmptyOneIsNotAnError() {
        val base = mapOf("ext_from" to day(9, 5), "new_to" to day(9, 20))
        assertEquals(FieldValue.Reference("", null), field(build(event(OccurrenceRequest(EXTENSION, base))), "grant").value)
        val numberOnly = base + ("grant" to SlotValue.RefValue(OrderRef("N/5", LeaveEventEditor.UNSET_DATE)))
        val half = build(event(OccurrenceRequest(EXTENSION, numberOnly)))
        assertEquals(FieldValue.Reference("N/5", null), field(half, "grant").value)
        assertEquals(Part2Strings.fieldIssue("Grant order", DraftIssueReason.BAD_DATE), field(half, "grant").error)
        assertNull(half.slip)
        val whole = base + ("grant" to SlotValue.RefValue(OrderRef("N/5", CalendarDate(2026, 6, 1))))
        assertNull(field(build(event(OccurrenceRequest(EXTENSION, whole))), "grant").error)
    }

    @Test
    fun anAckFindingIsKeyedByRuleAndCodeAndBlocksReadinessUntilTicked() {
        val values = mapOf("ext_from" to day(9, 5), "new_to" to day(9, 20))
        val event = event(OccurrenceRequest(EXTENSION, values))
        val open = assertNotNull(build(event).slip)
        val finding = open.findings.single()
        assertEquals("${SyntheticPart2Templates.GRANT_QUOTED}|$EXTENSION", finding.key)
        assertTrue(finding.ack && !finding.ticked)
        assertFalse(open.readyToPrint)
        val ticked = assertNotNull(build(event, acked = setOf(finding.key)).slip)
        assertTrue(ticked.findings.single().ticked && ticked.readyToPrint)
    }

    @Test
    fun aWarnNeverBlocksReadinessAndIsNotTickable() {
        val tooLong = complete + ("leave_to" to SlotValue.DateValue(CalendarDate(2026, 12, 31)))
        val slip = assertNotNull(build(event(OccurrenceRequest(GRANT, tooLong))).slip)
        val warn = slip.findings.single()
        assertFalse(warn.ack)
        assertTrue(slip.readyToPrint)
    }

    @Test
    fun aFindingsWordsComeFromItsRuleIdAndItsCitationFromItsDeclaration() {
        val ack = RuleFinding("al_year_cap", RuleSeverity.ACK, "GTDAL", LinkBasis.MANUAL, 115, "2")
        val ui = LeaveDraftStateBuilder.finding(ack, ticked = false)
        assertEquals("al_year_cap|GTDAL", ui.key)
        assertEquals(Part2Strings.finding("al_year_cap"), ui.text)
        assertEquals(Part2Strings.citation(LinkBasis.MANUAL, 115, "2"), ui.citation)
        assertTrue(ui.ack)
        assertEquals(Part2Strings.findingUnknown, LeaveDraftStateBuilder.finding(ack.copy(ruleId = "no_such_rule"), ticked = false).text)
        assertFalse(LeaveDraftStateBuilder.finding(ack.copy(severity = RuleSeverity.WARN), ticked = true).ack)
        assertTrue(LeaveDraftStateBuilder.finding(ack, ticked = true).ticked)
    }

    @Test
    fun anotherOccurrenceCanBeAddedUntilTheLimitAndAnySectionCanGoOnceThereAreTwo() {
        val one = build(event(OccurrenceRequest(GRANT)))
        assertEquals(SyntheticPart2Templates.lookup.let { com.payslipmax.pdfparser.part2.domain.Part2LookupIndex(it).draftOptions() }.map { it.code }, one.addable.map { it.code })
        val two = build(event(OccurrenceRequest(GRANT), OccurrenceRequest(GRANT)))
        assertEquals(listOf(Part2Strings.sectionTitle(1, GRANT), Part2Strings.sectionTitle(2, GRANT)), two.sections.map { it.title })
        assertTrue(two.sections.all { it.removable })
        val full = build(event(*Array(Part2Limits.REQUESTS_MAX) { OccurrenceRequest(GRANT) }))
        assertEquals(emptyList(), full.addable)
    }

    @Test
    fun theTemplateRevisionsAreThoseOfEveryTemplateTheEventDrafts() {
        assertEquals(mapOf(GRANT to "a0000001"), builder.templateRevs(event(OccurrenceRequest(GRANT))))
        assertEquals(mapOf(GRANT to "a0000001", RATION to "a0000003"), builder.templateRevs(event(OccurrenceRequest(GRANT, answers = setOf(ANSWER_RATION)))))
        assertEquals(emptyMap(), builder.templateRevs(event(OccurrenceRequest("NOSUCHCODE"))))
        assertEquals(mapOf(GRANT to "a0000001", EXTENSION to "a0000002"), builder.templateRevs(event(OccurrenceRequest(GRANT), OccurrenceRequest(EXTENSION))))
    }

    @Test
    fun aCodeThisVersionCannotDraftStillShowsASectionSoNothingIsDropped() {
        val state = build(event(OccurrenceRequest("NOSUCHCODE")))
        assertEquals("NOSUCHCODE", state.sections.single().code)
        assertEquals(emptyList(), state.sections.single().fields)
        assertNull(state.slip)
    }

    @Test
    fun theStateHidesEveryAnswerInItsTextForm() {
        assertFalse("2026" in build(event(OccurrenceRequest(GRANT, complete))).toString())
    }
}
