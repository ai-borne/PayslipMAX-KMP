package com.payslipmax.pdfparser.di

import androidx.lifecycle.viewModelScope
import com.payslipmax.pdfparser.database.PayslipDao
import com.payslipmax.pdfparser.guide.GuideRepository
import com.payslipmax.pdfparser.onboarding.OnboardingManager
import com.payslipmax.pdfparser.parser.PdfParser
import com.payslipmax.pdfparser.repository.FinancialIntelligenceRepository
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.telemetry.CrashReporter
import com.payslipmax.pdfparser.telemetry.InstallationIdManager
import com.payslipmax.pdfparser.testing.FakeOnboardingStorage
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.screens.PayAuditViewModel
import com.payslipmax.pdfparser.ui.screens.guide.GuidePinsModel
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import com.payslipmax.pdfparser.ui.screens.guide.isGuideEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.Koin
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame

/**
 * Both platforms start Koin from [appKoinModules], so this commonTest runs the real graph on the JVM and on
 * Kotlin/Native (`iosSimulatorArm64Test`). A binding that is missing on one platform would otherwise surface
 * only as a runtime crash on first screen composition. Only the leaves that need a database file or a PDF
 * engine are replaced; every wiring between the modules is the production one.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppKoinModulesTest {
    // PayslipViewModel launches on Dispatchers.Main, which has no implementation on a plain JVM test.
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    // Every PayslipViewModel starts collectors on viewModelScope, which dispatches on Main. One that is still running when
    // resetMain() removes the test Main resumes on a worker thread and throws "Main dispatcher missing"; kotlinx-coroutines-test
    // then reports it as UncaughtExceptionsBeforeTest on whichever test runs next. So the ones built here are stopped first.
    private val built = mutableListOf<PayslipViewModel>()

    @AfterTest
    fun tearDown() {
        runBlocking { built.forEach { it.viewModelScope.coroutineContext.job.cancelAndJoin() } }
        built.clear()
        Dispatchers.resetMain()
    }

    private fun Koin.payslipViewModel(): PayslipViewModel = get<PayslipViewModel>().also(built::add)

    private val testLeaves =
        module {
            single<PayslipDao> { FakePayslipDao() }
            single<PdfParser> { FakePdfParser() }
        }

    private fun start(platformModules: List<org.koin.core.module.Module> = emptyList()): Koin =
        koinApplication { modules(appKoinModules(platformModules = listOf(testLeaves) + platformModules)) }.koin

    @Test
    fun everyBindingTheScreensInjectResolves() {
        val koin = start()

        koin.get<PayslipRepository>()
        koin.get<FinancialIntelligenceRepository>()
        koin.get<CrashReporter>()
        koin.get<InstallationIdManager>()
        koin.get<PayslipBackupService>()
        koin.payslipViewModel()
        koin.get<PayAuditViewModel>()
        // Resolved by every screen that gates onboarding; unbound it throws on the first composition.
        koin.get<OnboardingManager>()
    }

    @Test
    fun theViewModelBacksUpThroughTheSharedBackupService() {
        val koin = start()

        // A ViewModel built without it reports "backup unavailable" on every export and restore, and
        // nothing else in the graph would notice: the field is nullable so older call sites still compile.
        assertSame(koin.get<PayslipBackupService>(), koin.payslipViewModel().backupService)
    }

    @Test
    fun onboardingStateIsOneInstanceSoEveryScreenSeesTheSameFlags() {
        val koin = start()

        // App, Dashboard and Pay Audit each ask for the manager; a per-call instance would let one screen
        // dismiss a sheet that another still shows.
        assertSame(koin.get<OnboardingManager>(), koin.get<OnboardingManager>())
    }

    // The Guide graph on its own: the app includes it only where the Guide is enabled (see the next test).
    private fun startGuide(): Koin = koinApplication { modules(sharedModule, guideModule, testLeaves) }.koin

    @Test
    fun guideBindingsExistExactlyWhereTheGuideIsEnabled() {
        // Release keeps the Guide dark until phase E9, and leaving guideModule out lets R8 drop all Guide code from it.
        assertEquals(isGuideEnabled(), start().getOrNull<GuideRepository>() != null)
        assertEquals(isGuideEnabled(), start().getOrNull<GuideViewModel>() != null)
    }

    @Test
    fun guideRepositoryIsOneInstanceSoTheBundleIsParsedOncePerProcess() {
        val koin = startGuide()

        // Every Guide screen asks for it; a per-call instance would re-read and re-parse 250 KB each time.
        assertSame(koin.get<GuideRepository>(), koin.get<GuideRepository>())
    }

    @Test
    fun guideViewModelIsAppScopedSoTabSwitchesAndLockKeepTheLoadedGuide() {
        val koin = startGuide()

        assertSame(koin.get<GuideViewModel>(), koin.get<GuideViewModel>())
    }

    @Test
    fun theGuideAndTheCardScreensShareOnePinsModelSoAPinShowsEverywhere() {
        val koin = startGuide()

        assertSame(koin.get<GuidePinsModel>(), koin.get<GuideViewModel>().pins)
    }

    @Test
    fun payslipViewModelIsOnePerRequestBecauseEachScreenScopeOwnsItsCollectors() {
        val koin = start()

        // A shared instance would outlive the screen that cancels its scope, and a later screen would get a dead model.
        assertNotSame(koin.payslipViewModel(), koin.payslipViewModel())
    }

    @Test
    fun payAuditViewModelIsOnePerRequestBecauseItOwnsAScopeThatTheScreenDisposes() {
        val koin = start()

        assertNotSame(koin.get<PayAuditViewModel>(), koin.get<PayAuditViewModel>())
    }

    @Test
    fun platformModulesLoadLastSoAPlatformCanReplaceADefault() {
        val storage = FakeOnboardingStorage(hasCompletedOnboarding = true)
        val koin = start(platformModules = listOf(module { single { OnboardingManager(storage) } }))

        assertFalse(koin.get<OnboardingManager>().shouldShowOnboarding())
    }
}
