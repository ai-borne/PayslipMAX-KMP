package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.guide.domain.GuideIndex
import com.payslipmax.pdfparser.guide.domain.GuideProfileBuilder
import com.payslipmax.pdfparser.guide.domain.GuideSearchIndex
import com.payslipmax.pdfparser.guide.domain.GuideSearchScope
import com.payslipmax.pdfparser.testing.SyntheticGuideRuleChange
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/**
 * The JVM proves the bundle is correct, not that Kotlin/Native parses it fast (see `ParserUtilsIosPerfTest`
 * for the regex stall that motivated these checks). This reads the real bundle from the iOS app's compose
 * resources, so it also proves the file is packaged on iOS, then times parse plus validation.
 */
class GuideLoaderIosPerfTest {
    @Test
    fun theShippedBundleParsesWithinBudgetOnNative() =
        runTest {
            val text = GuideBundleContract.readShippedBundleText()

            val mark = TimeSource.Monotonic.markNow()
            val bundle = GuideBundleContract.parseShippedBundle(text)
            val elapsedMs = mark.elapsedNow().inWholeMilliseconds

            assertTrue(elapsedMs < PARSE_BUDGET_MS, "parse and validate took ${elapsedMs}ms, budget ${PARSE_BUDGET_MS}ms")
            GuideBundleContract.assertMatchesCompiledDataset(bundle)
            GuideBundleContract.assertTilesMatchDataset(bundle)
        }

    @Test
    fun everyFeedAndCardBuildsWithinBudgetOnNative() =
        runTest {
            val bundle = GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText())

            // Index, all 44 feeds and all 402 cards: far more than one screen ever builds at once.
            val mark = TimeSource.Monotonic.markNow()
            GuideBundleContract.assertFeedsAndCardsMatchDataset(bundle)
            val elapsedMs = mark.elapsedNow().inWholeMilliseconds

