package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.guide.model.GuideCard
import com.payslipmax.pdfparser.testing.SyntheticGuideRuleChange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A replaced rule stays in the data because arrears are worked out under the old one. These tests pin the two reads the
 * app needs, "what is the rule now" and "what was it before", and that neither can loop on a damaged bundle.
 */
class GuideRuleHistoryTest {
    private val cards = SyntheticGuideRuleChange.bundle().cards
    private val history = GuideRuleHistory(cards)

    private fun chain(vararg replacedBy: Pair<String, String>): GuideRuleHistory {
        val template = cards.first { it.id == "RB-P1" }
        val ids = replacedBy.flatMap { listOf(it.first, it.second) }.distinct()
        val made: List<GuideCard> =
            ids.map { id -> template.copy(id = id, replacedBy = replacedBy.firstOrNull { it.first == id }?.second.orEmpty(), until = "2026-11-15") }
        return GuideRuleHistory(made)
    }

    @Test
    fun aReplacedCardKnowsItsSuccessorAndItsEndDate() {
        assertTrue(history.isReplaced(SyntheticGuideRuleChange.OLD_CARD))
        assertEquals(SyntheticGuideRuleChange.NEW_CARD, history.currentRule(SyntheticGuideRuleChange.OLD_CARD)?.id)
        assertEquals(SyntheticGuideRuleChange.EFFECTIVE, history.replacedOn(SyntheticGuideRuleChange.OLD_CARD))
    }

    @Test
    fun aCurrentCardIsItsOwnCurrentRuleAndHasNoReplacementDate() {
        assertFalse(history.isReplaced(SyntheticGuideRuleChange.NEW_CARD))
        assertEquals(SyntheticGuideRuleChange.NEW_CARD, history.currentRule(SyntheticGuideRuleChange.NEW_CARD)?.id)
        assertNull(history.replacedOn(SyntheticGuideRuleChange.NEW_CARD))
    }

    @Test
    fun theNewCardPointsBackAtTheRuleItReplaced() {
        assertEquals(listOf(SyntheticGuideRuleChange.OLD_CARD), history.earlierRules(SyntheticGuideRuleChange.NEW_CARD).map { it.id })
        assertTrue(history.earlierRules(SyntheticGuideRuleChange.OLD_CARD).isEmpty(), "nothing came before the oldest rule")
        assertEquals(SyntheticGuideRuleChange.EFFECTIVE, history.earlierBefore(SyntheticGuideRuleChange.NEW_CARD))
    }

    @Test
    fun aLongerChainWalksToTheEndInOneStepAndBackNearestFirst() {
        val three = chain("A" to "B", "B" to "C")

        assertEquals("C", three.currentRule("A")?.id, "See current rule goes straight to the rule in force")
        assertEquals(listOf("B", "A"), three.earlierRules("C").map { it.id }, "nearest earlier rule first")
    }

    @Test
    fun aReplacementLoopEndsInsteadOfSpinning() {
        val loop = chain("A" to "B", "B" to "A")

        assertNull(loop.currentRule("A"), "a loop has no rule in force; the link is simply not shown")
        assertEquals(listOf("B"), loop.earlierRules("A").map { it.id }, "the walk back stops when it reaches a card it has seen")
    }

    @Test
    fun onlyACardThatReachesALoopIsFlaggedAsLoopingNotOneThatEndsAtAMissingCard() {
        val loop = chain("A" to "B", "B" to "A")
        assertTrue(loop.loopsBack("A") && loop.loopsBack("B"))

        val ends = chain("A" to "B", "B" to "C")
        assertFalse(ends.loopsBack("A") || ends.loopsBack("C"))
        val dangling = GuideRuleHistory(listOf(cards.first { it.id == "RB-P1" }.copy(replacedBy = "RB-NOPE", until = "2026-11-15")))
        assertFalse(dangling.loopsBack("RB-P1"), "a missing target is reported by its own validator rule, not as a loop")
    }

    @Test
    fun aSelfReplacementAndADanglingTargetAreHandled() {
        val damaged = GuideRuleHistory(listOf(cards.first { it.id == "RB-P1" }.copy(replacedBy = "RB-P1", until = "2026-11-15")))
        assertNull(damaged.currentRule("RB-P1"))

        val dangling = GuideRuleHistory(listOf(cards.first { it.id == "RB-P1" }.copy(replacedBy = "RB-NOPE", until = "2026-11-15")))
        assertNull(dangling.currentRule("RB-P1"))
    }

    @Test
    fun anUnknownIdIsNotReplacedAndHasNoHistory() {
        assertFalse(history.isReplaced("RB-NOPE"))
        assertNull(history.currentRule("RB-NOPE"))
        assertTrue(history.earlierRules("RB-NOPE").isEmpty())
        assertNull(history.replacedOn("RB-NOPE"))
        assertNull(history.earlierBefore("RB-NOPE"))
    }

    @Test
    fun aBundleWithNoReplacedCardHasNoHistoryAnywhere() {
        val plain = GuideRuleHistory(cards.map { it.copy(replacedBy = "", until = "") })

        assertTrue(cards.map { it.id }.none { plain.isReplaced(it) || plain.earlierRules(it).isNotEmpty() })
    }
}
