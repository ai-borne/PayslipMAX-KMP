package com.payslipmax.pdfparser.testing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.payslipmax.pdfparser.onboarding.OnboardingManager
import com.payslipmax.pdfparser.onboarding.OnboardingStorage
import com.payslipmax.pdfparser.ui.screens.PayAuditEngine
import com.payslipmax.pdfparser.ui.screens.PayAuditViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.koin.compose.KoinIsolatedContext
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
