package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.guide.preclaim.PreClaimElement
import com.payslipmax.pdfparser.guide.preclaim.PreClaimOutcome
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideProfileProvider
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticPreClaimWorksheet
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.assertIs

/**
 * Set-up shared by the pre-claim ViewModel tests: the Guide loaded from a bundle with the full figures and a worksheet of the real
 * shape, a payslip profile for the owner's claim A (level 13A, DA 46%), and a fake crash reporter that records everything leaving
 * the device. [open] collects the state the way a screen does, so the payslip is read only then.
 */
abstract class PreClaimViewModelTestBase {
    protected val dispatcher = StandardTestDispatcher()
    protected val crash = FakeCrashReporter()
    protected val payslips = FakeGuideProfileProvider(SyntheticPreClaimWorksheet.profile)

    protected fun guide(result: GuideLoadResult = GuideLoadResult.Loaded(SyntheticPreClaimWorksheet.bundle())) = GuideViewModel(FakeGuideRepository(result), crash, dispatcher, profiles = payslips)

    protected fun withBundle(bundle: GuideBundle) = guide(GuideLoadResult.Loaded(bundle))

    protected fun test(
        guide: GuideViewModel = guide(),
        block: suspend TestScope.(PreClaimViewModel) -> Unit,
    ) = runTest(dispatcher) { block(open(guide)) }

    protected fun TestScope.open(guide: GuideViewModel = guide()): PreClaimViewModel {
        val model = PreClaimViewModel(guide, crash, dispatcher)
        backgroundScope.launch(dispatcher) { model.state.collect {} }
        testScheduler.advanceUntilIdle()
        return model
    }

    /** Runs [change] and lets the state settle, so the next read sees its effect. */
    protected fun PreClaimViewModel.edit(change: PreClaimViewModel.() -> Unit): PreClaimWorksheet {
        change()
        dispatcher.scheduler.advanceUntilIdle()
        return worksheet()
    }

    protected fun PreClaimViewModel.worksheet(): PreClaimWorksheet = assertIs<PreClaimUiState.Ready>(state.value).worksheet

    protected inline fun <reified T : PreClaimOutcome> PreClaimWorksheet.outcome(element: PreClaimElement): T = assertIs<T>(estimates.single { it.element == element })

    protected fun withNoPayslip() {
        payslips.current.value = null
    }

    protected fun date(
        year: Int,
        month: Int,
        day: Int,
    ) = CalendarDate(year, month, day)
}
