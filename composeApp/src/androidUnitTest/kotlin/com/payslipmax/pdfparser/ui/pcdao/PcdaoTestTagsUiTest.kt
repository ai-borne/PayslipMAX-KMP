package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pcdao.reconciliation.ActiveSituationalContext
import com.payslipmax.pcdao.reconciliation.SituationalCategory
import com.payslipmax.pcdao.reconciliation.SituationalTileKeys
import com.payslipmax.pcdao.reconciliation.SpecializedMilitaryFactor
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.ui.screens.PremiumToolsSection
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp")
@OptIn(ExperimentalTestApi::class)
class PcdaoTestTagsUiTest {
    @AfterTest
    fun tearDown() {
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    @Test
    fun postingPeaceTileAndAddFactorButton_haveTestTagsAndCanBeClicked() =
        runComposeUiTest {
            var toggledTileId: String? = null
            var addFactorClicked = false

            setContent {
                SituationalTileMatrix(
                    selectedCategory = SituationalCategory.POSTING,
                    activeContext = ActiveSituationalContext(),
                    autoInferredTileIds = emptySet(),
                    onCategorySelected = {},
                    onToggleTile = { toggledTileId = it },
                    onOpenAddFactorSheet = { addFactorClicked = true },
                )
            }

            // Verify POSTING_PEACE_TILE testTag and clickability
            val peaceTile = onNodeWithTag(TestTags.POSTING_PEACE_TILE, useUnmergedTree = true)
            peaceTile.performClick()
            assertEquals(SituationalTileKeys.POST_PEACE_HIGHER, toggledTileId)

            // Verify ADD_FACTOR_BUTTON testTag and clickability
            val addFactorBtn = onNodeWithTag(TestTags.ADD_FACTOR_BUTTON)
            addFactorBtn.performClick()
            assertTrue(addFactorClicked)
        }

    @Test
    fun marcosCheckboxAndApplyButton_haveTestTagsAndCanBeInteracted() =
        runComposeUiTest {
            var toggledFactor: SpecializedMilitaryFactor? = null
            var dismissed = false

            setContent {
                AddFactorBottomSheet(
                    activeFactors = emptySet(),
                    onToggleFactor = { toggledFactor = it },
                    onDismiss = { dismissed = true },
                )
            }

            // Verify MARCOS_CHECKBOX testTag
            val marcosCheckbox = onNodeWithTag(TestTags.MARCOS_CHECKBOX, useUnmergedTree = true)
            marcosCheckbox.performClick()
            assertEquals(SpecializedMilitaryFactor.MARCOS_SPECIAL_FORCES, toggledFactor)

            // Verify APPLY_FACTORS_BUTTON testTag
            val applyBtn = onNodeWithTag(TestTags.APPLY_FACTORS_BUTTON)
            applyBtn.performClick()
            assertTrue(dismissed)
        }

    @Test
    fun redressalKitButton_hasTestTagAndTriggersAction() =
        runComposeUiTest {
            var redressalClicked = false

            setContent {
                FeedHeaderSection(
                    selectedFilter = FindingFilter.ALL,
                    onFilterSelected = {},
                    onRedressalClick = { redressalClicked = true },
                    isUnlocked = true,
                )
            }

            val redressalBtn = onNodeWithTag(TestTags.REDRESSAL_KIT_BUTTON)
            redressalBtn.performClick()
            assertTrue(redressalClicked)
        }

    @Test
    fun pcdaoCard_hasTestTagInPremiumToolsSection() =
        runComposeUiTest {
            var navigatedScreen: Screen? = null

            setContent {
                PremiumToolsSection(
                    onNavigateTo = { navigatedScreen = it },
                )
            }

            val card = onNodeWithTag(TestTags.PCDAO_CARD)
            card.performClick()
            waitForIdle()
            assertEquals(Screen.PcdaoAudit, navigatedScreen)
        }

    @Test
    fun hazardsKpiCardAndExplainerDialog_haveTestTagsAndInteract() =
        runComposeUiTest {
            var hazardCardClicked = false
            var dialogDismissed = false

            setContent {
                ImpactCountersStrip(
                    unclaimedAmount = 10000.0,
                    hazardAmount = 25000.0,
                    criticalAlarmsCount = 1,
                    onHazardCardClick = { hazardCardClicked = true },
                )
                HazardExplainerDialog(
                    onDismiss = { dialogDismissed = true },
                )
            }

            val hazardCard = onNodeWithTag(TestTags.HAZARDS_KPI_CARD, useUnmergedTree = true)
            hazardCard.performClick()
            assertTrue(hazardCardClicked)

            val dialog = onNodeWithTag(TestTags.HAZARD_EXPLAINER_DIALOG)
            dialog.assertIsDisplayed()

            val dismissBtn = onNodeWithTag("hazard_explainer_dismiss_button")
            dismissBtn.performClick()
            assertTrue(dialogDismissed)
        }
}
