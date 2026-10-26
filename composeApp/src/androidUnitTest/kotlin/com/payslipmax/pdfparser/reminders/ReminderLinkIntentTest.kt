package com.payslipmax.pdfparser.reminders

import android.content.Intent
import com.payslipmax.pdfparser.part2.domain.ReminderKind
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * WHY: MainActivity is exported, so the extras of a tap are text any app can send. Reading them must hand over only what was there, must
 * not read the same intent twice (a rotation or a return from recents re-delivers it), and must do nothing while Part II is off.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderLinkIntentTest : RemindersAndroidTestBase() {
    @Test
    fun aTapIntentIsOfferedAndThenForgotten() {
        val intent = ReminderLinkIntent.tapIntent(context, ReminderKind.ADD_ORDER, "d1")

        ReminderLinkIntent.offer(intent, pending)

        assertEquals("d1", pending.link.value?.draftId)
        assertEquals(ReminderKind.ADD_ORDER, pending.link.value?.kind)
        assertNull(intent.getStringExtra(ReminderLinkIntent.EXTRA_KIND), "the extras are gone")
        assertNull(intent.getStringExtra(ReminderLinkIntent.EXTRA_DRAFT))
    }

    @Test
    fun aRedeliveredIntentDoesNotOpenAgain() {
        val intent = ReminderLinkIntent.tapIntent(context, ReminderKind.RETURN, "d1")
        ReminderLinkIntent.offer(intent, pending)
        runBlockingTake()

        ReminderLinkIntent.offer(intent, pending)

        assertNull(pending.link.value)
    }

    @Test
    fun anIntentRelaunchedFromRecentsIsIgnoredEvenWithItsExtras() {
        val intent = ReminderLinkIntent.tapIntent(context, ReminderKind.RETURN, "d1").addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)

        ReminderLinkIntent.offer(intent, pending)

        assertNull(pending.link.value)
    }

    @Test
    fun anIntentWithOtherOrForgedExtrasIsDropped() {
        for (bad in listOf(Intent(), Intent().putExtra(ReminderLinkIntent.EXTRA_KIND, "RETURN"), Intent().putExtra(ReminderLinkIntent.EXTRA_KIND, "x").putExtra(ReminderLinkIntent.EXTRA_DRAFT, "d1"))) {
            ReminderLinkIntent.offer(bad, pending)
            assertNull(pending.link.value)
        }
        ReminderLinkIntent.offer(null, pending)
        assertNull(pending.link.value)
    }

    @Test
    fun withPartTwoOffNothingIsKept() {
        ReminderLinkIntent.offer(ReminderLinkIntent.tapIntent(context, ReminderKind.RETURN, "d1"), null)

        assertNull(pending.link.value)
    }

    private fun runBlockingTake() = kotlinx.coroutines.runBlocking { pending.take() }
}
