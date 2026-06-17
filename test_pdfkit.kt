import platform.PDFKit.*
import platform.CoreGraphics.*

fun test(page: PDFPage) {
    val rect = CGRectMake(0.0, 0.0, 100.0, 100.0)
    val sel = page.selectionForRect(rect)
    val s: String? = sel?.string
}
