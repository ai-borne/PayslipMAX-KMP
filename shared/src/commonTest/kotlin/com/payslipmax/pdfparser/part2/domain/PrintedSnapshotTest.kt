package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * WHY: what the clerk was handed is the officer's only record of what HRMS should hold, and it is also what starts the "add the published
 * order" reminder. Editing the draft afterwards must not erase it (Round 18), but the preview must say when the paper and the draft no
 * longer agree, and say it differently when the cause is the manual's template rather than the officer.
 */
class PrintedSnapshotTest {
    private fun occurrence(
        code: String = "CODEA",
        narrative: String = "Granted 5 days",
        from: CalendarDate? = CalendarDate(2026, 7, 5),
        to: CalendarDate? = CalendarDate(2026, 7, 9),
        data: List<String> = listOf("x", "", "", ""),
        authority: String? = "Signal 12",
        certificate: String? = "CERTIFICATE: ok",
        group: String? = "A",
    ) = Part2Occurrence(code, group, narrative, from, to, data, authority, certificate, Part2Origin.Requested, "rev00001")

    private fun slip(vararg orders: List<Part2Occurrence>) = Part2Slip(null, orders.map { Part2Order(OrderFamily.FINANCIAL, it) }, emptyList(), emptyList())

    private val revs = mapOf("CODEA" to "a1", "CODEB" to "b1")

    private fun snapshot(vararg rows: Part2Occurrence) = PrintedSnapshot.of(slip(rows.toList()), revs, at = 77L)

    @Test
    fun theSnapshotKeepsEveryColumnOfEveryRowAsHrmsPrintsIt() {
        val taken = snapshot(occurrence(), occurrence(code = "CODEB", from = null, to = null, data = emptyList(), authority = null, certificate = null))

        assertEquals(
            listOf(
                PrintedRow("CODEA", "Granted 5 days", "05/07/2026", "09/07/2026", listOf("x", "", "", ""), "Signal 12", "CERTIFICATE: ok"),
                PrintedRow("CODEB", "Granted 5 days", "", "", emptyList(), null, null),
            ),
            taken.rows,
        )
        assertEquals(77L, taken.at)
        assertEquals(revs, taken.revs)
    }

    @Test
    fun rowsFromEveryOrderAreKeptInSlipOrder() {
        val two = slip(listOf(occurrence(code = "ONE")), listOf(occurrence(code = "TWO"), occurrence(code = "THREE")))

        assertEquals(listOf("ONE", "TWO", "THREE"), PrintedSnapshot.of(two, revs, 1L).rows.map { it.code })
    }

    @Test
    fun aDraftThatStillGivesTheSameRowsIsCurrent() {
        val taken = snapshot(occurrence())

        assertEquals(PrintedStatus.CURRENT, taken.statusFor(slip(listOf(occurrence())), revs))
    }

    @Test
    fun changingAnyColumnOfARowMakesItChanged() {
        val taken = snapshot(occurrence())
        val edits =
            listOf(
                occurrence(code = "CODEZ"),
                occurrence(narrative = "Granted 6 days"),
                occurrence(from = CalendarDate(2026, 7, 6)),
                occurrence(to = CalendarDate(2026, 7, 10)),
                occurrence(from = null),
                occurrence(data = listOf("y", "", "", "")),
                occurrence(authority = null),
                occurrence(certificate = "CERTIFICATE: changed"),
            )
        for (edit in edits) assertEquals(PrintedStatus.CHANGED, taken.statusFor(slip(listOf(edit)), revs))
    }

    @Test
    fun aRowAddedRemovedOrReorderedIsChanged() {
        val a = occurrence(code = "CODEA")
        val b = occurrence(code = "CODEB")
        val taken = snapshot(a, b)

        assertEquals(PrintedStatus.CHANGED, taken.statusFor(slip(listOf(a)), revs))
        assertEquals(PrintedStatus.CHANGED, taken.statusFor(slip(listOf(a, b, a)), revs))
        assertEquals(PrintedStatus.CHANGED, taken.statusFor(slip(listOf(b, a)), revs))
    }

    @Test
    fun aDraftThatIsIncompleteNowIsChangedBecauseThereIsNoSlipToMatch() {
        assertEquals(PrintedStatus.CHANGED, snapshot(occurrence()).statusFor(null, revs))
    }

    @Test
    fun aTemplateThatMovedSinceThePrintIsTemplateUpdatedEvenWhenTheRowsReadTheSame() {
        val taken = snapshot(occurrence())

        assertEquals(PrintedStatus.TEMPLATE_UPDATED, taken.statusFor(slip(listOf(occurrence())), revs + ("CODEA" to "a2")))
    }

    @Test
    fun aTemplateUpdateIsReportedBeforeTheOfficersOwnEdit() {
        val taken = snapshot(occurrence())

        assertEquals(PrintedStatus.TEMPLATE_UPDATED, taken.statusFor(slip(listOf(occurrence(narrative = "other"))), revs + ("CODEA" to "a2")))
        assertEquals(PrintedStatus.TEMPLATE_UPDATED, taken.statusFor(null, revs + ("CODEB" to "b2")))
    }

    @Test
    fun aCodeAddedOrDroppedAfterPrintingIsTheOfficersEditNotATemplateUpdate() {
        val taken = snapshot(occurrence(), occurrence(code = "CODEB"))

        // Dropped: CODEB is no longer drafted, so its revision is gone and its row with it.
        assertEquals(PrintedStatus.CHANGED, taken.statusFor(slip(listOf(occurrence())), mapOf("CODEA" to "a1")))
        // Added: CODEC was not on the paper, so it has no printed revision to have moved from.
        val added = slip(listOf(occurrence(), occurrence(code = "CODEB"), occurrence(code = "CODEC")))
        assertEquals(PrintedStatus.CHANGED, taken.statusFor(added, mapOf("CODEA" to "a1", "CODEB" to "b1", "CODEC" to "c9")))
    }

    @Test
    fun aSnapshotNeverShowsItsContentWhenPrintedAsText() {
        val text = snapshot(occurrence(narrative = "MARKER_SECRET_NARRATIVE")).toString() + snapshot(occurrence()).rows.first().toString()

        assertFalse("MARKER_SECRET" in text || "CODEA" in text || "05/07/2026" in text, text)
    }
}
