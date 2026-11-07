package com.payslipmax.pdfparser.parser

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val MARKER = "SYNTHETICNAME"

/**
 * WHY: every token of a payslip (name, account number, amounts) used to be printed to standard output, which reaches logcat on a release
 * build on every import. Crash telemetry is meant to be the only thing that leaves the parser, and it is sanitised; a stdout dump is
 * not. This builds a real PDF carrying a recognisable marker, runs the production extractor, and fails if any byte reaches stdout.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AndroidTokenExtractorOutputTest {
    private fun pdfWith(text: String): PDDocument {
        PDFBoxResourceLoader.init(ApplicationProvider.getApplicationContext<Context>())
        val document = PDDocument()
        val page = PDPage()
        document.addPage(page)
        PDPageContentStream(document, page).use { content ->
            content.beginText()
            content.setFont(PDType1Font.HELVETICA, 12f)
            content.newLineAtOffset(50f, 700f)
            content.showText(text)
            content.endText()
        }
        return document
    }

    @Test
    fun `extracting tokens writes nothing to standard output`() {
        val captured = ByteArrayOutputStream()
        val original = System.out
        val tokenized: TokenizedPayslip
        pdfWith("$MARKER Basic Pay 12345").use { document ->
            System.setOut(PrintStream(captured, true))
            try {
                tokenized = extractTokenized(document)
            } finally {
                System.setOut(original)
            }
        }

        assertTrue(tokenized.tableTokens.any { it.text.contains(MARKER) }, "the PDF must really be read, or this test proves nothing")
        assertEquals("", captured.toString(), "token text must never reach standard output (logcat)")
    }
}
