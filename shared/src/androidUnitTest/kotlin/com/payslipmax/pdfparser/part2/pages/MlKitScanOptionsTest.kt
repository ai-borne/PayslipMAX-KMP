package com.payslipmax.pdfparser.part2.pages

import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * WHY: what the officer may do in the system scanner is decided by its options, and each one is a privacy or storage choice: no import
 * from the gallery (DECISIONS Round 10), photos only (no PDF the scanner would write somewhere else), and no more pages than the order has
 * room for. The options object has no readable fields, so each is checked by equality with the options it must be.
 */
class MlKitScanOptionsTest {
    private fun expected(
        pages: Int,
        gallery: Boolean = false,
        formats: Int = GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
        mode: Int = GmsDocumentScannerOptions.SCANNER_MODE_FULL,
    ) = GmsDocumentScannerOptions.Builder().setGalleryImportAllowed(gallery).setPageLimit(pages).setResultFormats(formats).setScannerMode(mode).build()

    @Test
    fun theScannerAcceptsNoGalleryImportAndOnlyJpegPagesUpToTheRoomLeft() {
        assertEquals(expected(7), MlKitScanSource.options(7))
        assertEquals(expected(1), MlKitScanSource.options(1))
    }

    @Test
    fun eachChoiceMattersToTheOptions() {
        val options = MlKitScanSource.options(7)

        assertNotEquals(expected(7, gallery = true), options, "gallery import must stay off")
        assertNotEquals(expected(7, formats = GmsDocumentScannerOptions.RESULT_FORMAT_PDF), options, "no PDF")
        assertNotEquals(expected(8), options, "the page limit follows the room left")
    }

    @Test
    fun theOutputFolderIsTheOneSeenOnTheDevice() {
        assertEquals("mlkit_docscan_ui_client", MlKitScanSource.OUTPUT_DIR, "seen on the Pixel 9 with 16.0.0: <cache>/mlkit_docscan_ui_client/<n>.jpg; a changed folder makes every scan fail loudly")
    }
}
