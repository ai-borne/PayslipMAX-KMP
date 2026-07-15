package com.payslipmax.pdfparser.utils

import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.payslipmax.pdfparser.crypto.ContextHolder
import java.io.File
import java.io.FileOutputStream

actual fun sharePdf(
    fileName: String,
    title: String,
    bodyText: String,
) {
    val context = ContextHolder.context ?: return

    val titlePaint =
        Paint().apply {
            textSize = PdfLayoutSpec.TITLE_FONT_SIZE.toFloat()
            isFakeBoldText = true
            isAntiAlias = true
        }
    val bodyPaint =
        Paint().apply {
            textSize = PdfLayoutSpec.BODY_FONT_SIZE.toFloat()
            isAntiAlias = true
        }

    val document = PdfDocument()
    renderPages(document, PdfLetterFormatter.contentLines(title, bodyText), titlePaint, bodyPaint)

    val file = File(context.cacheDir, fileName)
    FileOutputStream(file).use { document.writeTo(it) }
    document.close()

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send =
        Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    val chooser =
        Intent.createChooser(send, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    context.startActivity(chooser)
}

private fun renderPages(
    document: PdfDocument,
    lines: List<PdfLine>,
    titlePaint: Paint,
    bodyPaint: Paint,
) {
    var pageNum = 1
    var page = document.startPage(pageInfo(pageNum))
    var canvas = page.canvas
    var y = PdfLayoutSpec.MARGIN + PdfLayoutSpec.LINE_HEIGHT

    for (line in lines) {
        val paint = if (line.isTitle) titlePaint else bodyPaint
        val wrapped = PdfLetterFormatter.wrapLine(line.text, PdfLayoutSpec.contentWidth.toDouble()) { paint.measureText(it).toDouble() }
        for (visual in wrapped) {
            if (y > PdfLayoutSpec.contentBottom) {
                document.finishPage(page)
                pageNum += 1
                page = document.startPage(pageInfo(pageNum))
                canvas = page.canvas
                y = PdfLayoutSpec.MARGIN + PdfLayoutSpec.LINE_HEIGHT
            }
            canvas.drawText(visual, PdfLayoutSpec.MARGIN.toFloat(), y.toFloat(), paint)
            y += PdfLayoutSpec.LINE_HEIGHT
        }
    }
    document.finishPage(page)
}

private fun pageInfo(pageNum: Int): PdfDocument.PageInfo =
    PdfDocument.PageInfo.Builder(PdfLayoutSpec.PAGE_WIDTH, PdfLayoutSpec.PAGE_HEIGHT, pageNum).create()
