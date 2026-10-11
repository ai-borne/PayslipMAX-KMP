package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import com.payslipmax.pdfparser.ui.theme.PreClaimStrings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test

/**
 * A Guide bundle that ships no worksheet (an older bundle with the flag on) reads as one message with a Retry, never as an empty
 * screen or a crash (plan 22, P6 owner decision, built here).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h2400dp")
class PreClaimUnavailableUiTest : PreClaimUiBase() {
    override fun bundle(): GuideBundle = (GuideBundleParser.parse(SyntheticGuideBundle.JSON) as GuideLoadResult.Loaded).bundle

    @Test
    fun aBundleWithoutAWorksheetShowsOneMessageAndARetryThatKeepsTheScreenStable() {
        show()
        tap(PreClaimStrings.entryTitle)
        composeRule.onNodeWithText(PreClaimStrings.unavailableTitle).assertIsDisplayed()
        composeRule.onNodeWithText(PreClaimStrings.unavailableBody).assertIsDisplayed()
        composeRule.onAllNodesWithText(PreClaimStrings.chooseMove).assertCountEquals(0)
        tap(GuideStrings.retry)
        composeRule.onNodeWithText(PreClaimStrings.unavailableTitle).assertIsDisplayed()
    }
}
