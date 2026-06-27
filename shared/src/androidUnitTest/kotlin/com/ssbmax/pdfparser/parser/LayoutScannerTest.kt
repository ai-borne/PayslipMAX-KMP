package com.ssbmax.pdfparser.parser

import com.tom_roush.pdfbox.text.TextPosition
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

private class TestableLayoutScanner : LayoutScanner() {
    fun simulateText(text: String, yAdj: Float, xAdj: Float = 10f) {
        val mockPos = mockk<TextPosition>(relaxed = true) {
            every { yDirAdj } returns yAdj
            every { xDirAdj } returns xAdj
        }
        writeString(text, mutableListOf(mockPos))
    }
}

class LayoutScannerTest {

    @Test
    fun `bpayY uses first occurrence when BPAY appears multiple times`() {
        val scanner = TestableLayoutScanner()

        scanner.simulateText("BPAY", yAdj = 300f)
        val firstY = scanner.bpayY
        assertEquals(300f, firstY, "bpayY must be set on first BPAY encounter")

        scanner.simulateText("BPAY", yAdj = 750f)
        assertEquals(firstY, scanner.bpayY, "bpayY must not be overwritten by a later BPAY match")
        assertNotEquals(750f, scanner.bpayY, "Footer BPAY y=750 must be ignored")
    }

    @Test
    fun `bpayY uses first occurrence when Basic Pay appears multiple times`() {
        val scanner = TestableLayoutScanner()

        scanner.simulateText("Basic Pay", yAdj = 280f)
        val firstY = scanner.bpayY
        assertEquals(280f, firstY, "bpayY must be set on first Basic Pay encounter")

        scanner.simulateText("Basic Pay", yAdj = 800f)
        assertEquals(firstY, scanner.bpayY, "bpayY must not be overwritten by later Basic Pay")
    }

    @Test
    fun `bpayY set by BPAY is not overwritten by subsequent Basic Pay`() {
        val scanner = TestableLayoutScanner()

        scanner.simulateText("BPAY", yAdj = 300f)
        assertEquals(300f, scanner.bpayY)

        scanner.simulateText("Basic Pay", yAdj = 700f)
        assertEquals(300f, scanner.bpayY, "First BPAY y=300 must not be overwritten by later Basic Pay")
    }

    @Test
    fun `bpayY default value is 250f before any match`() {
        val scanner = TestableLayoutScanner()
        assertEquals(250f, scanner.bpayY, "Default bpayY must be 250f (the unset sentinel)")
    }

    @Test
    fun `bpayY is set correctly on first encounter`() {
        val scanner = TestableLayoutScanner()
        scanner.simulateText("BPAY", yAdj = 315f)
        assertEquals(315f, scanner.bpayY)
    }
}
