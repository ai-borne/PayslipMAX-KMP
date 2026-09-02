package com.payslipmax.pdfparser.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.writeToFile
import platform.UIKit.NSFontAttributeName
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsPDFRenderer
import platform.UIKit.UIGraphicsPDFRendererContext
import platform.UIKit.drawAtPoint
import platform.UIKit.sizeWithAttributes

@OptIn(ExperimentalForeignApi::class)
actual fun sharePdf(
    fileName: String,
    title: String,
    bodyText: String,
) {
    val titleAttrs = mapOf<Any?, Any?>(NSFontAttributeName to UIFont.boldSystemFontOfSize(PdfLayoutSpec.TITLE_FONT_SIZE.toDouble()))
    val bodyAttrs = mapOf<Any?, Any?>(NSFontAttributeName to UIFont.systemFontOfSize(PdfLayoutSpec.BODY_FONT_SIZE.toDouble()))

    val bounds = CGRectMake(0.0, 0.0, PdfLayoutSpec.PAGE_WIDTH.toDouble(), PdfLayoutSpec.PAGE_HEIGHT.toDouble())
    val data =
        UIGraphicsPDFRenderer(bounds = bounds).PDFDataWithActions { ctx ->
            renderPages(ctx, PdfLetterFormatter.contentLines(title, bodyText), titleAttrs, bodyAttrs)
        }

    val path = NSTemporaryDirectory() + fileName
    data.writeToFile(path, atomically = true)
    presentIosShare(listOf(NSURL.fileURLWithPath(path)))
}

@OptIn(ExperimentalForeignApi::class)
private fun renderPages(
    ctx: UIGraphicsPDFRendererContext?,
    lines: List<PdfLine>,
    titleAttrs: Map<Any?, Any?>,
    bodyAttrs: Map<Any?, Any?>,
) {
    ctx ?: return
    ctx.beginPage()
    var y = PdfLayoutSpec.MARGIN.toDouble()
    for (line in lines) {
        val attrs = if (line.isTitle) titleAttrs else bodyAttrs
        val wrapped = PdfLetterFormatter.wrapLine(line.text, PdfLayoutSpec.contentWidth.toDouble()) { measureWidth(it, attrs) }
        for (visual in wrapped) {
            if (y + PdfLayoutSpec.LINE_HEIGHT > PdfLayoutSpec.contentBottom) {
                ctx.beginPage()
                y = PdfLayoutSpec.MARGIN.toDouble()
            }
            (visual as NSString).drawAtPoint(CGPointMake(PdfLayoutSpec.MARGIN.toDouble(), y), withAttributes = attrs)
            y += PdfLayoutSpec.LINE_HEIGHT
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun measureWidth(
    text: String,
    attrs: Map<Any?, Any?>,
): Double = (text as NSString).sizeWithAttributes(attrs).useContents { width }
