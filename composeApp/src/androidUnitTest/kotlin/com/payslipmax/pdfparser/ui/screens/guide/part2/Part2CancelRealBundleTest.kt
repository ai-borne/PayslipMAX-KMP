package com.payslipmax.pdfparser.ui.screens.guide.part2

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.guide.GuideBundleContract
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.LinkBasis
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.OrderRef
import com.payslipmax.pdfparser.part2.domain.Part2Rules
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.testing.FakePart2Repository
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.FindingUi
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.LeaveDraftViewModel
import com.payslipmax.pdfparser.ui.theme.Part2CancelStrings
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: the time-bar warning (notes 4 and 6, pp.226-227) compares the cancelled order's date with the day where the officer is, so it must
 * read the device's own day, not UTC, and never block printing (the manual says the sanction "may be enclosed"). Run on the shipped
 * templates and rules, with synthetic orders only.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class Part2CancelRealBundleTest {
    private val section = checkNotNull(kotlinx.coroutines.runBlocking { GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText()) }.part2)

    /** 25 Oct 2026, 20:00 UTC: already the 26th at +5:30. */
    private val evening = LocalDate.of(2026, 10, 25).atTime(20, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun TestScope.cancelModel(
        order: OrderRef?,
        offset: Long = 0L,
        code: String = "CANCEL",
        repo: Part2Repository = FakePart2Repository(),
    ): LeaveDraftViewModel {
        val values = buildMap<String, SlotValue> { order?.let { put("cancelled", SlotValue.RefValue(it)) } }
        val event = LeaveEventInput(listOf(OccurrenceRequest(code, values + if (code == "CANCELS") mapOf("cancelled_code" to SlotValue.TextValue("GTDAL"), "cancelled_serial" to SlotValue.NumberValue(3)) else emptyMap())))
        return LeaveDraftViewModel("d", section, repo, event, Part2Rules.shipped(), { evening }, StandardTestDispatcher(testScheduler)) { offset }.also { advanceUntilIdle() }
    }

    private fun LeaveDraftViewModel.findings(): List<FindingUi> = state.value.slip!!.findings

    private val barredText = Part2CancelStrings.finding("cancel_time_barred")!!

    @Test
    fun anOrderExactlyTwoYearsOldIsNotBarredInUtcButIsOverTwoYearsOldWhereTheDayHasTurned() =
        runTest {
            val order = OrderRef("A/1", CalendarDate(2024, 10, 25))

            assertTrue(cancelModel(order, offset = 0L).findings().none { it.text == barredText }, "25 Oct 2026 UTC: exactly 2 years")
            assertTrue(cancelModel(order, offset = 19_800_000L).findings().any { it.text == barredText }, "26 Oct 2026 at +5:30: a day over")
        }

    @Test
    fun theTimeBarWarningIsOnlyAWarningWithTheManualsCitationAndNeverBlocksPrinting() =
        runTest {
            val finding = cancelModel(OrderRef("A/1", CalendarDate(2020, 1, 1))).findings().single()

            assertFalse(finding.ack, "the sanction 'may be enclosed': a warning, not a tick")
            assertEquals(Part2Strings.citation(LinkBasis.MANUAL, 226, "4"), finding.citation)
            assertTrue(cancelModel(OrderRef("A/1", CalendarDate(2020, 1, 1))).state.value.slip!!.readyToPrint)
        }

    @Test
    fun cancelsCitesItsOwnNote() =
        runTest {
            val finding = cancelModel(OrderRef("A/1", CalendarDate(2020, 1, 1)), code = "CANCELS").findings().single()

            assertEquals(Part2Strings.citation(LinkBasis.MANUAL, 227, "6"), finding.citation)
            assertEquals(barredText, finding.text)
        }

    @Test
    fun anOrderNotYetQuotedIsWarnedAboutAndStillPrintsBlank() =
        runTest {
            val finding = cancelModel(order = null).findings().single()

            assertEquals(Part2CancelStrings.finding("cancel_quoted"), finding.text)
            assertEquals(Part2Strings.citation(LinkBasis.MANUAL, 226, "2"), finding.citation)
        }

    @Test
    fun aRecentOrderRaisesNothing() =
        runTest {
            assertEquals(emptyList(), cancelModel(OrderRef("A/1", CalendarDate(2026, 6, 20))).findings())
        }
}
