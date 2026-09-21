package com.payslipmax.pdfparser.di

import com.payslipmax.pdfparser.repository.FinancialIntelligenceRepository
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.ui.FakeFinancialIntelligenceRepository
import com.payslipmax.pdfparser.ui.PayslipViewModel
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
import kotlin.test.assertNotSame
import kotlin.test.assertSame

/**
 * The iOS installer's progress/completion reporters are process-wide statics that each new
 * installer overwrites, so a second [PayslipViewModel] built with its own installer silently
 * orphans the first one's subscriber. The ViewModel is a Koin `factory` (a new instance per
 * resolve), so the only thing keeping every ViewModel on one live installer is [appModule]
 * handing them the same one. A default-argument installer would compile fine and pass every
 * single-ViewModel test while reintroducing the orphaning.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GemmaInstallerDiWiringTest {
    private lateinit var koin: Koin

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        koin =
            koinApplication {
                modules(
                    appModule,
                    module {
                        single { PayslipRepository(FakePayslipDao(), FakePdfParser(), Dispatchers.Unconfined) }
                        single<FinancialIntelligenceRepository> { FakeFinancialIntelligenceRepository() }
                    },
                )
            }.koin
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun everyViewModelInstanceSharesOneGemmaInstaller() {
        val first = koin.get<PayslipViewModel>()
        val second = koin.get<PayslipViewModel>()

        assertNotSame(first, second, "premise: PayslipViewModel is a factory, so each resolve is a new instance")
        assertSame(
            first.gemmaBaseModelInstaller,
            second.gemmaBaseModelInstaller,
            "a second ViewModel must not get its own installer, or it overwrites the first one's static reporters",
        )
    }
}
