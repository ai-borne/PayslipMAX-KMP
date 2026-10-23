package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.guide.model.Part2Lookup
import java.io.File
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/** The Part II section of the real shipped Guide bundle, the real rule registry and synthetic answers, for tests that must not use invented templates. */
internal object RealPart2 {
    val section: Part2Lookup by lazy {
        val text = File("../composeApp/src/commonMain/composeResources/files/guide/guide_bundle.json").readText()
        assertNotNull(assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(text)).bundle.part2)
    }

    val engine: Part2DraftEngine by lazy { Part2DraftEngine(section, Part2Rules.shipped()) }

    fun date(
        month: Int,
        day: Int,
        year: Int = 2026,
    ) = SlotValue.DateValue(CalendarDate(year, month, day))

    fun number(value: Int) = SlotValue.NumberValue(value)

    fun text(value: String) = SlotValue.TextValue(value)

    fun ref(
        no: String,
        month: Int,
        day: Int,
        year: Int = 2026,
    ) = SlotValue.RefValue(OrderRef(no, CalendarDate(year, month, day)))

    /** A synthetic 30-day annual leave: 06 Jul to 04 Aug 2026, prefix from Sat 04 Jul, in an invented R&H station with an invented Corps notification. */
    val annualLeave: Map<String, SlotValue> =
        mapOf(
            "leave_from" to date(7, 6),
            "leave_to" to date(8, 4),
            "prefix_first" to date(7, 4),
            "year" to number(2026),
            "station" to text("ZZ01"),
            "corps" to text("Alpha"),
            "notif" to ref("N-17/2026", 3, 2),
            "serial" to text("12"),
            "leave_kind" to text("annual leave"),
        )

    fun draft(
        vararg requests: OccurrenceRequest,
        last: OrderRef? = null,
    ): Part2Slip = assertIs<DraftResult.Drafted>(engine.draft(LeaveEventInput(requests.toList(), last))).slip

    fun rows(slip: Part2Slip): List<Part2Occurrence> = slip.orders.flatMap { it.occurrences }

    fun answers(vararg answer: String): Set<String> = answer.toSet()
}
