package com.payslipmax.pdfparser.di

import com.payslipmax.pdfparser.billing.BillingManager
import com.payslipmax.pdfparser.domain.AppIntegrityChecker
import com.payslipmax.pdfparser.insights.gemma.GemmaModelStorageManager
import com.payslipmax.pdfparser.onboarding.OnboardingStorage
import com.payslipmax.pdfparser.rating.RatingPromptManager
import com.payslipmax.pdfparser.telemetry.GemmaInstallTelemetry
import com.payslipmax.pdfparser.telemetry.InstallationIdStorage
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.test.Test

/**
 * Static check of the whole production graph (`sharedModule` + `appModule`): every constructor parameter
 * of every declared binding must itself be declared, or listed here as a type the class defaults. A binding
 * that grows a new required dependency without a definition fails here, at test time, not as a runtime
 * `NoDefinitionFoundException` on first screen open. [AppKoinModulesTest] covers actual resolution.
 */
@OptIn(KoinExperimentalAPI::class)
class AppModuleGraphTest {
    @Test
    fun everyBindingsConstructorDependenciesAreDeclared() {
        module { includes(sharedModule, appModule) }.verify(
            extraTypes =
                listOf(
                    CoroutineDispatcher::class,
                    GemmaModelStorageManager::class,
                    InstallationIdStorage::class,
                    GemmaInstallTelemetry::class,
                    AppIntegrityChecker::class,
                    BillingManager::class,
                    RatingPromptManager::class,
                    OnboardingStorage::class,
                    Function0::class,
                    Function3::class,
                ),
        )
    }
}
