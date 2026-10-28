package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult

/**
 * What a person who cannot see the screen, has large hands or has turned the font up needs from it, checked on the semantics Compose
 * builds. Each function returns the faults it found as plain sentences (empty is a pass), so a test can say which control is wrong.
 * Part II screens only ever show the UI copy and synthetic dates, so a control's name in a message is safe to print.
 */
internal class Part2A11yChecks(private val rule: ComposeTestRule) {
    private fun actions(): List<SemanticsNode> = rule.onAllNodes(hasClickAction()).fetchSemanticsNodes()

    private fun SemanticsNode.name(): String {
        val described = config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        val written = config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
        val typed = config.getOrNull(SemanticsProperties.EditableText)?.text.orEmpty()
        return (described + written + typed).joinToString(" ").trim()
    }

    private fun SemanticsNode.describe(): String = "${name().ifBlank { "(no name)" }.take(NAME_CHARS)} at ${boundsInRoot.left.toInt()},${boundsInRoot.top.toInt()}"

    /** Every control with a click action has a name a screen reader can say. */
    fun unnamedActions(): List<String> = actions().filter { it.name().isBlank() }.map { "an action has no name, at ${it.boundsInRoot.left.toInt()},${it.boundsInRoot.top.toInt()}" }

    /**
     * No two controls' touch areas overlap by more than a few dp in both directions, so a finger meant for one cannot land on the other.
     * (The 48dp minimum itself is not checked: Compose widens `touchBoundsInRoot` to 48dp for every clickable, so that check cannot fail;
     * a 24dp control passed it in this test's own first draft.)
     */
    fun crowdedTargets(): List<String> {
        val nodes = actions()
        val faults = mutableListOf<String>()
        for (i in nodes.indices) {
            for (j in i + 1 until nodes.size) {
                val a = nodes[i].touchBoundsInRoot
                val b = nodes[j].touchBoundsInRoot
                val dp = nodes[i].layoutInfo.density.density
                val across = minOf(a.right, b.right) - maxOf(a.left, b.left)
                val down = minOf(a.bottom, b.bottom) - maxOf(a.top, b.top)
                if (across > OVERLAP_DP * dp && down > OVERLAP_DP * dp) faults += "${nodes[i].describe()} and ${nodes[j].describe()} overlap by ${(across / dp).toInt()}x${(down / dp).toInt()}dp"
            }
        }
        return faults
    }

    /** At a large font: no text runs past the screen's right edge and no control's label is cut short with an ellipsis. */
    fun largeFontFaults(): List<String> {
        val rootWidth = rule.onRoot().fetchSemanticsNode().size.width
        val faults = mutableListOf<String>()
        val texts = rule.onAllNodes(SemanticsMatcher("has a text layout") { it.config.contains(SemanticsActions.GetTextLayoutResult) }, useUnmergedTree = true)
        for (node in texts.fetchSemanticsNodes()) {
            val right = node.positionInRoot.x + node.size.width
            if (right > rootWidth + TOLERANCE_PX) faults += "${node.describe()} ends at ${right.toInt()} of $rootWidth"
            val layouts = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            val layout = layouts.firstOrNull() ?: continue
            val inAnAction = generateSequence(node) { it.parent }.any { it.config.contains(SemanticsActions.OnClick) }
            if (inAnAction && (0 until layout.lineCount).any { layout.isLineEllipsized(it) }) faults += "${node.describe()} is cut short"
        }
        return faults
    }

    private companion object {
        const val OVERLAP_DP = 4f
        const val TOLERANCE_PX = 1f
        const val NAME_CHARS = 40
    }
}
