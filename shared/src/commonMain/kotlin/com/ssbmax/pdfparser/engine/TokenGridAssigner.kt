package com.ssbmax.pdfparser.engine

import com.ssbmax.pdfparser.parser.PositionedToken

/** Assigner that maps tokens to (RowIndex, ColumnIndex) coordinates based on inferred ColumnBands. */
object TokenGridAssigner {
    fun assignTokensToCells(
        rows: List<DetectedRow>,
        bands: List<ColumnBand>,
        pageIndex: Int,
    ): List<List<IrCell>> {
        val gridRows = mutableListOf<List<IrCell>>()

        for (row in rows) {
            val cellList = mutableListOf<IrCell>()
            val tokensByCol = mutableMapOf<Int, MutableList<PositionedToken>>()

            for (token in row.tokens) {
                val matchedBand = findBestBand(token, bands)
                tokensByCol.getOrPut(matchedBand.columnIndex) { mutableListOf() }.add(token)
            }

            for (band in bands) {
                val colTokens = tokensByCol[band.columnIndex] ?: emptyList()
                if (colTokens.isNotEmpty()) {
                    val subClusters = clusterColTokens(colTokens)
                    for (cluster in subClusters) {
                        cellList.add(buildIrCell(row.rowIndex, band.columnIndex, pageIndex, cluster))
                    }
                }
            }
            if (cellList.isNotEmpty()) {
                gridRows.add(cellList)
            }
        }
        return gridRows
    }

    private fun clusterColTokens(tokens: List<PositionedToken>): List<List<PositionedToken>> {
        if (tokens.isEmpty()) return emptyList()
        val sorted = tokens.sortedBy { it.x }

        val clusters = mutableListOf<MutableList<PositionedToken>>()
        for (token in sorted) {
            if (clusters.isEmpty()) {
                clusters.add(mutableListOf(token))
            } else {
                val lastCluster = clusters.last()
                val lastToken = lastCluster.last()
                val isTypeMismatch = isAmountToken(token.text) != isAmountToken(lastToken.text)
                val bothNonAmount = !isAmountToken(token.text) && !isAmountToken(lastToken.text)
                // If both are non-amount tokens in the same column band, combine them.
                val isGapExceeded = if (bothNonAmount) false else (token.x - lastToken.right) > 15f
                if (isGapExceeded || isTypeMismatch) {
                    clusters.add(mutableListOf(token))
                } else {
                    lastCluster.add(token)
                }
            }
        }
        return clusters
    }

    private fun isAmountToken(text: String): Boolean {
        val cleaned = text.replace(",", "").trim()
        return Regex("^-?\\d+(\\.\\d+)?$").matches(cleaned)
    }

    private fun findBestBand(
        token: PositionedToken,
        bands: List<ColumnBand>,
    ): ColumnBand {
        val posX = token.x
        return bands.find { posX >= it.minX && posX < it.maxX }
            ?: bands.minByOrNull { kotlin.math.abs(it.alignmentX - posX) }
            ?: bands.first()
    }

    private fun buildIrCell(
        rowIndex: Int,
        columnIndex: Int,
        pageIndex: Int,
        tokens: List<PositionedToken>,
    ): IrCell {
        val irTokens =
            tokens.mapIndexed { idx, t ->
                val (type, conf) = SemanticTokenClassifier.classify(t.text)
                IrToken(
                    id = "p${pageIndex}_r${rowIndex}_c${columnIndex}_t$idx",
                    text = t.text,
                    pageIndex = pageIndex,
                    rowIndex = rowIndex,
                    columnIndex = columnIndex,
                    boundingBox = RectF(t.x, t.y, t.right, t.y + t.height),
                    semanticType = type,
                    confidenceScore = conf,
                    sourceToken = t,
                )
            }

        val text = irTokens.joinToString(" ") { it.text }
        val minLeft = irTokens.minOf { it.boundingBox.left }
        val minTop = irTokens.minOf { it.boundingBox.top }
        val maxRight = irTokens.maxOf { it.boundingBox.right }
        val maxBottom = irTokens.maxOf { it.boundingBox.bottom }
        val primaryType = irTokens.firstOrNull()?.semanticType ?: SemanticTokenType.Unknown
        val avgConf = irTokens.map { it.confidenceScore }.average().toFloat()

        return IrCell(
            rowIndex = rowIndex,
            columnIndex = columnIndex,
            tokens = irTokens,
            text = text,
            boundingBox = RectF(minLeft, minTop, maxRight, maxBottom),
            primaryType = primaryType,
            confidenceScore = avgConf,
        )
    }
}
