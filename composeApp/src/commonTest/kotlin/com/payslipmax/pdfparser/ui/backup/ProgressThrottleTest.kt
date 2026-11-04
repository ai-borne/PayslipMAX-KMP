package com.payslipmax.pdfparser.ui.backup

import com.payslipmax.pdfparser.repository.BackupOp
import com.payslipmax.pdfparser.repository.BackupStage
import com.payslipmax.pdfparser.ui.backup.BackupViewModelHarness.Companion.PASSWORD
import com.payslipmax.pdfparser.ui.screens.BackupContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P7): a 250 MB vault reports progress once per page and per row, thousands of times a second. Each state change
 * recomposes the sheet, so the view model passes on about ten a second, and always the latest: a bar that stops short of where the work is
 * would tell the officer it hung.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProgressThrottleTest {
    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun aBurstOfUpdatesBecomesAboutTenASecondAndTheLastIsAlwaysDelivered() =
        runTest {
            val delivered = mutableListOf<BackupUiState.Working>()
            val throttle = ProgressThrottle(backgroundScope) { delivered += it }
            throttle.start(BackupUiState.Working(BackupOp.BACKUP, BackupStage.OPENING, 0, 0))
            val progress = throttle.progress(BackupOp.BACKUP)
            runCurrent()

            // One second of a page every 10 ms.
            for (page in 1..100) {
                progress.update(BackupStage.PAGES, page, 100)
                advanceTimeBy(10)
                runCurrent()
            }
            advanceTimeBy(ProgressThrottle.INTERVAL_MS)
            runCurrent()

            assertTrue(delivered.size in 10..13, "${delivered.size} updates were passed on for one second of work")
            assertEquals(BackupUiState.Working(BackupOp.BACKUP, BackupStage.PAGES, 100, 100), delivered.last(), "the latest one arrives")
            throttle.stop()
        }

    @Test
    fun nothingIsPassedOnOnceTheOperationHasStopped() =
        runTest {
            val delivered = mutableListOf<BackupUiState.Working>()
            val throttle = ProgressThrottle(backgroundScope) { delivered += it }
            throttle.start(BackupUiState.Working(BackupOp.RESTORE, BackupStage.OPENING, 0, 0))
            val progress = throttle.progress(BackupOp.RESTORE)
            progress.update(BackupStage.READING, 1, 9)
            runCurrent()
            progress.update(BackupStage.READING, 2, 9)

            throttle.stop()
            advanceTimeBy(10 * ProgressThrottle.INTERVAL_MS)
            runCurrent()

            assertTrue(delivered.none { it.done == 2 }, "a late update would overwrite the result the operation ended with")
        }

    /**
     * The view model's own progress goes through the throttle: the read-back of 36 payslips reports each one, and must not surface each.
     * The service runs on its own dispatcher, as in the app (off the main thread), so a state set per row is seen as it is set.
     */
    @Test
    fun aBackupOfManyRowsShowsAFewWorkingStatesNotOnePerRow() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val harness = BackupViewModelHarness(serviceDispatcher = StandardTestDispatcher(testScheduler))
            for (year in 2022..2024) for (month in 1..12) harness.seedPayslip("${month.toString().padStart(2, '0')}/$year")
            harness.record(this)

            harness.viewModel.backUp(PASSWORD, BackupContents(payslips = 36, part2Items = 0))
            advanceUntilIdle()

            assertIs<BackupUiState.Done>(harness.viewModel.state.value)
            val working = harness.states.filterIsInstance<BackupUiState.Working>()
            assertTrue(working.size <= 4, "${working.size} working states for one backup that took no time")
            harness.clear()
        }

    /** The same for a restore: opening 36 payslips reports each one, and the states up to its preview are a few, not one per row. */
    @Test
    fun aRestoreOfManyRowsShowsAFewWorkingStatesNotOnePerRow() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val harness = BackupViewModelHarness(serviceDispatcher = StandardTestDispatcher(testScheduler))
            for (year in 2022..2024) for (month in 1..12) harness.seedPayslip("${month.toString().padStart(2, '0')}/$year")
            val picked = harness.pickedBackupOfTheDevice()
            harness.record(this)

            harness.viewModel.restore(picked, PASSWORD)
            advanceUntilIdle()

            assertIs<BackupUiState.Preview>(harness.viewModel.state.value)
            val working = harness.states.filterIsInstance<BackupUiState.Working>()
            assertTrue(working.size <= 4, "${working.size} working states for one restore that took no time")
            harness.clear()
        }
}
