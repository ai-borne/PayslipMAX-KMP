package com.payslipmax.pdfparser.engine

import com.payslipmax.pdfparser.parser.PositionedToken
import kotlin.test.Test
import kotlin.test.assertEquals

class EngineDetectionTest {
    @Test
    fun testRowDetector() {
        val t1 = PositionedToken("Row1Col1", x = 10f, y = 20f, width = 30f, height = 10f)
        val t2 = PositionedToken("Row1Col2", x = 100f, y = 21f, width = 30f, height = 10f)
        val t3 = PositionedToken("Row2Col1", x = 10f, y = 50f, width = 30f, height = 10f)

        val rows = RowDetector.detectRows(listOf(t1, t2, t3))
        assertEquals(2, rows.size)
        assertEquals(2, rows[0].tokens.size)
        assertEquals(1, rows[1].tokens.size)
    }

    @Test
    fun testColumnBoundaryDetector() {
        val t1 = PositionedToken("BPAY", x = 40f, y = 20f, width = 30f, height = 10f)
        val t2 = PositionedToken("144700", x = 140f, y = 20f, width = 30f, height = 10f)
        val t3 = PositionedToken("DA", x = 40f, y = 40f, width = 30f, height = 10f)
        val t4 = PositionedToken("84906", x = 140f, y = 40f, width = 30f, height = 10f)

        val rows = RowDetector.detectRows(listOf(t1, t2, t3, t4))
        val bands = ColumnBoundaryDetector.discoverColumnBands(rows)

        assertEquals(2, bands.size)
        assertEquals(0, bands[0].columnIndex)
        assertEquals(1, bands[1].columnIndex)
    }
}
