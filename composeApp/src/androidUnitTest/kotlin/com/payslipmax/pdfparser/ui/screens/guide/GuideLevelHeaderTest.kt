package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The breadcrumb reads as the header title's overline: its first crumb starts exactly where the title starts. Lining
 * it up must not shrink a crumb below the 48dp touch target, even the short "Guide".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GuideLevelHeaderTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val crumbs = listOf(GuideCrumb(GuideStrings.breadcrumbHome, emptyList()), GuideCrumb("Retirement and death", emptyList()))

    private fun show() =
        composeRule.setContent { GuideLevelHeader(crumbs, onCrumb = {}, title = "Retirement and release", subtitle = null, onBack = {}) }

    @Test
    fun theFirstCrumbStartsWhereTheTitleStarts() {
        show()
        // The node is the crumb's 48dp tap area; its label sits one padding further in (8dp).
        val crumbLeft = composeRule.onNodeWithText(GuideStrings.breadcrumbHome).getUnclippedBoundsInRoot().left + 8.dp
        val titleLeft = composeRule.onNodeWithText("Retirement and release").getUnclippedBoundsInRoot().left
        assertEquals(titleLeft, crumbLeft)
    }

    @Test
    fun everyCrumbKeepsA48dpTouchTarget() {
        show()
        crumbs.forEach { crumb ->
            composeRule
                .onNodeWithContentDescription(GuideStrings.breadcrumbDescription(crumb.label))
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }
    }
}
