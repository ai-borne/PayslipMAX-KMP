package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.part2.domain.LeaveEventEditor
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.testing.FakeLeaveBankSource
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.EXTENSION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The one defence a view-model test cannot reach, because it only matters when two threads race: an atomic edit that is retried finds the
 * place the fill was pressed on holding another order. The filler is given an `edit` that applies its change to such a state, as the retry would.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class LeaveBankFillerTest : LeaveDraftFillBase() {
    @Test
    fun aFillThatFindsAnotherOrderInThePlaceItWasPressedOnWritesNothingAndSaysNothing() =
        runTest {
            val pressedOn = LeaveEventEditor.start(GRANT)
            val replacedBy = LeaveEventEditor.start(EXTENSION)
            var applied: LeaveEventInput? = null
            val messages = mutableListOf<Map<Int, FillMessage>>()
            lateinit var filler: LeaveBankFiller
            filler =
                LeaveBankFiller(
                    scope = CoroutineScope(Job() + StandardTestDispatcher(testScheduler)),
                    source = FakeLeaveBankSource(rows),
                    repository = repo,
                    rules = rules,
                    draftId = "draft-1",
                    event = { pressedOn },
                    edit = { change -> applied = change(replacedBy) },
                    onChange = { messages += filler.messages },
                )
            filler.start()
            advanceUntilIdle()
            filler.fill(0, replace = false)
            advanceUntilIdle()
            assertEquals(replacedBy, applied, "the edit ran and left the other order untouched")
            assertEquals(emptyList(), fill.asked, "the fill was never asked about an order it does not serve")
            assertNull(messages.lastOrNull()?.get(0), "and says nothing about it")
        }
}
