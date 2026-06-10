package com.ssbmax.pdfparser.parser

import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.IOException

class LayoutScanner : PDFTextStripper() {
    var bpayY: Float = 250f
    var dsopX: Float = 150f
    var totalCreditY: Float = 700f
    var detailsX: Float = 0f

    init {
        sortByPosition = true
    }

    @Throws(IOException::class)
    override fun writeString(
        text: String?,
        textPositions: MutableList<TextPosition>?,
    ) {
        super.writeString(text, textPositions)
        if (text == null || textPositions == null || textPositions.isEmpty()) return

        val lineText = text.trim()
        val lowerText = lineText.lowercase()

        // Locate BPAY / Basic Pay Y coordinate
        if (lowerText.contains("bpay") || lowerText.contains("basic pay")) {
            bpayY = textPositions.first().yDirAdj
        }

        // Locate DSOP / AGIF / ITAX X coordinate
        if (lowerText.contains("dsop") || lowerText.contains("agif") || lowerText.contains("itax")) {
            val idx =
                lowerText.indexOf("dsop").takeIf { it >= 0 }
                    ?: lowerText.indexOf("agif").takeIf { it >= 0 }
                    ?: lowerText.indexOf("itax").takeIf { it >= 0 }
                    ?: 0
            if (idx < textPositions.size) {
                val charX = textPositions[idx].xDirAdj
                if (dsopX == 150f || charX < dsopX) {
                    dsopX = charX
                }
            }
        }

        // Locate DETAILS OF TRANSACTIONS / DETAILS OF DO2s / DETAILS OF / laona dona / loona dona
        if (lowerText.contains("details of transactions") || lowerText.contains("details of do2s") ||
            lowerText.contains("details of") || lowerText.contains("laona dona") || lowerText.contains("loona dona")
        ) {
            val idx =
                lowerText.indexOf("details of transactions").takeIf { it >= 0 }
                    ?: lowerText.indexOf("details of do2s").takeIf { it >= 0 }
                    ?: lowerText.indexOf("details of").takeIf { it >= 0 }
                    ?: lowerText.indexOf("laona dona").takeIf { it >= 0 }
                    ?: lowerText.indexOf("loona dona").takeIf { it >= 0 }
                    ?: 0
            if (idx < textPositions.size) {
                val charX = textPositions[idx].xDirAdj
                if (detailsX == 0f || charX < detailsX) {
                    detailsX = charX
                }
            }
        }

        // Locate Total Credit / Gross Pay / Total Debit Y coordinate
        if (lowerText.contains("total credit") || lowerText.contains("gross pay") ||
            lowerText.contains("total debit") || lowerText.contains("total deductions")
        ) {
            totalCreditY = textPositions.first().yDirAdj
        }
    }
}
