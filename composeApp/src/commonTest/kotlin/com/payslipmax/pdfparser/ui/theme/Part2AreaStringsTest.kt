package com.payslipmax.pdfparser.ui.theme

import com.payslipmax.pdfparser.part2.domain.AreaRules
import com.payslipmax.pdfparser.part2.domain.LongLeaveRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: the officer picks their own area code by these words, and a wrong pick drafts an order for a rate they do not draw. The five Transport
 * Allowance categories share one printed name in the manual (serial 3.53), so their option words come from here, taken from the manual's
 * note i, and must differ. The reissue question names the area allowances now too, because their rows also run to the old return day.
 */
class Part2AreaStringsTest {
    @Test
    fun everyAreaQuestionIsWordedAndAnUnknownOneIsNot() {
        val ids = AreaRules.questions.map { it.id }
        assertEquals(listOf("rh_code", "haa_code", "sccia_code", "field_code", "tran_code"), ids)
        for (id in ids) assertTrue(Part2Strings.hasQuestion(id), id)
        assertEquals("Which Transport Allowance category do you draw?", Part2Strings.question("tran_code"))
        assertTrue("HAA" in Part2Strings.question("haa_code") && "SCCIA" in Part2Strings.question("sccia_code") && "field" in Part2Strings.question("field_code").lowercase())
        assertEquals(ids.size, ids.map(Part2Strings::question).toSet().size, "no two questions share their words")
    }

    @Test
    fun theFiveTransportCategoriesHaveDistinctWordsFromTheManualsNoteOne() {
        val names = AreaRules.tranCodes.map { Part2AreaStrings.optionName(it) }
        assertEquals(
            listOf(
                "Category 1: lower rate",
                "Category 2: higher rate",
                "Category 3: with disability, lower rate",
                "Category 4: with disability, higher rate",
                "Category 5: Maj Gen and above",
            ),
            names,
        )
        assertEquals(5, names.toSet().size)
    }

    @Test
    fun otherAreaCodesTakeTheManualsOwnSerialNameAndSoHaveNoWordsHere() {
        for (code in AreaRules.haaCodes + AreaRules.scciaCodes + AreaRules.fieldCodes + AreaRules.rhCodes) assertNull(Part2AreaStrings.optionName(code), code)
    }

    @Test
    fun theReissueQuestionAndWarningNameTheAreaAllowancesBesideRationAndRh() {
        assertTrue("area allowance" in Part2LongLeaveStrings.question(LongLeaveRules.ANSWER_EARLIER_ALLOWANCES), Part2LongLeaveStrings.question(LongLeaveRules.ANSWER_EARLIER_ALLOWANCES))
        val warning = checkNotNull(Part2LongLeaveStrings.finding(LongLeaveRules.REISSUE_ALLOWANCES))
        assertTrue("area allowance" in warning && "cannot be amended" in warning && "notified afresh" in warning, warning)
    }
}
