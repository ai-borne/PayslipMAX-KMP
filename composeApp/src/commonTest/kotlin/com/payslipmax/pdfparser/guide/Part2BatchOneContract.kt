package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.part2.domain.AreaRules
import com.payslipmax.pdfparser.part2.domain.DraftResult
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.OrderRef
import com.payslipmax.pdfparser.part2.domain.Part2DraftEngine
import com.payslipmax.pdfparser.part2.domain.Part2Rules
import com.payslipmax.pdfparser.part2.domain.RationRules
import com.payslipmax.pdfparser.part2.domain.SlotValue
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * Part II content batch 1 (annual leave set, ration four, R&H) drafted on the real shipped bundle and the real rule registry, on each
 * platform (`GuideBundleAndroidContractTest`, and timed on Kotlin/Native by `GuideLoaderIosPerfTest`). The JVM golden slips
 * (`Part2BatchOneGoldenTest`) prove the wording; this proves the same bundle packaged for the app drafts the same slip, and gives
 * the iOS perf test a real 7-row event to time. Dates, station and notification are invented.
 */
object Part2BatchOneContract {
    private fun day(
        month: Int,
        day: Int,
    ) = SlotValue.DateValue(CalendarDate(2026, month, day))

    internal val values: Map<String, SlotValue> =
        mapOf(
            "leave_from" to day(7, 6),
            "leave_to" to day(8, 4),
            "prefix_first" to day(7, 4),
            "year" to SlotValue.NumberValue(2026),
            "station" to SlotValue.TextValue("ZZ01"),
            "corps" to SlotValue.TextValue("Alpha"),
            "notif" to SlotValue.RefValue(OrderRef("N-17/2026", CalendarDate(2026, 3, 2))),
            "serial" to SlotValue.TextValue("12"),
            "leave_kind" to SlotValue.TextValue("annual leave"),
        )

    internal val answers = setOf(RationRules.ANSWER_ON_RATION, AreaRules.ANSWER_RH_PREFIX + "GTDRH12")

    /** The engine over the shipped section and shipped rules. Build it once and draft many times when timing. */
    fun engine(bundle: GuideBundle) = Part2DraftEngine(assertNotNull(bundle.part2), Part2Rules.shipped())

    /** Drafts the 30-day annual leave with ration and R&H and returns the printed codes in order. */
    fun draftAnnualLeave(engine: Part2DraftEngine): List<String> {
        val result = engine.draft(LeaveEventInput(listOf(OccurrenceRequest("GTDAL", values, answers))))
        val slip = assertIs<DraftResult.Drafted>(result).slip
        assertEquals(emptyList(), slip.findings, "30 days with a 2-day prefix breaks no note")
        return slip.orders.single().occurrences.map { it.code }
    }

    fun assertBatchOneDraftsOnTheShippedBundle(bundle: GuideBundle) {
        assertEquals(
            listOf("SORS", "GTDRSHNA", "STPRH", "GTDAL", "TORS", "STPRSHNA", "GTDRH12"),
            draftAnnualLeave(engine(bundle)),
            "ration four and R&H around a 30-day annual leave: one order, in date order",
        )
    }
}
