package com.payslipmax.pdfparser.ui.screens.guide.part2.due

import androidx.compose.foundation.lazy.LazyListScope
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2TileView
import com.payslipmax.pdfparser.ui.theme.Part2DueStrings

/** What the Part II home shows of the Due list: [count] things due now, and where a tap goes. */
internal class Part2DueRow(
    val count: Int,
    val onOpen: () -> Unit,
)

/** The "Due" row on the Part II home: nothing while [row] is null (not entitled, or nothing planned). */
internal fun LazyListScope.part2DueItems(row: Part2DueRow?) {
    if (row == null) return
    item(key = "due-row") { Part2TileView(Part2DueStrings.rowTitle, if (row.count == 0) Part2DueStrings.rowNothingDue else Part2DueStrings.rowDue(row.count), row.onOpen) }
}
