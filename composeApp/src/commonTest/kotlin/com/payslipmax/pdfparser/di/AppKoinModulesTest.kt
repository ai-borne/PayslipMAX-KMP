package com.payslipmax.pdfparser.di

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.Koin
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
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

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

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
        koin.get<PayslipViewModel>()
        koin.get<PayAuditViewModel>()
        // Resolved by every screen that gates onboarding; unbound it throws on the first composition.
        koin.get<OnboardingManager>()
    }

    @Test
    fun theViewModelBacksUpThroughTheSharedBackupService() {
        val koin = start()

        // A ViewModel built without it reports "backup unavailable" on every export and restore, and
        // nothing else in the graph would notice: the field is nullable so older call sites still compile.
        assertSame(koin.get<PayslipBackupService>(), koin.get<PayslipViewModel>().backupService)
    }

    @Test
    fun onboardingStateIsOneInstanceSoEveryScreenSeesTheSameFlags() {
        val koin = start()

        // App, Dashboard and Pay Audit each ask for the manager; a per-call instance would let one screen
        // dismiss a sheet that another still shows.
        assertSame(koin.get<OnboardingManager>(), koin.get<OnboardingManager>())
    }

    @Test
    fun guideRepositoryIsOneInstanceSoTheBundleIsParsedOncePerProcess() {
        val koin = start()

        // Every Guide screen asks for it; a per-call instance would re-read and re-parse 250 KB each time.
        assertSame(koin.get<GuideRepository>(), koin.get<GuideRepository>())
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
