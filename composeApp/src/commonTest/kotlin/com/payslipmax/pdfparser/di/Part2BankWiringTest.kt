package com.payslipmax.pdfparser.di

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.part2.data.InMemoryLastBackupStorage
import com.payslipmax.pdfparser.part2.data.LastBackupStorage
import com.payslipmax.pdfparser.part2.data.Part2Dao
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.FakePart2Dao
import com.payslipmax.pdfparser.testing.SyntheticBank
import com.payslipmax.pdfparser.testing.SyntheticGuidePart2
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.BankViewModel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * WHY: the Bank model is app-scoped like the Learn model, so an open entry form survives a tab switch, and it must read the same Learn model
 * (one loaded bundle) and the same encrypted store the backup reads. One test, a fresh module: Koin keeps a `single`'s instance inside the
 * module object (see [Part2WiringTest]).
 */
class Part2BankWiringTest {
    @Test
    fun theBankModelIsOneInstanceOverTheLearnModelAndTheStoreTheBackupReads() =
        runTest(StandardTestDispatcher()) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val guide = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(SyntheticGuidePart2.bundle())), FakeCrashReporter(), dispatcher)
            val dao = FakePart2Dao()
            val koin =
                koinApplication {
                    modules(
                        part2Module(),
                        module {
                            single { guide }
                            single<Part2Dao> { dao }
                            single<LastBackupStorage> { InMemoryLastBackupStorage() }
                        },
                    )
                }.koin
            assertSame(koin.get<BankViewModel>(), koin.get<BankViewModel>(), "one model, so an open form survives a tab switch")
            guide.load()
            testScheduler.advanceUntilIdle()

            val bank = koin.get<BankViewModel>()
            assertNotNull(bank.beginNew("FIRSTCODE"), "the form comes from the Learn model's loaded bundle")
            assertNull(bank.beginNew("NOSUCHCODE"))
            koin.get<Part2Repository>().saveEntry(SyntheticBank.entry("e1"))
            assertNotNull(dao.entry("e1"), "the repository writes the one store the backup reads")
        }
}
