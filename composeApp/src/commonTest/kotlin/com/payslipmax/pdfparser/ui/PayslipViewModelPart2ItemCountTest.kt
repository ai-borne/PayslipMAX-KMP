package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.part2.data.Part2Backup
import com.payslipmax.pdfparser.part2.data.RoomPart2Repository
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
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
 * WHY (Phase 11 item 56): Settings asks the view model how many Part II items a backup would carry, so a vault with no payslips can be
 * backed up. A view model built without a backup service (as in many tests) answers 0 rather than failing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PayslipViewModelPart2ItemCountTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        dao: FakePayslipDao,
        withBackup: Boolean,
    ) = PayslipViewModel(
        PayslipRepository(dao, FakePdfParser(), Dispatchers.Unconfined),
        backupService = if (withBackup) PayslipBackupService(dao, Dispatchers.Unconfined, part2Backup = Part2Backup(dao.part2)) else null,
    )

    @Test
    fun itCountsTheDraftsAndOrdersTheDeviceHolds() =
        runTest {
            val dao = FakePayslipDao()
            val repository = RoomPart2Repository(dao.part2)
            repository.saveDraft(SyntheticPart2Drafts.fullDraft("d1", 1))
            repository.saveOrder(SyntheticPart2Drafts.order("o1"))

            assertEquals(2, viewModel(dao, withBackup = true).part2ItemCount())
        }

    @Test
    fun withoutABackupServiceItIsZero() =
        runTest {
            val dao = FakePayslipDao()
            RoomPart2Repository(dao.part2).saveDraft(SyntheticPart2Drafts.fullDraft("d1", 1))

            assertEquals(0, viewModel(dao, withBackup = false).part2ItemCount())
        }
}
