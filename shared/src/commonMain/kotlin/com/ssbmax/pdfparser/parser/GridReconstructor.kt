package com.ssbmax.pdfparser.parser

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
    /** Fallback used when token heights are unavailable; mirrors the legacy ±3f y-clustering. */
    private const val MIN_TOLERANCE = 3f

    fun reconstruct(
        tokens: List<PositionedToken>,
        debugCollector: com.ssbmax.pdfparser.parser.debug.ParserDebugCollector? = null,
    ): Grid {
        val usable = tokens.filter { it.text.isNotBlank() }
        if (usable.isEmpty()) return Grid(emptyList())

        val pageTablesIr = com.ssbmax.pdfparser.engine.TableReconstructionEngine.reconstructPage(usable, pageIndex = 0)
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

        println("=== STAGE 2: ROW RECONSTRUCTION (ENGINE DELEGATE) ===")
        println("Reconstructed grid confidence: ${tableIr.confidenceScore}")
        grid.rows.forEachIndexed { idx, row ->
            val rowStr = row.cells.joinToString(" | ") { it.text }
            println("ROW $idx\n$rowStr")
        }
        println("=====================================================")

        return grid
    }

    /** Groups tokens whose vertical centers fall within [tolerance] of the row's running center. */
    private fun clusterRows(
        tokens: List<PositionedToken>,
        tolerance: Float,
    ): List<List<PositionedToken>> {
        val sorted = tokens.sortedBy { it.centerY }
        val rows = mutableListOf<MutableList<PositionedToken>>()
        var refCenter = Float.NaN
        for (token in sorted) {
            if (rows.isEmpty() || token.centerY - refCenter > tolerance) {
                rows.add(mutableListOf(token))
                refCenter = token.centerY
            } else {
                rows.last().add(token)
            }
        }
        return rows
    }

    /** Within one row, joins tokens into cells, breaking when the horizontal gap exceeds [cellGap]. */
    private fun buildRow(
        rowTokens: List<PositionedToken>,
        cellGap: Float,
    ): GridRow {
        val sorted = rowTokens.sortedBy { it.x }
        val cells = mutableListOf<MutableList<PositionedToken>>()
        var prevRight = Float.NaN
        var prevToken: PositionedToken? = null
        for (token in sorted) {
            if (prevToken != null) {
                val gap = token.x - prevToken.right
                println("GAP: ${prevToken.text} -> ${token.text} = $gap pt (cellGap=$cellGap pt)")
            }
            if (cells.isEmpty() || token.x - prevRight > cellGap) {
                cells.add(mutableListOf(token))
            } else {
                cells.last().add(token)
            }
            prevRight = maxOf(if (prevRight.isNaN()) token.right else prevRight, token.right)
            prevToken = token
        }
        return GridRow(cells.map { GridCell(it) })
    }

    private fun medianOf(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2f
    }
}
