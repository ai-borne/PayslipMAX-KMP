package com.payslipmax.pdfparser.parser

/**
 * A contiguous run of [PositionedToken]s on one visual line that belong together (a table "cell"),
 * e.g. a multi-word label like "Basic Pay" or a single amount like "140500".
 */
data class GridCell(
    val tokens: List<PositionedToken>,
) {
    /** Space-joined text of the constituent tokens, in left-to-right order. */
    val text: String get() = tokens.joinToString(" ") { it.text }
    val left: Float get() = tokens.first().x
    val right: Float get() = tokens.maxOf { it.right }
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = tokens.map { it.centerY }.average().toFloat()
    val isBold: Boolean get() = tokens.any { it.isBold }
    val maxFontSize: Float get() = tokens.maxOfOrNull { it.fontSize } ?: 0f
}

/** One reconstructed visual row of the table: its cells, left-to-right. */
data class GridRow(
    val cells: List<GridCell>,
) {
    val centerY: Float get() = cells.flatMap { it.tokens }.map { it.centerY }.average().toFloat()
}

/** The reconstructed 2D table grid for a single page of [PositionedToken]s. */
data class Grid(
    val rows: List<GridRow>,
)

/**
 * Phase 3 — reconstructs a 2D grid from a flat, unordered list of [PositionedToken]s **without any
 * hardcoded geometry** (no `xSplit`, no fixed table bounds). Clustering tolerances are derived
 * per-document from the tokens' own median height, so the same engine adapts across pay commissions
 * and years instead of needing per-month geometry patches.
 *
 * Pipeline: tokens → cluster by y into rows → within each row, cluster by x into cells. Downstream,
 * [RowPairing] turns rows into (label, amount) pairs and [TokenTableClassifier] assigns credit/debit.
 */
object GridReconstructor {
    fun reconstruct(
        tokens: List<PositionedToken>,
        debugCollector: com.payslipmax.pdfparser.parser.debug.ParserDebugCollector? = null,
    ): Grid {
        val usable = tokens.filter { it.text.isNotBlank() }
        if (usable.isEmpty()) return Grid(emptyList())

        val pageTablesIr = com.payslipmax.pdfparser.engine.TableReconstructionEngine.reconstructPage(usable, pageIndex = 0)
        val tableIr = pageTablesIr.tables.firstOrNull() ?: return Grid(emptyList())

        val gridRows =
            tableIr.rows.map { irRow ->
                val cells =
                    irRow.map { irCell ->
                        GridCell(tokens = irCell.tokens.map { it.sourceToken })
                    }
                GridRow(cells = cells)
            }
        val grid = Grid(gridRows)
        debugCollector?.recordStage2(grid)

        return grid
    }
}
