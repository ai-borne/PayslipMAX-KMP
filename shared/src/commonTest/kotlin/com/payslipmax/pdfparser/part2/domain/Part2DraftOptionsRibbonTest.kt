package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.testing.SyntheticPart2Ribbons
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.CLAIM
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.EXTENSION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.NOTE
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.RATION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.REJOIN_ALL
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.STOP
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.UNGROUPED
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Which codes "Add another occurrence" offers on a slip (DECISIONS Round 22): the codes of the ribbon the slip's first code belongs to, so a
 * cancel slip offers the cancellation codes and a leave slip never offers them. Read from the layout, so a later batch needs no code here.
 */
class Part2DraftOptionsRibbonTest {
    private val index = Part2LookupIndex(SyntheticPart2Ribbons.twoRibbons)

    @Test
    fun withoutACodeEveryDraftableCodeIsOffered() {
        assertEquals(listOf(GRANT, EXTENSION, RATION, STOP, NOTE, REJOIN_ALL, CLAIM).sorted(), index.draftOptions().map { it.code }.sorted())
    }

    @Test
    fun aSlipStartedInOneRibbonOffersOnlyThatRibbonsCodes() {
        val first = index.draftOptions(sameRibbonAs = GRANT).map { it.code }
        assertEquals(false, CLAIM in first, "the claim lives in the second ribbon")
        assertEquals(true, GRANT in first && EXTENSION in first && REJOIN_ALL in first)
        assertEquals(listOf(CLAIM), index.draftOptions(sameRibbonAs = CLAIM).map { it.code })
    }

    @Test
    fun aCodeNoBuiltRibbonHomesOffersNothingSoItCannotPullInOtherCodes() {
        assertEquals(emptyList(), index.draftOptions(sameRibbonAs = "NOSUCHCODE"))
    }

    @Test
    fun aLookupOnlyCodeIsNotHomedAnywhereSoItOffersNothing() {
        // why: a lookup-only serial is never drafted; a slip cannot start there, so it must not borrow its ribbon's codes.
        assertEquals(emptyList(), index.draftOptions(sameRibbonAs = UNGROUPED))
    }
}
