package com.payslipmax.pdfparser.di

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.telemetry.CrashReporter
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticPreClaimWorksheet
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimMoveType
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimViewModel
import kotlinx.coroutines.test.StandardTestDispatcher
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

/**
 * WHY: unlike the Guide's app-scoped models, each worksheet must be a new instance, because the form it holds has to die with the
 * screen, a lock and process death. A `single` here would carry one officer's typed values into the next visit. The wiring also
 * has to use the Guide's own loaded bundle, not a second copy of it. Kept apart from [AppKoinModulesTest] like [GuideNotesWiringTest].
 */
class PreClaimWiringTest {
    private val dispatcher = StandardTestDispatcher()
    private val guide = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(SyntheticPreClaimWorksheet.bundle())), FakeCrashReporter(), dispatcher)
    private val crash = FakeCrashReporter()
    private val koin =
        koinApplication {
            modules(
                preClaimModule,
                module {
                    single { guide }
                    single<CrashReporter> { crash }
                },
            )
        }.koin

    @Test
    fun everyScreenGetsItsOwnWorksheetSoNoFormSurvivesIntoTheNextVisit() {
        assertNotSame(koin.get<PreClaimViewModel>(), koin.get<PreClaimViewModel>())
    }

    @Test
    fun theWorksheetUsesTheAppScopedGuideAndTheBoundCrashReporter() {
        val model = koin.get<PreClaimViewModel>()
        dispatcher.scheduler.advanceUntilIdle()

        model.chooseMove(PreClaimMoveType.PERMANENT_DUTY)

        assertSame(guide, koin.get<GuideViewModel>())
        assertEquals(listOf("preclaim_opened_PD"), crash.logs, "the breadcrumb goes through the reporter Koin binds, once the shared Guide has loaded")
        model.close()
    }
}
