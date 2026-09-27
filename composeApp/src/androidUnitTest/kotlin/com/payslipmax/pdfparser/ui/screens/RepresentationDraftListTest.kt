package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.database.RepresentationDraftEntity
import kotlin.test.Test

/**
 * [RepresentationDraftList] (docs/Plan/09_PayAudit_PhasePlan.md Phase 5, P7-19b): the UI-layer half of the
 * representation-gating path. Phase 5's repository-level tests
 * (FinancialIntelligenceRepositoryTest.testProcessPayslipGeneratesRepresentationDraftAndInsightsForMissingTPTA/
 * testProcessPayslipDoesNotGenerateDraftForUnprovenSalaryLoss) already prove a proven finding inserts a
 * draft and an unproven one does not; this covers the other half — that an inserted draft actually renders
 * as a card, and that an empty draft list (what an unproven finding leaves behind) renders the empty state
 * rather than nothing.
 */
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class RepresentationDraftListTest {
    private val provenDraft =
        RepresentationDraftEntity(
            id = "draft-1",
            disputeMonth = "05/2026",
            disputeType = "MISSING_TPTA",
            recipient = "PCDA_O_PUNE",
            subject = "Missing Transport Allowance for 05/2026",
            bodyText = "Body of the letter.",
            createdAt = 0L,
        )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aDraftFromAProvenFindingRendersAsACard() =
        runComposeUiTest {
            setContent { RepresentationDraftList(drafts = listOf(provenDraft), onBack = {}, onSelect = {}, onExportPdf = {}) }

            onNodeWithText("Missing Transport Allowance for 05/2026").assertExists()
            onNodeWithText("Month: 05/2026", substring = true).assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun noDraftsRendersTheEmptyStateNotAnEmptyList() =
        runComposeUiTest {
            setContent { RepresentationDraftList(drafts = emptyList(), onBack = {}, onSelect = {}, onExportPdf = {}) }

            onNodeWithText(
                "No salary discrepancies or missing allowances detected. Representation drafts will appear here if the local audit engine flags any issues.",
            ).assertExists()
        }
}
