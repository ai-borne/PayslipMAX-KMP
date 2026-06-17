import platform.PDFKit.PDFPage
import kotlinx.cinterop.ExperimentalForeignApi
@OptIn(ExperimentalForeignApi::class)
fun test(page: PDFPage) {
    val rect = page.characterBoundsAtIndex(0)
}
