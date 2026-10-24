package com.payslipmax.pdfparser.part2.pages

import android.content.IntentSender
import io.mockk.mockk
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: on Android only a screen can start the scanner's intent and receive its answer, so a view model's suspending call is bridged to the
 * screen's result callback. The bridge must hand the answer to the call that started it, ignore an answer nobody asked for (a late or
 * repeated callback), and not keep a finished or abandoned scan waiting.
 */
class ScanResultBridgeTest {
    private val sender = mockk<IntentSender>(relaxed = true)

    @Test
    fun theScreenAnswerReachesTheCallThatStartedTheScan() =
        runTest {
            val bridge = ScanResultBridge()
            val started = mutableListOf<IntentSender>()
            bridge.start = { started += it }

            val answer = async(start = CoroutineStart.UNDISPATCHED) { bridge.launch(sender) }
            bridge.onResult(ScanActivityResult(true, null))

            assertEquals(listOf(sender), started)
            assertTrue(answer.await().ok)
        }

    @Test
    fun anAnswerThatArrivesDuringTheStartIsNotLost() =
        runTest {
            val bridge = ScanResultBridge()
            bridge.start = { bridge.onResult(ScanActivityResult(false, null)) }

            assertFalse(bridge.launch(sender).ok)
        }

    @Test
    fun anAnswerWithNoScanWaitingIsIgnoredAndDoesNotAnswerTheNextScan() =
        runTest {
            val bridge = ScanResultBridge()
            bridge.start = {}
            bridge.onResult(ScanActivityResult(true, null))

            val answer = async(start = CoroutineStart.UNDISPATCHED) { bridge.launch(sender) }
            bridge.onResult(ScanActivityResult(false, null))

            assertFalse(answer.await().ok, "the stray answer from before must not have been kept for this scan")
        }

    @Test
    fun aSecondAnswerForTheSameScanIsIgnored() =
        runTest {
            val bridge = ScanResultBridge()
            bridge.start = {}
            val answer = async(start = CoroutineStart.UNDISPATCHED) { bridge.launch(sender) }

            bridge.onResult(ScanActivityResult(true, null))
            bridge.onResult(ScanActivityResult(false, null))

            assertTrue(answer.await().ok)
        }

    @Test
    fun aCancelledScanLeavesNothingWaitingForTheNextOne() =
        runTest {
            val bridge = ScanResultBridge()
            bridge.start = {}
            val abandoned = launch(start = CoroutineStart.UNDISPATCHED) { bridge.launch(sender) }
            abandoned.cancelAndJoin()

            val next = async(start = CoroutineStart.UNDISPATCHED) { bridge.launch(sender) }
            bridge.onResult(ScanActivityResult(true, null))

            assertTrue(next.await().ok)
        }
}
