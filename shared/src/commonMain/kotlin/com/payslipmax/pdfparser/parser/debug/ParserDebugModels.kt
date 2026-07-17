package com.payslipmax.pdfparser.parser.debug

import com.payslipmax.pdfparser.parser.TableSide

/** Raw PositionedToken dump for Stage 1. */
data class PositionedTokenDump(
    val text: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val page: String,
    val fontSize: Float,
    val isBold: Boolean,
)

data class Stage1TokenDump(
    val tableTokens: List<PositionedTokenDump>,
    val taxTokens: List<PositionedTokenDump>,
    val dsopTokens: List<PositionedTokenDump>,
)

/** Reconstructed GridCell and GridRow dumps for Stage 2. */
data class CellDump(
    val text: String,
    val left: Float,
    val right: Float,
    val centerY: Float,
    val tokens: List<String>,
)

data class RowDump(
    val rowIndex: Int,
    val centerY: Float,
    val cells: List<CellDump>,
)

data class Stage2RowDump(
    val rows: List<RowDump>,
)

/** Column band and assignment dumps for Stage 3. */
data class EntryAssignmentDump(
    val rawLabel: String,
    val amount: Double,
    val side: TableSide,
    val labelCenterX: Float,
    val centerY: Float,
    val isCleanMatch: Boolean,
    val matchedKey: String?,
)

data class Stage3ColumnDump(
    val creditBand: Float?,
    val debitBand: Float?,
    val acceptRadius: Float,
    val assignedEntries: List<EntryAssignmentDump>,
    val droppedEntries: List<EntryAssignmentDump>,
)

/** Field classification recognition vs rejection dumps for Stage 4. */
enum class FieldStatus { RECOGNIZED, REJECTED }

data class FieldClassificationDump(
    val fieldKeyOrLabel: String,
    val amount: Double,
    val status: FieldStatus,
    val reason: String,
    val side: TableSide?,
    val sourceTokens: String,
)

data class Stage4ClassificationDump(
    val fields: List<FieldClassificationDump>,
)

/** Reconciliation solver metric and review decision dumps for Stage 5. */
data class Stage5ReconciliationDump(
    val grossPay: Double,
    val totalDeductions: Double,
    val netRemittance: Double,
    val earningsSum: Double,
    val deductionsSum: Double,
    val netResidual: Double,
    val needsReview: Boolean,
    val reviewReasons: List<String>,
    val fieldConfidence: Map<String, Float>,
)

/** Container holding full pipeline debug state for a PDF parse. */
data class ParserDebugArtifact(
    val platform: String,
    val filename: String,
    val stage1: Stage1TokenDump,
    val stage2: Stage2RowDump,
    val stage3: Stage3ColumnDump,
    val stage4: Stage4ClassificationDump,
    val stage5: Stage5ReconciliationDump,
)
