package com.payslipmax.pdfparser.guide

import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The real bundle is read through Android assets, exactly as production reads it (compose resources are
 * packaged as assets), so a resource that is not packaged fails here rather than as an empty Guide tab.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GuideBundleAndroidContractTest {
    @Test
    fun theShippedBundleLoadsAndMatchesTheCompiledDataset() =
        runTest {
            val text = GuideBundleContract.readShippedBundleText()

            val bundle = GuideBundleContract.parseShippedBundle(text)
            GuideBundleContract.assertMatchesCompiledDataset(bundle)
            GuideBundleContract.assertTilesMatchDataset(bundle)
            GuideBundleContract.assertRuleHistoryBehaves(bundle)
            GuideBundleContract.assertFeedsAndCardsMatchDataset(bundle)
            GuideBundleContract.assertSearchMatchesDataset(bundle)
            GuideBundleContract.assertTrustAndPreviewMatchDataset(bundle)
            GuideBundleContract.assertPaywallOnlyWhenNoUnverifiedRateCard(bundle)
            GuideBundleContract.assertPayAuditLinksPointAtRealCards(bundle)
            GuideBundleContract.assertPinsAndShareNotesMatchDataset(bundle)
            GuideFiguresContract.assertShippedFiguresMatchTheApprovedLetters(bundle)
            GuideFiguresContract.assertPreClaimFiguresMatchTheApprovedRegister(bundle)
            GuideFiguresContract.assertPermanentDutyEstimatesMatchTheApprovedRules(bundle)
            GuideFiguresContract.assertTemporaryDutyEstimatesMatchTheApprovedRules(bundle)
            GuideFiguresContract.assertOnlyPersonalCardsResolveAFigure(bundle)
            GuideFiguresContract.assertEveryPayLevelHasARate(bundle)
            GuideFiguresContract.assertTransportBasesMatchPayAudit(bundle)
            GuideFiguresContract.assertHistoryResolvesAllFourCards(bundle, GuideFiguresContract.realisticHistory())
            GuidePreClaimContract.assertPreClaimWorksheetReachesTheApp(bundle)
            GuidePreClaimContract.assertRulePickerLinksToRealCards(bundle)
            PreClaimFlowContract.assertTheWorksheetFlowRunsOnTheShippedBundle(bundle)
            Part2LookupContract.assertPart2LookupReachesTheApp(bundle)
            Part2LookupContract.assertCodePagesCarryTheManualsOwnWords(bundle)
            Part2LookupContract.assertRibbonsHomeEverySerialOnce(bundle)
            Part2LookupContract.assertOnlyLeaveAndRecordsAreBuiltAndFiftyThreeSerialsAreLookupOnly(bundle)
            Part2LookupContract.assertRulesMatchTheRegistry(bundle)
            Part2BatchOneContract.assertBatchOneDraftsOnTheShippedBundle(bundle)
            Part2DraftContract.assertEveryCheckAndQuestionHasItsWords(bundle)
            Part2DraftContract.assertTheFormCompletesEveryOfferedCode(bundle)
            Part2DraftContract.assertTheLeaveBankFillIsOfferedOnTheYearEndFormsOnly(bundle)
            assertEquals(7, Part2DraftContract.rowsOf(Part2DraftContract.newBuilder(bundle)))
            Part2BrowseContract.assertTilesShowOnlyForTheBuiltRibbons(bundle)
            Part2BrowseContract.assertEveryCodePageIsReachableBySerialCodeAndName(bundle)
            Part2BrowseContract.assertOnlyTheTwoBlankGroupsAreBlankAndEveryOtherGroupHasItsMeaning(bundle)
            Part2BrowseContract.assertRejoinalIsTheOnlyCodeThePageSaysTheSlipPrintsDifferently(bundle)
        }
}
