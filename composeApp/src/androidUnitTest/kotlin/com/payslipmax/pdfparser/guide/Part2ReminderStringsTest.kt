package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.ui.theme.Part2DueStrings
import com.payslipmax.pdfparser.ui.theme.Part2ReminderStrings
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * WHY (DECISIONS Rounds 10 and 28): a notification can show on a lock screen, so its words are one generic sentence for every reminder.
 * They must never name a code, an order number or a date, nor say what kind of reminder it is, and the platform must be handed exactly these.
 */
class Part2ReminderStringsTest {
    private val shown =
        with(Part2ReminderStrings) { listOf(notificationTitle, notificationBody, channelName, channelDescription) }

    @Test
    fun theNotificationNamesNoCodeNoDigitNoSlashAndNoMonth() {
        val text = File("src/commonMain/composeResources/files/guide/guide_bundle.json").readText()
        val codes = assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(text)).bundle.part2!!.serials.flatMap { it.codes }.toSet()
        val months = listOf("January", "February", "March", "April", "June", "July", "August", "September", "October", "November", "December")
        for (words in shown) {
            assertTrue(words.isNotBlank(), "every notification string is filled in")
            assertEquals(emptyList(), codes.filter { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(words) }, "'$words' names a code")
            assertFalse(words.any { it.isDigit() || it == '/' }, "'$words' carries a digit or a slash, which is how an order number or date looks")
            assertEquals(emptyList(), months.filter { it in words }, "'$words' names a month")
        }
    }

    @Test
    fun theNotificationDoesNotSayWhichKindOfReminderItIs() {
        val kindWords = listOf("rejoin", "order", "year-end", "year end", "leave", "accumulation", "cancel", "slip", "draft")
        val body = Part2ReminderStrings.notificationBody.lowercase()
        assertEquals(emptyList(), kindWords.filter { it in body }, "'$body' tells a lock screen what the reminder is about")
        assertEquals("PayslipMax", Part2ReminderStrings.notificationTitle, "the title is the app's name and nothing else")
        for (kind in com.payslipmax.pdfparser.part2.domain.ReminderKind.entries) {
            assertTrue(Part2DueStrings.action(kind, 2026) != Part2ReminderStrings.notificationBody, "the body is not the Due list's per-kind line")
        }
    }

    @Test
    fun thePlatformIsHandedExactlyTheseWords() {
        val text = Part2ReminderStrings.notificationText()

        assertEquals(shown, listOf(text.title, text.body, text.channelName, text.channelDescription))
    }

    @Test
    fun theExplainerPromisesTheTextIsGenericAndHasTwoDistinctChoices() {
        assertTrue("never" in Part2ReminderStrings.promptBody.lowercase() && "code" in Part2ReminderStrings.promptBody.lowercase(), "it tells the officer what the reminder will not say")
        assertTrue(Part2ReminderStrings.promptAllow.isNotBlank() && Part2ReminderStrings.promptAllow != Part2ReminderStrings.promptNotNow)
    }
}
