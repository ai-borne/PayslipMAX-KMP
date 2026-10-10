package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.payslipmax.pdfparser.guide.GuideBundleContract
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Guide Home lists one area per row, in the app's accent-card style with the area's emoji. The emoji is decoration:
 * a screen reader hears the area and its topic count as one button, never the emoji's name.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GuideHomeRowsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val areas = runBlocking { GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText()) }.toReady().areas

    @Test
    fun everyShippedAreaHasItsOwnEmoji() {
        // A new area in the bundle falls back to a generic book; this asks for a fitting emoji to be chosen for it.
        val generic = areas.filter { GuideStrings.areaEmoji(it.id) == GuideStrings.areaEmojiFallback }.map { it.id }
        assertTrue(generic.isEmpty(), "areas with no emoji of their own in GuideStrings.areaEmoji: $generic")
        assertEquals(areas.size, areas.map { GuideStrings.areaEmoji(it.id) }.toSet().size, "two areas share an emoji")
    }

    @Test
    fun anUnknownAreaStillGetsAnEmoji() {
        assertEquals(GuideStrings.areaEmojiFallback, GuideStrings.areaEmoji("not-an-area"))
        assertNotEquals("", GuideStrings.areaEmojiFallback)
    }

    @Test
    fun eachAreaRowIsOneButtonReadAsTitleAndTopicCount() {
        val opened = mutableListOf<String>()
        composeRule.setContent { GuideHomeScreen(areas, onOpenArea = { opened += it }, onOpenSearch = {}) }

        areas.forEach { tile ->
            composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(tile.title))
            val row = composeRule.onNodeWithText(tile.title)
            row.assertHasClickAction()
            val text = row.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString(" ")
            assertEquals("${tile.title} ${GuideStrings.topicCount(tile.caseCount)}", text)
            row.performClick()
        }
        assertEquals(areas.map { it.id }, opened)
    }
}
