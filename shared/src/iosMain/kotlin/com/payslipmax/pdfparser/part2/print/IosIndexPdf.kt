package com.payslipmax.pdfparser.part2.print

import com.payslipmax.pdfparser.utils.IndexLayoutSpec
import com.payslipmax.pdfparser.utils.IndexLayoutSpec.Font
import com.payslipmax.pdfparser.utils.PdfLayoutSpec
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGContextAddLineToPoint
import platform.CoreGraphics.CGContextMoveToPoint
import platform.CoreGraphics.CGContextSetLineWidth
import platform.CoreGraphics.CGContextSetStrokeColorWithColor
import platform.CoreGraphics.CGContextStrokePath
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.UIKit.NSFontAttributeName
import platform.UIKit.NSForegroundColorAttributeName
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsPDFRenderer
import platform.UIKit.UIGraphicsPDFRendererContext
import platform.UIKit.drawAtPoint
import platform.UIKit.sizeWithAttributes

/** Draws laid-out index pages into PDF data in memory, in the sizes and inks the spec names, as Android's `IndexPdf` does with its own fonts. */
@OptIn(ExperimentalForeignApi::class)
internal object IosIndexPdf {
    private val attributes: Map<Font, Map<Any?, Any?>> =
        Font.entries.associateWith { font ->
            mapOf<Any?, Any?>(
                NSFontAttributeName to if (font.bold) UIFont.boldSystemFontOfSize(font.size.toDouble()) else UIFont.systemFontOfSize(font.size.toDouble()),
                NSForegroundColorAttributeName to color(font.ink),
            )
        }

    private fun color(argb: Long) =
        UIColor(
            red = ((argb shr 16) and 0xFF) / 255.0,
            green = ((argb shr 8) and 0xFF) / 255.0,
            blue = (argb and 0xFF) / 255.0,
            alpha = ((argb shr 24) and 0xFF) / 255.0,
        )

    /** The pages of [document] as UIKit's fonts lay them out. */
    fun layout(document: IndexDocument): List<IndexPage> =
        IndexLayout.paginate(document) { text, font -> (text as Any as NSString).sizeWithAttributes(attributes.getValue(font)).useContents { width } }

    /** The PDF of [document], or null when it has no pages (an empty Bank prints nothing). */
    fun render(document: IndexDocument): NSData? {
        val pages = layout(document).ifEmpty { return null }
        val bounds = CGRectMake(0.0, 0.0, PdfLayoutSpec.PAGE_WIDTH.toDouble(), PdfLayoutSpec.PAGE_HEIGHT.toDouble())
        return UIGraphicsPDFRenderer(bounds = bounds).PDFDataWithActions { context -> context?.let { ctx -> pages.forEach { draw(ctx, it) } } }
    }

    private fun draw(
        context: UIGraphicsPDFRendererContext,
        page: IndexPage,
    ) {
        context.beginPage()
        for (text in page.texts) (text.text as Any as NSString).drawAtPoint(CGPointMake(text.x.toDouble(), text.top.toDouble()), withAttributes = attributes.getValue(text.font))
        val canvas = context.CGContext
        CGContextSetStrokeColorWithColor(canvas, color(IndexLayoutSpec.RULE).CGColor)
        CGContextSetLineWidth(canvas, IndexLayoutSpec.RULE_WIDTH)
        for (rule in page.rules) {
            CGContextMoveToPoint(canvas, rule.x1.toDouble(), rule.y.toDouble())
            CGContextAddLineToPoint(canvas, rule.x2.toDouble(), rule.y.toDouble())
            CGContextStrokePath(canvas)
        }
    }
}
