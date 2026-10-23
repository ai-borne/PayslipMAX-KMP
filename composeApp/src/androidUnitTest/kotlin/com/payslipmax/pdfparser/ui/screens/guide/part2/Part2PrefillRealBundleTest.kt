package com.payslipmax.pdfparser.ui.screens.guide.part2

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.guide.GuideBundleContract
import com.payslipmax.pdfparser.part2.domain.AreaRules
import com.payslipmax.pdfparser.part2.domain.LeaveEventEditor
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.OrderRef
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.Part2Rules
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.testing.FakePart2Repository
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.FieldValue
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.LeaveDraftViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/** The prefill on the shipped templates: choosing the R&H code an earlier draft chose brings back its station, corps, notification and serial, and no date. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class Part2PrefillRealBundleTest {
    private val section = checkNotNull(kotlinx.coroutines.runBlocking { GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText()) }.part2)
    private val code = AreaRules.rhCodes[1]

    @Test
    fun theAreaDetailsOfTheLastDraftWithTheSameRhCodeAreBroughtBack() =
        runTest {
            val repo = FakePart2Repository()
            val values =
                mapOf(
                    "station" to SlotValue.TextValue("ZZ01"),
                    "corps" to SlotValue.TextValue("Alpha"),
                    "notif" to SlotValue.RefValue(OrderRef("N-17/2026", CalendarDate(2026, 3, 2))),
                    "serial" to SlotValue.TextValue("12"),
                    "leave_from" to SlotValue.DateValue(CalendarDate(2026, 7, 6)),
                )
            repo.saveDraft(Part2Draft("old", LeaveEventInput(listOf(OccurrenceRequest("GTDAL", values, setOf(AreaRules.ANSWER_RH_PREFIX + code)))), emptyMap(), 1L, 5L))
            val vm = LeaveDraftViewModel("new", section, repo, LeaveEventEditor.start("GTDAL"), Part2Rules.shipped(), { 1L }, StandardTestDispatcher(testScheduler))
            vm.choose(0, "rh_code", code)
            advanceUntilIdle()
            val fields = vm.state.value.sections[0].fields.associate { it.slotId to it.value }
            assertEquals(FieldValue.Words("ZZ01"), fields["station"])
            assertEquals(FieldValue.Words("Alpha"), fields["corps"])
            assertEquals(FieldValue.Reference("N-17/2026", CalendarDate(2026, 3, 2)), fields["notif"])
            assertEquals(FieldValue.Words("12"), fields["serial"])
            assertEquals(FieldValue.Date(null), fields["leave_from"], "the dates of the last leave are the officer's to enter again")
        }
}
