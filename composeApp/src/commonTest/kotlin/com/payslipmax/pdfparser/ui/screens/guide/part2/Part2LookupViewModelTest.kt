package com.payslipmax.pdfparser.ui.screens.guide.part2

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.part2.domain.Part2SearchResult
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticGuidePart2
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import com.payslipmax.pdfparser.ui.screens.guide.MAX_SEARCH_QUERY_LENGTH
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * WHY: the ViewModel is what the Learn screens read, so these pin what the officer sees: every ribbon as a flat list, a casualty
 * page's fields, no inferred group, the REJOINAL note, and a search that reaches every code. It reads the loaded Guide and
 * has no CrashReporter at all, so nothing a code, a name or a query says can reach telemetry.
 */
class Part2LookupViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private fun model(
        bundle: GuideBundle = SyntheticGuidePart2.bundle(),
        loaded: Boolean = true,
        block: TestScope.(Part2LookupViewModel) -> Unit,
    ) = runTest(dispatcher) {
        val guide = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(bundle)), FakeCrashReporter(), dispatcher)
        if (loaded) {
            guide.load()
            testScheduler.advanceUntilIdle()
        }
        block(Part2LookupViewModel(guide))
    }

    @Test
    fun nothingShowsUntilTheGuideHasLoaded() =
        model(loaded = false) { part2 ->
            assertEquals(emptyList(), part2.ribbons())
            assertNull(part2.code("FIRSTCODE"))
            assertEquals(Part2SearchResult.Idle, part2.search("firstcode"), "there is nothing to search yet")
        }

    @Test
    fun aBundleWithoutPartTwoShowsNothing() =
        model(bundle = (SyntheticGuideBundle.withPart2()).copy(part2 = null)) { part2 ->
            assertEquals(emptyList(), part2.ribbons())
            assertNull(part2.ribbon("leave"))
            assertEquals(Part2SearchResult.Idle, part2.search("firstcode"))
        }

    @Test
    fun theHomeListsEveryRibbonAndEachRibbonIsOneFlatListUnderItsCaseNames() =
        model { part2 ->
            assertEquals(listOf("leave", "records"), part2.ribbons().map { it.id })
            val leave = assertNotNull(part2.ribbon("leave"))
            assertEquals(listOf("First case", "Second case"), leave.sections.map { it.title })
            assertEquals(listOf("FIRSTCODE", "SECONDCODE", "THIRDCODE", "FOURTH11"), leave.sections.flatMap { s -> s.rows.map { it.openCode } })
            assertNull(part2.ribbon("nope"))
        }

    @Test
    fun aCasualtyPageHasItsColumnsItsPlainEnglishPageAndItsCompanions() =
        model { part2 ->
            val page = assertNotNull(part2.code("FIRSTCODE"))
            assertEquals(3, page.columns.size)
            assertEquals("What it is.", page.about?.what)
            assertEquals(listOf("SECONDCODE", "STOPCODE"), page.related.map { it.code })
            assertNull(assertNotNull(part2.code("SECONDCODE")).about)
        }

    @Test
    fun aSerialTheManualGivesNoGroupHasNoGroupToShow() =
        model { part2 ->
            assertNull(assertNotNull(part2.code("STOPCODE")).group)
        }

    @Test
    fun aCodePageHasTheCodeNameGroupWithMeaningChapterAndManualPage() =
        model { part2 ->
            val page = assertNotNull(part2.code("THIRDCODE"))
            assertEquals(listOf("THIRDCODE"), page.codes)
            assertEquals("Released from the third thing", page.name)
            assertEquals("AM", page.group?.letter)
            assertEquals("Money, wanted by MISO", page.group?.meaning, "what the letter means is the bundle's para 3, never inferred here")
            assertEquals(2, page.chapter)
            assertEquals(39, page.page)
        }

    @Test
    fun searchFindsByCodeAndByName() =
        model { part2 ->
            assertEquals(listOf("1.1"), assertIs<Part2SearchResult.Results>(part2.search("firstcode")).rows.map { it.serial })
            assertEquals(listOf("3.27", "3.28"), assertIs<Part2SearchResult.Results>(part2.search("variant")).rows.map { it.serial })
        }

    @Test
    fun theQueryIsHeldInMemoryBoundedAndClearedOnEntry() =
        model { part2 ->
            part2.onQueryChange("variant")
            assertEquals("variant", part2.query.value)
            assertEquals(listOf("3.27", "3.28"), assertIs<Part2SearchResult.Results>(part2.search()).rows.map { it.serial }, "search() reads the held query")
            part2.onQueryChange("x".repeat(MAX_SEARCH_QUERY_LENGTH + 50))
            assertEquals(MAX_SEARCH_QUERY_LENGTH, part2.query.value.length, "a pasted wall of text is cut")
            part2.clear()
            assertEquals("", part2.query.value)
            assertEquals(Part2SearchResult.Idle, part2.search())
        }
}
