package com.payslipmax.pdfparser.di

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.part2.domain.Part2SearchResult
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuidePart2
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2LookupViewModel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

/**
 * WHY: the Learn model is app-scoped like the Guide's own search, so a typed query survives opening a casualty page and coming back, and
 * it must read the Guide's one loaded bundle rather than a second copy. One test on purpose, and a fresh module for it: Koin keeps a
 * `single`'s instance inside the module object, so a second start in this JVM would see the first one's Guide. Kept apart from [AppKoinModulesTest] like [PreClaimWiringTest].
 */
class Part2WiringTest {
    @Test
    fun theLearnModelIsOneInstanceOverTheGuidesOwnLoadedBundle() =
        runTest(StandardTestDispatcher()) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val guide = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(SyntheticGuidePart2.bundle())), FakeCrashReporter(), dispatcher)
            val koin = koinApplication { modules(part2Module(), module { single { guide } }) }.koin
            assertSame(koin.get<Part2LookupViewModel>(), koin.get<Part2LookupViewModel>(), "one model, so the query survives a tab switch")

            guide.load()
            testScheduler.advanceUntilIdle()

            val rows = assertIs<Part2SearchResult.Results>(koin.get<Part2LookupViewModel>().search("firstcode")).rows
            assertEquals(listOf("1.1"), rows.map { it.serial }, "it reads the Guide that Koin hands out, not a second copy")
        }
}
