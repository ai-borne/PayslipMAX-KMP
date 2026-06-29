package com.ssbmax.pdfparser.engine

import com.ssbmax.pdfparser.parser.PositionedToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TableReconstructionEngineTest {
    @Test
    fun testAdjacentTokenBoundarySeparation() {
        // Simulating iOS tokens for Row 8 where 144700 and DSOP have a gap of 9.29 pt
        val t1 = PositionedToken("BPAY", x = 43.65f, y = 253.3f, width = 26.68f, height = 10f)
        val t2 = PositionedToken("144700", x = 143.45f, y = 253.3f, width = 33.36f, height = 10f)
        val t3 = PositionedToken("DSOP", x = 186.10f, y = 253.3f, width = 28.34f, height = 10f) // gap = 9.29 pt
        val t4 = PositionedToken("40000", x = 275.46f, y = 253.3f, width = 27.80f, height = 10f)

        // Add a second row to form alignment peaks for column bands
        val r2t1 = PositionedToken("DA", x = 43.65f, y = 273.3f, width = 13.89f, height = 10f)
        val r2t2 = PositionedToken("84906", x = 149.01f, y = 273.3f, width = 27.80f, height = 10f)
        val r2t3 = PositionedToken("AGIF", x = 186.10f, y = 273.3f, width = 23.34f, height = 10f)
        val r2t4 = PositionedToken("10000", x = 275.46f, y = 273.3f, width = 27.80f, height = 10f)

        val tokens = listOf(t1, t2, t3, t4, r2t1, r2t2, r2t3, r2t4)
        val pageIr = TableReconstructionEngine.reconstructPage(tokens, pageIndex = 0)

        assertEquals(1, pageIr.tables.size)
        val table = pageIr.tables.first()
        assertTrue(table.confidenceScore > 0.8f)

        val row0 = table.rows[0]
        // Verify that 144700 and DSOP are strictly separated into separate cells by column identity!
        assertEquals(4, row0.size)
        assertEquals("BPAY", row0[0].text)
        assertEquals("144700", row0[1].text)
        assertEquals("DSOP", row0[2].text)
        assertEquals("40000", row0[3].text)

        // Verify semantic typing
        assertEquals(SemanticTokenType.Label, row0[0].primaryType)
        assertEquals(SemanticTokenType.Amount, row0[1].primaryType)
        assertEquals(SemanticTokenType.Label, row0[2].primaryType)
        assertEquals(SemanticTokenType.Amount, row0[3].primaryType)
    }
}
