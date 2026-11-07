package com.payslipmax.pdfparser.part2.pages

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * WHY: the source decides what the officer's tap on "scan" turns into: no screen means no scanner, a scanner Play services cannot start
 * means no scanner (never a crash), a closed scanner means a cancel, and only the pages it really returned go on. The two Play services
 * calls are replaced here; they were proved on a device in the Phase 6a spike.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MlKitScanSourceTest {
    private val activity = mockk<Activity>(relaxed = true)
    private val sender = mockk<IntentSender>(relaxed = true)
    private val data = Intent()

    private fun source(
        activity: Activity? = this.activity,
        answer: ScanActivityResult = ScanActivityResult(true, data),
        start: suspend (Activity, Int) -> IntentSender = { _, _ -> sender },
        pages: (Intent?) -> List<Uri>? = { listOf(Uri.parse("file:///c/mlkit_docscan_ui_client/1.jpg")) },
        launched: MutableList<IntentSender> = mutableListOf(),
    ) = MlKitScanSource(activity, {
        launched += it
        answer
    }, start, pages)

    @Test
    fun withNoScreenThereIsNoScanner() =
        runTest {
            assertEquals(ScanFailure.UNAVAILABLE, assertIs<SourceResult.Failed>(source(activity = null).scan(3)).reason)
        }

    @Test
    fun aScannerPlayServicesCannotStartIsUnavailableNotACrash() =
        runTest {
            val result = source(start = { _, _ -> error("MARKER_module_missing") }).scan(3)

            assertEquals(ScanFailure.UNAVAILABLE, assertIs<SourceResult.Failed>(result).reason)
        }

    @Test
    fun theStartIsAskedForTheRoomLeftOnTheScreenItWasGiven() =
        runTest {
            val asked = mutableListOf<Pair<Activity, Int>>()

            source(start = { screen, pages ->
                asked += screen to pages
                sender
            }).scan(7)

            assertEquals(listOf(activity to 7), asked)
        }

    @Test
    fun theScannerIsLaunchedWithTheStartedSender() =
        runTest {
            val launched = mutableListOf<IntentSender>()

            source(launched = launched).scan(3)

            assertEquals(listOf(sender), launched)
        }

    @Test
    fun aClosedScannerIsACancelAndItsPagesAreNotLookedAt() =
        runTest {
            var looked = false

            val result =
                source(answer = ScanActivityResult(false, data), pages = {
                    looked = true
                    emptyList()
                }).scan(3)

            assertEquals(SourceResult.Cancelled, result)
            assertTrue(!looked, "a cancelled scan has no result to read")
        }

    @Test
    fun theConfirmedPagesGoOnAsFilePathsInOrder() =
        runTest {
            val received = mutableListOf<Intent?>()
            val result =
                source(pages = {
                    received += it
                    listOf(Uri.parse("file:///c/1.jpg"), Uri.parse("content://x/2"), Uri.parse("file:///c/3.jpg"))
                }).scan(3)

            assertEquals(listOf<Intent?>(data), received)
            assertEquals(listOf("/c/1.jpg", "/c/3.jpg"), assertIs<SourceResult.Files>(result).paths)
        }

    @Test
    fun anAnswerThatHoldsNoResultIsAFailure() =
        runTest {
            assertEquals(ScanFailure.FAILED, assertIs<SourceResult.Failed>(source(pages = { null }).scan(3)).reason)
        }

    @Test
    fun walkingAwayWhileTheScannerStartsIsACancellationNotAnUnavailableScanner() =
        runTest {
            val entered = CompletableDeferred<Unit>()
            val never = CompletableDeferred<IntentSender>()
            var outcome: Result<SourceResult>? = null
            val job =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    outcome =
                        runCatching {
                            source(start = { _, _ ->
                                entered.complete(Unit)
                                never.await()
                            }).scan(3)
                        }
                }
            entered.await()

            job.cancelAndJoin()

            assertTrue(outcome!!.exceptionOrNull() is kotlinx.coroutines.CancellationException, "$outcome")
        }

    @Test
    fun aSecondScanIsIndependentOfTheFirst() =
        runTest {
            val scanner = source()

            val first = async { scanner.scan(3) }
            val second = async { scanner.scan(3) }

            assertIs<SourceResult.Files>(first.await())
            assertIs<SourceResult.Files>(second.await())
        }
}
