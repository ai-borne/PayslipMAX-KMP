package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.payslipmax.pcdao.reconciliation.ActiveSituationalContext
import com.payslipmax.pcdao.reconciliation.MissionPresetId
import com.payslipmax.pcdao.reconciliation.SituationalCategory
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
class SituationalMatrixAffordanceTest {
    @AfterTest
    fun tearDown() {
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    @Test
    fun missionPresetsCarousel_displaysSimulationHint_andAllowsSelection() =
        runComposeUiTest {
            var selectedPreset: MissionPresetId? = null

            setContent {
                MissionPresetsCarousel(
                    activePresetId = null,
                    onPresetSelected = { selectedPreset = it },
                )
            }

            // Verify safe simulation baseline clarification hint is visible
            onNodeWithText(AppStringsPcdao.presetsSimulationHint).assertIsDisplayed()

            // Verify tapping a preset triggers callback with correct id
            onNodeWithTag("preset_chip_${MissionPresetId.SIACHEN_BRIGADE.name}").performClick()
            waitForIdle()
            assertEquals(MissionPresetId.SIACHEN_BRIGADE, selectedPreset)
        }

    @Test
    fun impactCountersStrip_hazardsCardIsClickable_triggersCallback() =
        runComposeUiTest {
            var hazardCardClicked = false

            setContent {
                ImpactCountersStrip(
                    unclaimedAmount = 12500.0,
                    hazardAmount = 42000.0,
                    criticalAlarmsCount = 2,
                    onHazardCardClick = { hazardCardClicked = true },
                )
            }

            val hazardCard = onNodeWithTag(TestTags.HAZARDS_KPI_CARD, useUnmergedTree = true)
            hazardCard.assertIsDisplayed()
            hazardCard.performClick()
            waitForIdle()
            assertTrue(hazardCardClicked)
        }

    @Test
    fun hazardExplainerDialog_displaysStatutoryNotice_andDismisses() =
        runComposeUiTest {
            var dismissed = false

            setContent {
                HazardExplainerDialog(
                    onDismiss = { dismissed = true },
                )
            }

            onNodeWithTag(TestTags.HAZARD_EXPLAINER_DIALOG).assertIsDisplayed()
            onNodeWithText(AppStringsPcdao.hazardDialogTitle).assertIsDisplayed()
            onNodeWithText(AppStringsPcdao.hazardDialogBody).assertIsDisplayed()

            onNodeWithTag("hazard_explainer_dismiss_button").performClick()
            waitForIdle()
            assertTrue(dismissed)
        }

    @Test
    fun situationalTileMatrix_categoryTabs_canBeSelected() =
        runComposeUiTest {
            var selectedCategory: SituationalCategory? = null

            setContent {
                SituationalTileMatrix(
                    selectedCategory = SituationalCategory.POSTING_OPS,
                    activeContext = ActiveSituationalContext(),
                    autoInferredTileIds = emptySet(),
                    onCategorySelected = { selectedCategory = it },
                    onToggleTile = {},
                    onOpenAddFactorSheet = {},
                )
            }

            // Select Housing & TLC tab
            onNodeWithText(AppStringsPcdao.tabHousingTlc).performClick()
            waitForIdle()
            assertEquals(SituationalCategory.HOUSING_TLC, selectedCategory)
        }

    @Test
    fun situationalCategoryTabBar_displaysScrollCue_whenCanScrollForward() =
        runComposeUiTest {
            setContent {
                Box(modifier = Modifier.width(150.dp)) {
                    SituationalCategoryTabBar(
                        selectedCategory = SituationalCategory.POSTING_OPS,
                        onCategorySelected = {},
                    )
                }
            }
            waitForIdle()

            val cueNode = onNodeWithTag(TestTags.MATRIX_SCROLL_CUE, useUnmergedTree = true)
            cueNode.assertExists()
            cueNode.assertIsDisplayed()
        }
}
