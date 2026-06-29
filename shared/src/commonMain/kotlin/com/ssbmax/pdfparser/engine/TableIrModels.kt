package com.ssbmax.pdfparser.engine

import com.ssbmax.pdfparser.parser.PositionedToken

/** Generic semantic classification categories for tokens in structured documents. */
enum class SemanticTokenType {
    Amount,
    Label,
    MixedCode,
    Header,
    Footer,
    Metadata,
    Unknown,
}

/** Simple bounding rectangle representation for engine calculations. */
data class RectF(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

/** Immutable token representation within the reconstruction engine hierarchy. */
data class IrToken(
    val id: String,
    val text: String,
    val pageIndex: Int,
    val rowIndex: Int,
    val columnIndex: Int,
    val boundingBox: RectF,
    val semanticType: SemanticTokenType,
    val confidenceScore: Float,
    val sourceToken: PositionedToken,
)

/** Immutable cell representation belonging to a single (RowIndex, ColumnIndex). */
data class IrCell(
    val rowIndex: Int,
    val columnIndex: Int,
    val tokens: List<IrToken>,
    val text: String,
    val boundingBox: RectF,
    val primaryType: SemanticTokenType,
    val confidenceScore: Float,
)

/** Immutable 2D grid representation for a single detected table. */
data class TableGridIR(
    val tableId: String,
    val pageIndex: Int,
    val columnCount: Int,
    val rows: List<List<IrCell>>,
    val confidenceScore: Float,
)

/** Immutable container holding all tables detected on a single page. */
data class PageTablesIR(
    val pageIndex: Int,
    val tables: List<TableGridIR>,
)
