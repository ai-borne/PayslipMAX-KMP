package com.payslipmax.pdfparser.guide.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Search finds rules by number ("177", "177B", "Rule 114"), so reading a number wrongly either hides a card the
 * user needs or shows a wrong one. A cite also holds dates, letter numbers and amounts that are not rules.
 */
class GuideRuleNumberParserTest {
    @Test
    fun wordsIgnoreCaseAndPunctuationAndJoinApostrophes() {
        assertEquals(listOf("familys", "ltc", "journey", "warrant", "or", "fare"), GuideRuleNumberParser.words("Family's LTC journey: warrant, or FARE?"))
        assertEquals(listOf("familys"), GuideRuleNumberParser.words("Family’s"))
        assertEquals(emptyList(), GuideRuleNumberParser.words(" ?! "))
    }

    @Test
    fun aCitesRuleNumbersAreTheOnesAfterRuleOrRules() {
        assertEquals(setOf("177b"), GuideRuleNumberParser.ruleNumbers("Rule 177B(i)(g), TR 2014"))
        assertEquals(setOf("85a"), GuideRuleNumberParser.ruleNumbers("Rule 85A, TR 2014"))
        assertEquals(setOf("82", "67"), GuideRuleNumberParser.ruleNumbers("Rule 82(a) and Rule 67(c)(iii), TR 2014"))
        assertEquals(setOf("2"), GuideRuleNumberParser.ruleNumbers("Rule 2 and Note 2 below 'Duty', TR 2014"))
    }

    @Test
    fun aListOfRulesGivesEachNumber() {
        val cite = "Rules 94, 95 and 100 to 101, Pay and Allowances Regulations for the Army"
        assertEquals(setOf("94", "95", "100", "101"), GuideRuleNumberParser.ruleNumbers(cite))
    }

    @Test
    fun aRangeOfRulesGivesEveryRuleInIt() {
        // RB-SS-P034 cites "Rules 88 to 91"; a person looking for rule 89 must find it.
        assertEquals(setOf("88", "89", "90", "91"), GuideRuleNumberParser.ruleNumbers("Rules 88 to 91, Pay and Allowances Regulations"))
        assertEquals(setOf("94", "95", "100", "101"), GuideRuleNumberParser.ruleNumbers("Rules 94, 95 and 100 to 101"))
    }

    @Test
    fun aRangeIsNotExpandedWhenItIsAbsurdlyWideOrLettered() {
        assertEquals(setOf("5", "400"), GuideRuleNumberParser.ruleNumbers("Rule 5 to 400"))
        assertEquals(setOf("177a", "177c"), GuideRuleNumberParser.ruleNumbers("Rules 177A to 177C"))
        assertEquals(setOf("10", "12"), GuideRuleNumberParser.ruleNumbers("Rules 10 and 12"), "and is a list, not a range")
    }

    @Test
    fun datesLetterNumbersAndAmountsInACiteAreNeverRules() {
        val cite = "MoD letter 1(16)/2017/D(Pay/Services) dt 18-09-2017; CGDA letter AT/I/0/42/VLL dt 26-02-1991"
        assertEquals(emptySet(), GuideRuleNumberParser.ruleNumbers(cite))
    }

    @Test
    fun aRuleNumberIsDigitsWithAtMostTwoLetters() {
        assertTrue(GuideRuleNumberParser.isRuleNumber("114"))
        assertTrue(GuideRuleNumberParser.isRuleNumber("177b"))
        assertFalse(GuideRuleNumberParser.isRuleNumber("tr"))
        assertFalse(GuideRuleNumberParser.isRuleNumber("177bcd"))
        assertFalse(GuideRuleNumberParser.isRuleNumber(""))
    }

    @Test
    fun aNumberQueryMatchesWholeNumbersNeverAPrefixOfALongerOne() {
        assertFalse(GuideRuleNumberParser.numberMatches("177", "1770"))
        assertFalse(GuideRuleNumberParser.numberMatches("17", "177"))
        assertTrue(GuideRuleNumberParser.numberMatches("177", "177"))
    }

    @Test
    fun aBareNumberFindsItsLetteredRulesButALetteredQueryFindsOnlyItself() {
        assertTrue(GuideRuleNumberParser.numberMatches("177", "177b"))
        assertFalse(GuideRuleNumberParser.numberMatches("177", "177bb"))
        assertTrue(GuideRuleNumberParser.numberMatches("177b", "177b"))
        assertFalse(GuideRuleNumberParser.numberMatches("177b", "177"))
        assertFalse(GuideRuleNumberParser.numberMatches("177b", "177c"))
    }
}
