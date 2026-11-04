package com.payslipmax.pdfparser.ui.backup

import com.payslipmax.pdfparser.part2.data.RoomPart2Repository
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY (Phase 11 item 56): Settings asks the view model how many Orders Bank items a backup would carry, so a vault with no payslips can be
 * backed up, and a restore onto a device that holds only Bank items still asks Replace or Merge. Moved from
 * PayslipViewModelPart2ItemCountTest (docs/Plan/14a P7).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BackupRestoreViewModelPart2ItemCountTest {
    private val harness = BackupViewModelHarness()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() {
        harness.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun itCountsTheEntriesAndOrdersTheDeviceHolds() =
        runTest {
            val repository = RoomPart2Repository(harness.dao.part2)
            repository.saveEntry(SyntheticBank.entry("e1", 1))
            repository.saveOrder(SyntheticBank.order("o1"))

            assertEquals(2, harness.viewModel.part2ItemCount())
        }

    /** Rewritten in P7 from "without a backup service it is zero": that view model cannot be built any more. The intent is the same: a
     *  device without Bank items answers 0 (so a payslip-only device backs up and restores as before) rather than failing. */
    @Test
    fun aDeviceWithoutBankItemsAnswersZero() =
        runTest {
            harness.seedPayslip("08/2024")

            assertEquals(0, harness.viewModel.part2ItemCount())
        }
}
