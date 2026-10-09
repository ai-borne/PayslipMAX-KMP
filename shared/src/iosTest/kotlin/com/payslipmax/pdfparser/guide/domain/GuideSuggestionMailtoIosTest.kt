package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.utils.buildMailtoUrlString
import platform.Foundation.NSURL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * M5 on Native: the real suggestion email, with every character that can break a `mailto:` link, goes through the real iOS
 * link builder. The subject and body must come back byte for byte after the mail app decodes them, the link must stay one
 * query with one separator, and NSURL must accept it. This is the iOS half of the "special characters" evidence; the
 * encoder itself is also covered by [com.payslipmax.pdfparser.utils.ShareUtilsIosTest].
 */
class GuideSuggestionMailtoIosTest {
    private val labels =
        GuideSuggestionLabels("[Guide]", "Guide correction suggestion", "Card:", "Title:", "Guide data:", "Card revision:", "App version:", "Suggestion:")

    private val hostile = "Rs 1,500 & \"x\" <b>1</b> 100% = ₹1,500 + #1? a=b&c=d रिपोर्ट 😀 [x]\ttab\nline2\r\nline3"

    private fun parts(text: String) = GuideSuggestionParts("RB-T068", "What's the food rate? (A&B)", "2026-10-07", "ab12cd34", "1.3.0", text)

    private fun url(text: String): Triple<String, String, String> {
        val p = parts(text)
        val subject = GuideSuggestion.subject(p, labels)
        val body = GuideSuggestion.body(p, labels)
        return Triple(buildMailtoUrlString("founder@ai-borne.in", subject, body), subject, body)
    }

    /** Percent-decodes as a mail app would: %XX are bytes of UTF-8. Independent of the encoder under test. */
    private fun decode(encoded: String): String {
        val bytes = ArrayList<Byte>()
        var i = 0
        while (i < encoded.length) {
            if (encoded[i] == '%') {
                bytes += encoded.substring(i + 1, i + 3).toInt(16).toByte()
                i += 3
            } else {
                bytes += encoded[i].code.toByte()
                i++
            }
        }
        return bytes.toByteArray().decodeToString()
    }

    @Test
    fun subjectAndBodyRoundTripExactlyThroughTheMailtoLinkWithHostileCharacters() {
        val (link, subject, body) = url(hostile)

        assertEquals(subject, decode(link.substringAfter("subject=").substringBefore("&body=")))
        assertEquals(body, decode(link.substringAfter("&body=")))
    }

    @Test
    fun theLinkStaysOneQueryAndNeverHoldsARawDelimiterSpaceBracketOrLineBreak() {
        val (link, _, _) = url(hostile)

        assertEquals(1, link.count { it == '&' }, "only the subject/body separator may remain unescaped")
        assertEquals(1, link.count { it == '?' })
        for (raw in listOf(' ', '\n', '\r', '\t', '[', ']', '"', '#', '<', '+')) assertFalse(link.contains(raw), "raw '$raw' in the link")
        assertTrue(link.startsWith("mailto:founder%40ai-borne.in?subject=%5BGuide%5D%20RB-T068&body="))
    }

    @Test
    fun nsUrlAcceptsTheLinkEvenForAFullLengthTextOfThreeByteCharacters() {
        val full = "रि".repeat(GuideSuggestion.MAX_TEXT_LENGTH / 2)
        val (link, _, body) = url(full)

        assertNotNull(NSURL.URLWithString(link), "a maximum-length Devanagari text must still be a valid URL")
        assertEquals(body, decode(link.substringAfter("&body=")))
    }

    @Test
    fun anEmojiCutAtTheLimitNeverReachesTheLinkAsABrokenCharacter() {
        val cut = "a".repeat(GuideSuggestion.MAX_TEXT_LENGTH - 1) + "😀"
        val (link, _, body) = url(cut)

        assertFalse(link.contains("%EF%BF%BD"), "no replacement character: the half pair was dropped before encoding")
        assertEquals(body, decode(link.substringAfter("&body=")))
    }
}
