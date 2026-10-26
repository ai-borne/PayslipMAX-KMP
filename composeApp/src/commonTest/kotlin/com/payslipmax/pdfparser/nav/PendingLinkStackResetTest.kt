package com.payslipmax.pdfparser.nav

import com.payslipmax.pdfparser.testing.FakePart2Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: on iOS a pushed detail is a native screen that hides the tab tree, so a reminder tap that opens Due must first close it. That must
 * happen only once the app is unlocked (a locked app pushes and pops nothing), once per tap, and never when nothing is waiting.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PendingLinkStackResetTest {
    private val pending = PendingPart2Link(FakePart2Repository())
    private val locked = MutableStateFlow(false)
    private var resets = 0

    private fun TestScopeJob(scope: CoroutineScope): Job {
        PendingLinkStackReset(pending, locked) { resets++ }.start(scope)
        return scope.coroutineContext[Job]!!
    }

    @Test
    fun aTapThatArrivesWhileUnlockedClosesTheDetailsOnce() =
        runTest(UnconfinedTestDispatcher()) {
            val scope = CoroutineScope(coroutineContext + Job())
            TestScopeJob(scope)

            pending.offer("RETURN", "d1")

            assertEquals(1, resets)
            scope.coroutineContext[Job]!!.cancel()
        }

    @Test
    fun aTapThatWaitedWhileLockedClosesThemWhenTheAppUnlocks() =
        runTest(UnconfinedTestDispatcher()) {
            locked.value = true
            val scope = CoroutineScope(coroutineContext + Job())
            TestScopeJob(scope)

            pending.offer("RETURN", "d1")
            assertEquals(0, resets, "a locked app pushes and pops nothing")
            locked.value = false

            assertEquals(1, resets)
            scope.coroutineContext[Job]!!.cancel()
        }

    @Test
    fun withNothingWaitingNothingIsClosedWhateverTheLockDoes() =
        runTest(UnconfinedTestDispatcher()) {
            val scope = CoroutineScope(coroutineContext + Job())
            TestScopeJob(scope)

            locked.value = true
            locked.value = false

            assertEquals(0, resets)
            scope.coroutineContext[Job]!!.cancel()
        }

    @Test
    fun aSecondTapAfterTheFirstWasOpenedClosesThemAgain() =
        runTest(UnconfinedTestDispatcher()) {
            val scope = CoroutineScope(coroutineContext + Job())
            TestScopeJob(scope)
            pending.offer("RETURN", "d1")

            pending.offer("ADD_ORDER", "d1")
            assertEquals(1, resets, "a newer tap replacing a waiting one is still one reset: the screen is already closed")
            kotlinx.coroutines.runBlocking { pending.take() }
            pending.offer("YEAR_END", "d1")

            assertEquals(2, resets)
            scope.coroutineContext[Job]!!.cancel()
        }
}
