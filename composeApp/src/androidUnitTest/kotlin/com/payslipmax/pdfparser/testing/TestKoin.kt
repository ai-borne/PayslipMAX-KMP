package com.payslipmax.pdfparser.testing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.payslipmax.pdfparser.nav.PendingPart2Link
import com.payslipmax.pdfparser.onboarding.OnboardingManager
import com.payslipmax.pdfparser.onboarding.OnboardingStorage
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.reminders.ShortDelayReminderScheduler
import com.payslipmax.pdfparser.ui.screens.PayAuditEngine
import com.payslipmax.pdfparser.ui.screens.PayAuditViewModel
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2DraftsViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.due.DueCardModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.due.DueViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.isPart2Enabled
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.koin.compose.KoinIsolatedContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.module.Module
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * Test double for the app's injected collaborators: a fake onboarding storage and a Pay Audit ViewModel
 * running a fake [payAuditEngine] on an unconfined dispatcher.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun testAppModule(
    storage: OnboardingStorage = FakeOnboardingStorage(hasCompletedOnboarding = true, hasSeenUploadCoachmark = true, hasSeenPayAuditIntro = true),
    payAuditEngine: PayAuditEngine? = null,
): Module =
    module {
        includes(*part2TestModules().toTypedArray())
        single { OnboardingManager(storage) }
        factory {
            if (payAuditEngine != null) {
                PayAuditViewModel(engine = payAuditEngine, dispatcher = UnconfinedTestDispatcher())
            } else {
                PayAuditViewModel(dispatcher = UnconfinedTestDispatcher())
            }
        }
    }

/**
 * The Part II bindings the app injects outside the Guide (the repository the Settings vault line reads, the pending notification tap, the Dashboard's Due card, the debug reminder switch), as fakes, and only where
 * Part II is on: a flag-on build resolves them from `part2Module`, and a test that builds its own graph must supply them or the first screen
 * fails with `NoDefinitionFoundException` (found by running the suite in a flag-on copy, Phase 10). Empty while the flag is off.
 */
fun part2TestModules(): List<Module> =
    if (!isPart2Enabled()) {
        emptyList()
    } else {
        listOf(
            module {
                val repository = FakePart2Repository()
                single<Part2Repository> { repository }
                single { PendingPart2Link(repository) }
                single { DueCardModel(DueViewModel(repository, Part2DraftsViewModel(GuideViewModel(FakeGuideRepository(), FakeCrashReporter()), repository))) }
                single { ShortDelayReminderScheduler(FakeReminderScheduler(), isDebug = { false }, now = { 0L }) }
            },
        )
    }

/**
 * For tests that render screens against the process-wide Koin the Robolectric Application started: puts the Part II fakes over the real
 * bindings, so a flag-on build never opens the real database (its native SQLite library is not loadable under Robolectric). A no-op while
 * the flag is off or no Koin is running.
 */
fun overridePart2WithFakes() {
    if (GlobalContext.getOrNull() != null) loadKoinModules(part2TestModules())
}

/**
 * Runs [content] against an isolated Koin built from [modules]. It never touches the process-wide Koin, so
 * it cannot clash with the one Robolectric's Application starts, and `koinInject()` inside resolves here.
 */
@Composable
fun WithTestKoin(
    vararg modules: Module,
    content: @Composable () -> Unit,
) {
    val application = remember { koinApplication { modules(*modules) } }
    DisposableEffect(application) { onDispose { application.close() } }
    KoinIsolatedContext(application, content)
}