            assertTrue(elapsedMs < FEEDS_BUDGET_MS, "feeds and cards took ${elapsedMs}ms, budget ${FEEDS_BUDGET_MS}ms")
        }

    @Test
    fun ruleHistoryAndChipsForEveryCardWithinBudgetOnNative() =
        runTest {
            val bundle = GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText())

            // M4: every row asks the index for its chips, so build history and change log once and ask for all 404 cards, plain
            // list and map work. Then the same invariants as on the JVM, and the rule-change fixture (a replaced pair).
            val mark = TimeSource.Monotonic.markNow()
            val index = GuideIndex(bundle)
            repeat(CHIP_REPEATS) { bundle.cards.forEach { index.trust(it) } }
            val elapsedMs = mark.elapsedNow().inWholeMilliseconds

            println("guide chips on Native: $CHIP_REPEATS passes over ${bundle.cards.size} cards ${elapsedMs}ms")
            assertTrue(elapsedMs < FEEDS_BUDGET_MS, "chips took ${elapsedMs}ms, budget ${FEEDS_BUDGET_MS}ms")
            GuideBundleContract.assertRuleHistoryBehaves(bundle)
            GuideBundleContract.assertRuleHistoryBehaves(SyntheticGuideRuleChange.bundle())
        }

    @Test
    fun searchBuildsAndAnswersWithinBudgetOnNative() =
        runTest {
            val bundle = GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText())

            // The index over all 402 cards, then the fixed set of realistic queries (a search per keystroke). Plain
            // string scans, so a regex or quadratic regression on Native would blow these budgets.
            val buildMark = TimeSource.Monotonic.markNow()
            val index = GuideSearchIndex(bundle)
            val buildMs = buildMark.elapsedNow().inWholeMilliseconds
            val queryMark = TimeSource.Monotonic.markNow()
            val hits = GuideBundleContract.realisticQueries.sumOf { index.search(it).size }
            val queriesMs = queryMark.elapsedNow().inWholeMilliseconds

            println("guide search on Native: index ${buildMs}ms, ${GuideBundleContract.realisticQueries.size} queries ${queriesMs}ms")
            assertTrue(hits > 0)
            assertTrue(buildMs < SEARCH_BUDGET_MS, "index build took ${buildMs}ms, budget ${SEARCH_BUDGET_MS}ms")
            assertTrue(queriesMs < SEARCH_BUDGET_MS, "queries took ${queriesMs}ms, budget ${SEARCH_BUDGET_MS}ms")
        }

    @Test
    fun searchGivesTheSameAnswersOnNativeAsOnTheJvm() =
        runTest {
            // The full correctness workload, untimed: it proves Native reads words and rule numbers the same way.
            GuideBundleContract.assertSearchMatchesDataset(GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText()))
        }

    @Test
    fun thePart2LookupIsValidatedAndHeldOnNativeToo() =
        runTest {
            // Part II phase 1: 303 code pages and 48 cases are validated on every load, with plain scans and no regex; the parse test
            // above already times that validation, so this proves Native reads the same section the JVM does.
            val bundle = GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText())
            Part2LookupContract.assertPart2LookupReachesTheApp(bundle)
            Part2LookupContract.assertCodePagesCarryTheManualsOwnWords(bundle)
            Part2LookupContract.assertRibbonsHomeEverySerialOnce(bundle)
            Part2LookupContract.assertOnlyLeaveAndRecordsAreBuiltAndFiftyThreeSerialsAreLookupOnly(bundle)
            Part2LookupContract.assertRulesMatchTheRegistry(bundle)
        }

    @Test
    fun payAuditLinksPointAtRealCardsOnNativeToo() =
        runTest {
            // E7: a plain id lookup (no regex), checked on Native so the iOS link can never lead to a missing card.
            GuideBundleContract.assertPayAuditLinksPointAtRealCards(GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText()))
        }

    @Test
    fun pinsAndShareNotesMatchTheDatasetOnNativeToo() =
        runTest {
            // E8: plain string building and a character scan (no regex), checked on Native for all 402 cards.
            GuideBundleContract.assertPinsAndShareNotesMatchDataset(GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText()))
        }

    @Test
    fun previewSearchAndTrustBehaveOnNativeAsOnTheJvm() =
        runTest {
            val bundle = GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText())

            // E5: the free preview searches titles and rule numbers only; time its realistic queries like the full ones.
            val index = GuideSearchIndex(bundle)
            val queryMark = TimeSource.Monotonic.markNow()
            val previewHits = GuideBundleContract.realisticQueries.sumOf { index.search(it, GuideSearchScope.PREVIEW).size }
            val queriesMs = queryMark.elapsedNow().inWholeMilliseconds

            println("guide preview search on Native: ${GuideBundleContract.realisticQueries.size} queries ${queriesMs}ms")
            assertTrue(previewHits > 0)
            assertTrue(queriesMs < SEARCH_BUDGET_MS, "preview queries took ${queriesMs}ms, budget ${SEARCH_BUDGET_MS}ms")
            // The chip counts, the locked cards and the no-leak preview check, untimed: a correctness workload.
            GuideBundleContract.assertTrustAndPreviewMatchDataset(bundle)
        }

    @Test
    fun yourFigureBuildsFromALongHistoryWithinBudgetOnNative() =
        runTest {
            val bundle = GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText())
            val history = GuideFiguresContract.realisticHistory(months = 140)

            // E6: opening a figure card builds the profile from every stored payslip (Pay Audit's timeline), then resolves.
            val mark = TimeSource.Monotonic.markNow()
            repeat(FIGURE_REPEATS) { GuideProfileBuilder.from(history) }
            val elapsedMs = mark.elapsedNow().inWholeMilliseconds

            println("guide figure on Native: $FIGURE_REPEATS profiles over ${history.size} payslips ${elapsedMs}ms")
            assertTrue(elapsedMs < FIGURE_BUDGET_MS, "profiles took ${elapsedMs}ms, budget ${FIGURE_BUDGET_MS}ms")
            // The correctness workload, untimed: Native resolves the same amounts as the JVM.
            GuideFiguresContract.assertShippedFiguresMatchTheApprovedLetters(bundle)
            GuideFiguresContract.assertPreClaimFiguresMatchTheApprovedRegister(bundle)
            GuideFiguresContract.assertPermanentDutyEstimatesMatchTheApprovedRules(bundle)
            GuideFiguresContract.assertTemporaryDutyEstimatesMatchTheApprovedRules(bundle)
            GuideFiguresContract.assertOnlyPersonalCardsResolveAFigure(bundle)
            GuideFiguresContract.assertEveryPayLevelHasARate(bundle)
            GuideFiguresContract.assertTransportBasesMatchPayAudit(bundle)
            GuideFiguresContract.assertHistoryResolvesAllFourCards(bundle, history)
            // Plan 22, P3: the worksheet decodes and validates on Native exactly as on the JVM.
            GuidePreClaimContract.assertPreClaimWorksheetReachesTheApp(bundle)
            GuidePreClaimContract.assertRulePickerLinksToRealCards(bundle)
            // Plan 22, P6: the worksheet flow does the same sums on Native.
            PreClaimFlowContract.assertTheWorksheetFlowRunsOnTheShippedBundle(bundle)
        }

    @Test
    fun batchOneDraftsTheAnnualLeaveEventWithinBudgetOnNative() =
        runTest {
            val bundle = GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText())
            Part2BatchOneContract.assertBatchOneDraftsOnTheShippedBundle(bundle)

            // The leave form redrafts on every change: 200 drafts of the 7-row event (30-day AL with ration and R&H) on the real templates.
            val engine = Part2BatchOneContract.engine(bundle)
            val mark = TimeSource.Monotonic.markNow()
            repeat(DRAFT_REPEATS) { Part2BatchOneContract.draftAnnualLeave(engine) }
            val elapsedMs = mark.elapsedNow().inWholeMilliseconds

            println("part2 batch one on Native: $DRAFT_REPEATS drafts of the 7-row event ${elapsedMs}ms")
            assertTrue(elapsedMs < DRAFT_BUDGET_MS, "$DRAFT_REPEATS drafts took ${elapsedMs}ms, budget ${DRAFT_BUDGET_MS}ms")
        }

    @Test
    fun theLeaveFormRebuildsItsScreenWithinBudgetOnNative() =
        runTest {
            val bundle = GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText())
            Part2DraftContract.assertEveryCheckAndQuestionHasItsWords(bundle)
            Part2DraftContract.assertTheFormCompletesEveryOfferedCode(bundle)
            Part2DraftContract.assertTheLeaveBankFillIsOfferedOnTheYearEndFormsOnly(bundle)

            // Phase 4b: the form rebuilds its whole screen (fields, questions, slip, checks) on every change, on the main thread.
            val builder = Part2DraftContract.newBuilder(bundle)
            val mark = TimeSource.Monotonic.markNow()
            repeat(DRAFT_REPEATS) { assertEquals(7, Part2DraftContract.rowsOf(builder)) }
            val elapsedMs = mark.elapsedNow().inWholeMilliseconds

            println("part2 leave form on Native: $DRAFT_REPEATS rebuilds of the 7-row screen ${elapsedMs}ms")
            assertTrue(elapsedMs < DRAFT_BUDGET_MS, "$DRAFT_REPEATS rebuilds took ${elapsedMs}ms, budget ${DRAFT_BUDGET_MS}ms")
        }

    @Test
    fun thePart2BrowseAndSearchRunWithinBudgetOnNative() =
        runTest {
            val bundle = GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText())
            Part2BrowseContract.assertTilesShowOnlyForTheBuiltRibbons(bundle)
            Part2BrowseContract.assertOnlyTheTwoBlankGroupsAreBlankAndEveryOtherGroupHasItsMeaning(bundle)
            Part2BrowseContract.assertRejoinalIsTheOnlyCodeThePageSaysTheSlipPrintsDifferently(bundle)

            // Part II phase 4a: all 303 code pages found by serial, by each of 321 codes and by name (about 930 searches over 303 entries,
            // plain scans, no regex): far more than a keystroke ever costs.
            val mark = TimeSource.Monotonic.markNow()
            Part2BrowseContract.assertEveryCodePageIsReachableBySerialCodeAndName(bundle)
            val elapsedMs = mark.elapsedNow().inWholeMilliseconds

            println("part2 browse on Native: every code page found three ways in ${elapsedMs}ms")
            assertTrue(elapsedMs < SEARCH_BUDGET_MS, "the Part II search walk took ${elapsedMs}ms, budget ${SEARCH_BUDGET_MS}ms")
        }

    private companion object {
        // Generous for a debug simulator build: the first Guide open waits on this, and a quadratic
        // regression in the parser or validator would blow far past it.
        const val PARSE_BUDGET_MS = 1_500L

        // The same margin for the E3 feed and card derivations (plain list and string scans, no regex).
        const val FEEDS_BUDGET_MS = 1_500L

        // E4 search, for the index build and for the realistic queries each: the same margin again.
        const val SEARCH_BUDGET_MS = 1_500L

        // M4: ten passes of every card's chips (a feed asks for its rows, search for its hits); the same margin again.
        const val CHIP_REPEATS = 10

        // E6: ten profile builds over a 140-month history (a card open builds one); the same margin again.
        const val FIGURE_REPEATS = 10
        const val FIGURE_BUDGET_MS = 1_500L

        // Part II batch 1: 200 drafts of the 7-row annual-leave event on the real templates (the form redrafts on each change).
        const val DRAFT_REPEATS = 200
        const val DRAFT_BUDGET_MS = 3_000L
    }
}
