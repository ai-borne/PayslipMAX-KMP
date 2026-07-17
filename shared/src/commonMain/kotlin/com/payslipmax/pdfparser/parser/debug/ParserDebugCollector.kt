package com.payslipmax.pdfparser.parser.debug

import com.payslipmax.pdfparser.parser.Grid
import com.payslipmax.pdfparser.parser.PositionedToken
import com.payslipmax.pdfparser.parser.TokenizedPayslip

/** Passive collector accumulating debug data across stages 1 to 5. */
class ParserDebugCollector(
    val platform: String = "Unknown",
    val filename: String = "payslip.pdf",
) {
    var stage1: Stage1TokenDump? = null
    var stage2: Stage2RowDump? = null
    var stage3: Stage3ColumnDump? = null
    val stage4Fields = mutableListOf<FieldClassificationDump>()
    var stage5: Stage5ReconciliationDump? = null

    fun recordStage1(tokenized: TokenizedPayslip) {
        val tableDumps = tokenized.tableTokens.map { it.toDump("table") }
        val taxDumps = tokenized.taxTokens.map { it.toDump("tax") }
        val dsopDumps = tokenized.dsopTokens.map { it.toDump("dsop") }
        stage1 = Stage1TokenDump(tableDumps, taxDumps, dsopDumps)
    }

    fun recordStage2(grid: Grid) {
        val rowDumps =
            grid.rows.mapIndexed { idx, row ->
                val cellDumps =
                    row.cells.map { cell ->
                        CellDump(
                            text = cell.text,
                            left = cell.left,
                            right = cell.right,
                            centerY = cell.centerY,
                            tokens = cell.tokens.map { it.text },
                        )
                    }
                RowDump(rowIndex = idx, centerY = row.centerY, cells = cellDumps)
            }
        stage2 = Stage2RowDump(rowDumps)
    }

    fun recordStage4Field(field: FieldClassificationDump) {
        stage4Fields.add(field)
    }

    fun buildArtifact(): ParserDebugArtifact {
        val s1 = stage1 ?: Stage1TokenDump(emptyList(), emptyList(), emptyList())
        val s2 = stage2 ?: Stage2RowDump(emptyList())
        val s3 = stage3 ?: Stage3ColumnDump(null, null, 0f, emptyList(), emptyList())
        val s4 = Stage4ClassificationDump(stage4Fields.toList())
        val s5 = stage5 ?: Stage5ReconciliationDump(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false, emptyList(), emptyMap())
        return ParserDebugArtifact(platform, filename, s1, s2, s3, s4, s5)
    }

    private fun PositionedToken.toDump(page: String): PositionedTokenDump {
        return PositionedTokenDump(
            text = text,
            x = x,
            y = y,
            width = width,
            height = height,
            page = page,
            fontSize = fontSize,
            isBold = isBold,
        )
    }
}
