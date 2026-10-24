package com.payslipmax.pdfparser.part2.print

import android.app.Activity
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintJob
import android.print.PrintJobInfo
import android.print.PrintManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.CoroutineContext
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * WHY: the officer prints from the screen they are looking at, so the printer is built from that Activity; if the screen is gone it
 * must do nothing at all. It reports a print only when the system's job completes, and it reaches the system's print dialog only through
 * the print manager: never a share intent, never a file handed to another app.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidSlipPrinterTest {
    private val document = SlipPrintFixtures.document
    private val activity = mockk<Activity>(relaxed = true)
    private val manager = mockk<PrintManager>()
    private val job = mockk<PrintJob>()
    private val info = mockk<PrintJobInfo>()

    private fun printer(on: Activity? = activity) = AndroidSlipPrinter(on, UnconfinedTestDispatcher())

    // `PrintManager.print` is declared without a nullability, and the system can answer null; mockk's `returns` will not take one.
    @Suppress("UNCHECKED_CAST")
    private fun <T> noJob(): T = null as T

    private fun jobStates(vararg states: Int) {
        every { info.state } returnsMany states.toList()
        every { job.info } returns info
        every { manager.print(any(), any(), any()) } returns job
        every { activity.getSystemService(Context.PRINT_SERVICE) } returns manager
    }

    @Test
    fun withNoActivityNothingHappens() =
        runTest {
            assertEquals(PrintOutcome.NOT_PRINTED, printer(null).print("Job", document))
        }

    @Test
    fun aJobTheSystemCompletesIsPrintedUnderTheGivenNameOnA4() =
        runTest {
            jobStates(PrintJobInfo.STATE_CREATED, PrintJobInfo.STATE_QUEUED, PrintJobInfo.STATE_COMPLETED)
            val name = slot<String>()
            val adapter = slot<android.print.PrintDocumentAdapter>()
            val attributes = slot<PrintAttributes>()
            every { manager.print(capture(name), capture(adapter), capture(attributes)) } returns job

            val outcome = printer().print("Job name", document)

            assertEquals(PrintOutcome.COMPLETED, outcome)
            assertEquals("Job name", name.captured)
            assertIs<SlipPrintAdapter>(adapter.captured)
            assertEquals(PrintAttributes.MediaSize.ISO_A4, attributes.captured.mediaSize)
        }

    @Test
    fun aCancelledOrFailedJobIsNotPrinted() =
        runTest {
            for (end in listOf(PrintJobInfo.STATE_CANCELED, PrintJobInfo.STATE_FAILED)) {
                jobStates(PrintJobInfo.STATE_CREATED, end)
                assertEquals(PrintOutcome.NOT_PRINTED, printer().print("Job", document))
            }
        }

    @Test
    fun aSystemThatWillNotPrintIsNotPrinted() =
        runTest {
            every { activity.getSystemService(Context.PRINT_SERVICE) } returns null
            assertEquals(PrintOutcome.NOT_PRINTED, printer().print("Job", document), "no print service")

            every { activity.getSystemService(Context.PRINT_SERVICE) } returns manager
            every { manager.print(any(), any(), any()) } answers { noJob() }
            assertEquals(PrintOutcome.NOT_PRINTED, printer().print("Job", document), "no job")

            every { manager.print(any(), any(), any()) } throws IllegalStateException("the screen is closing")
            assertEquals(PrintOutcome.NOT_PRINTED, printer().print("Job", document), "the dialog could not open")
        }

    @Test
    fun theDialogIsOpenedOnTheUiDispatcherAndNothingIsSentToAnotherApp() =
        runTest {
            jobStates(PrintJobInfo.STATE_COMPLETED)

            AndroidSlipPrinter(activity, Dispatchers.Unconfined).print("Job", document)

            verify(exactly = 0) { activity.startActivity(any()) }
            verify(exactly = 0) { activity.startActivity(any(), any()) }
            verify(exactly = 0) { activity.startIntentSender(any(), any(), any(), any(), any()) }
        }

    @Test
    fun theDialogIsOpenedOnTheDispatcherItWasGiven() =
        runTest {
            jobStates(PrintJobInfo.STATE_COMPLETED)
            var dispatched = 0
            val ui =
                object : CoroutineDispatcher() {
                    override fun dispatch(
                        context: CoroutineContext,
                        block: Runnable,
                    ) {
                        dispatched++
                        block.run()
                    }
                }

            AndroidSlipPrinter(activity, ui).print("Job", document)

            assertEquals(1, dispatched, "PrintManager.print is called on the main thread, once")
        }
}
