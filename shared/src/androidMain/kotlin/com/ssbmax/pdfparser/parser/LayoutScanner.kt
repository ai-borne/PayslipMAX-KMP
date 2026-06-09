package com.ssbmax.pdfparser.parser

import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.IOException

class LayoutScanner : PDFTextStripper() {
    var bpayY: Float = 250f
    var dsopX: Float = 150f
    var totalCreditY: Float = 700f

    init {
        sortByPosition = true
    }

    @Throws(IOException::class)
    override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
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
            val idx = lowerText.indexOf("dsop").takeIf { it >= 0 }
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

        // Locate Total Credit / Gross Pay / Total Debit Y coordinate
        if (lowerText.contains("total credit") || lowerText.contains("gross pay") ||
            lowerText.contains("total debit") || lowerText.contains("total deductions")) {
            totalCreditY = textPositions.first().yDirAdj
        }
    }
}
