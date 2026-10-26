package com.payslipmax.pdfparser.reminders

import android.content.Intent
import com.payslipmax.pdfparser.part2.reminders.ReminderSync
import com.payslipmax.pdfparser.testing.FakeReminderScheduler
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY (Phase 7b device walk, 2026-10-26): after a reboot the system had dropped all seven queued reminder jobs and nothing brought them back
 * until the app was opened. The receiver rebuilds them from the drafts when the system says boot is complete, and for nothing else.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderBootReceiverTest : RemindersAndroidTestBase() {
    private val scheduler = FakeReminderScheduler()
    private val sync get() = ReminderSync(repository, scheduler, planner, { clock }, kotlinx.coroutines.Dispatchers.Unconfined)

    private fun handle(
        action: String?,
        withSync: Boolean = true,
    ) = runBlocking { ReminderBootReceiver().handle(Intent(action), if (withSync) sync else null) }

    @Test
    fun bootCompletedRebuildsTheScheduleFromTheDrafts() {
        clock = returnDay - 3_600_000L
        save(printedLeave())

        handle(Intent.ACTION_BOOT_COMPLETED)

        assertEquals(planner.plan(listOf(printedLeave()), emptyList(), clock).upcoming, scheduler.pending)
        assertTrue(scheduler.pending.isNotEmpty())
    }

    @Test
    fun anyOtherActionIsIgnored() {
        clock = returnDay - 3_600_000L
        save(printedLeave())

        for (action in listOf(null, Intent.ACTION_USER_PRESENT, Intent.ACTION_LOCKED_BOOT_COMPLETED, "com.example.FAKE_BOOT_COMPLETED", "")) handle(action)

        assertEquals(emptyList(), scheduler.calls)
    }

    @Test
    fun withPartTwoOffThereIsNoSyncAndNothingHappens() {
        save(printedLeave())

        handle(Intent.ACTION_BOOT_COMPLETED, withSync = false)

        assertEquals(emptyList(), scheduler.calls)
    }

    @Test
    fun theReceiverIsDeclaredNotExportedAndListensForBootCompletedOnly() {
        val manifest = File("src/androidMain/AndroidManifest.xml").readText()
        val block = Regex("""<receiver[^>]*ReminderBootReceiver[^>]*>.*?</receiver>""", RegexOption.DOT_MATCHES_ALL).find(manifest)!!.value

        assertTrue("android:exported=\"false\"" in block)
        assertEquals(listOf("android.intent.action.BOOT_COMPLETED"), Regex("""<action android:name="([^"]+)"""").findAll(block).map { it.groupValues[1] }.toList())
    }

    @Test
    fun onReceiveHoldsTheBroadcastOpenWhileItRebuildsAndAlwaysReleasesIt() {
        val source = File("src/androidMain/kotlin/com/payslipmax/pdfparser/reminders/ReminderBootReceiver.kt").readText()

        assertTrue("goAsync()" in source && "finally" in source && "finish()" in source, "a broadcast that returns at once lets the process die mid-rebuild")
    }

    @Test
    fun aDraftsStoreThatCannotBeReadAtBootDoesNotCrashTheProcess() {
        val broken =
            object : com.payslipmax.pdfparser.part2.data.Part2Repository by repository {
                override fun observeDrafts(): kotlinx.coroutines.flow.Flow<com.payslipmax.pdfparser.part2.data.StoredPart2<com.payslipmax.pdfparser.part2.domain.Part2Draft>> = error("the store could not be opened")
            }

        runBlocking { ReminderBootReceiver().handle(Intent(Intent.ACTION_BOOT_COMPLETED), ReminderSync(broken, scheduler, planner, { clock }, kotlinx.coroutines.Dispatchers.Unconfined)) }

        assertEquals(emptyList(), scheduler.calls)
    }
}
