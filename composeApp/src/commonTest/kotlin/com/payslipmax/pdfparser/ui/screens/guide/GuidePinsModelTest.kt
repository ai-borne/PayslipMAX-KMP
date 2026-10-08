package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuidePinsStorage
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GuidePinsModelTest {
    @Test
    fun aPinIsSavedTheMomentItIsMadeAndSurvivesANewModel() {
        val storage = FakeGuidePinsStorage()

        GuidePinsModel(storage).apply {
            toggle("RB-T1")
            toggle("RB-T2")
        }

        assertEquals(listOf("RB-T2", "RB-T1"), GuidePinsModel(storage).pins.value.newestFirst)
    }

    @Test
    fun unpinningIsSavedToo() {
        val storage = FakeGuidePinsStorage()
        val model = GuidePinsModel(storage)
        model.toggle("RB-T1")

        model.toggle("RB-T1")

        assertTrue(GuidePinsModel(storage).pins.value.newestFirst.isEmpty())
    }

    @Test
    fun idsTheBundleNoLongerHoldsAreDroppedFromTheStateAndFromStorage() {
        val storage = FakeGuidePinsStorage(stored = "RB-GONE\nRB-T1")
        val model = GuidePinsModel(storage)

        model.retainKnown { it == "RB-T1" }

        assertEquals(listOf("RB-T1"), model.pins.value.newestFirst)
        assertEquals("RB-T1", storage.stored)
    }

    @Test
    fun nothingIsWrittenWhenNothingChanged() {
        val storage = FakeGuidePinsStorage(stored = "RB-T1")
        val model = GuidePinsModel(storage)

        model.retainKnown { true }

        assertEquals(0, storage.saves)
    }

    @Test
    fun pinnedRowsSkipAnIdTheBundleDoesNotHoldAndKeepTheGivenOrder() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val guide = GuideViewModel(FakeGuideRepository(), FakeCrashReporter(), dispatcher)
            guide.load()
            advanceUntilIdle()

            val rows = guide.pinnedRows(listOf("RB-T9", "RB-GONE", "RB-T1"))

            assertEquals(listOf("RB-T9", "RB-T1"), rows.map { it.cardId })
            assertEquals("Synthetic card RB-T9?", rows.first().title)
        }
}
