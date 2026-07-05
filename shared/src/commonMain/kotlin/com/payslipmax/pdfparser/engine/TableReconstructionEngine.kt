package com.payslipmax.pdfparser.engine

import com.payslipmax.pdfparser.parser.PositionedToken

/** Main orchestrator for the generic PDF table reconstruction pipeline. */
object TableReconstructionEngine {
    fun reconstructPage(
        tokens: List<PositionedToken>,
        pageIndex: Int,
    ): PageTablesIR {
        val usable = tokens.filter { it.text.isNotBlank() }
        if (usable.isEmpty()) {
            return PageTablesIR(pageIndex, emptyList())
        }

        val rows = RowDetector.detectRows(usable)
        val bands = ColumnBoundaryDetector.discoverColumnBands(rows)
        val gridRows = TokenGridAssigner.assignTokensToCells(rows, bands, pageIndex)

        val gridConfidence = calculateGridConfidence(gridRows)
        val tableIr =
            TableGridIR(
                tableId = "table_p$pageIndex",
                pageIndex = pageIndex,
                columnCount = bands.size,
                rows = gridRows,
                confidenceScore = gridConfidence,
            )

        return PageTablesIR(pageIndex = pageIndex, tables = listOf(tableIr))
    }

    private fun calculateGridConfidence(rows: List<List<IrCell>>): Float {
        val allCells = rows.flatten()
        if (allCells.isEmpty()) return 0f
        val avgCellConf = allCells.map { it.confidenceScore }.average().toFloat()
        val rowConsistency = if (rows.size >= 2) 0.95f else 0.8f
        return (avgCellConf * 0.7f) + (rowConsistency * 0.3f)
    }
}
