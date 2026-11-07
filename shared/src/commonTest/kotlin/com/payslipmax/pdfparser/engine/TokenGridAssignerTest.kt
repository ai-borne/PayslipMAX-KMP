package com.payslipmax.pdfparser.engine

import com.payslipmax.pdfparser.parser.PositionedToken
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: [TokenGridAssigner] turns a row of loose tokens into the cells the table classifier reads. A label split across tokens must stay
 * one cell, but a label and its amount (or two amounts) must never fuse, or an amount is read as part of a name and pay goes missing.
 */
class TokenGridAssignerTest {
    private val twoBands = listOf(ColumnBand(0, 0f, 100f, 10f, 0.95f), ColumnBand(1, 100f, 10000f, 200f, 0.95f))

    private fun token(
        text: String,
        x: Float,
        width: Float = 20f,
    ) = PositionedToken(text, x, 0f, width, 10f)

    private fun assign(
        vararg tokens: PositionedToken,
        bands: List<ColumnBand> = twoBands,
    ): List<List<IrCell>> = TokenGridAssigner.assignTokensToCells(listOf(DetectedRow(3, tokens.toList(), 5f)), bands, pageIndex = 2)

    @Test
    fun wordsOfOneLabelInTheSameBandJoinIntoOneCellHoweverFarApart() {
        val cells = assign(token("Basic", 10f), token("Pay", 80f)).single()

        assertEquals(listOf("Basic Pay"), cells.map { it.text })
    }

    @Test
    fun aLabelAndAnAmountInTheSameBandStaySeparateCells() {
        val cells = assign(token("Rs", 110f), token("500", 125f)).single()

        assertEquals(listOf("Rs", "500"), cells.map { it.text })
    }

    @Test
    fun twoAmountsMoreThanFifteenPointsApartAreTwoCellsAndNoMoreThanFifteenFuse() {
        // the first token ends at x=130, so these gaps are 15 and 16 points.
        assertEquals(listOf("100 200"), assign(token("100", 110f), token("200", 145f)).single().map { it.text })
        assertEquals(listOf("100", "200"), assign(token("100", 110f), token("200", 146f)).single().map { it.text })
    }

    @Test
    fun cellsCarryTheirRowColumnPageAndStableIds() {
        val cells = assign(token("DA", 10f), token("1,000", 150f)).single()

        assertEquals(listOf(0, 1), cells.map { it.columnIndex })
        assertEquals(listOf(3, 3), cells.map { it.rowIndex })
        assertEquals("p2_r3_c1_t0", cells[1].tokens.single().id)
    }

    @Test
    fun aNegativeOrDecimalNumberIsAnAmountButCurrencyPrefixedTextIsNot() {
        // 5pt apart: only a type mismatch can keep them in separate cells.
        assertEquals(listOf("Rs.500", "1,000.50"), assign(token("Rs.500", 110f), token("1,000.50", 135f)).single().map { it.text })
        assertEquals(listOf("-5.00 7"), assign(token("-5.00", 110f), token("7", 135f)).single().map { it.text })
    }

    @Test
    fun aTokenOutsideEveryBandGoesToTheNearestAlignment() {
        val gapped = listOf(ColumnBand(0, 0f, 50f, 10f, 0.95f), ColumnBand(1, 100f, 200f, 150f, 0.95f))

        assertEquals(listOf(0), assign(token("A", 60f), bands = gapped).single().map { it.columnIndex })
        assertEquals(listOf(1), assign(token("B", 90f), bands = gapped).single().map { it.columnIndex })
    }

    @Test
    fun anEmptyRowProducesNoGridRow() {
        assertEquals(emptyList(), TokenGridAssigner.assignTokensToCells(listOf(DetectedRow(0, emptyList(), 0f)), twoBands, 0))
    }
}
