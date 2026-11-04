package com.payslipmax.pdfparser.ui.screens

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * WHY (docs/Plan/14a P8, D12): there is no Cancel for a backup or restore, so the sheet itself must not be closable while one runs: not by
 * swipe-down (the sheet's state), not by the scrim, not by the system Back. An officer who closes it mid-restore would be looking at
 * Settings with work still running and nothing to say so. When nothing is running the same gestures close it as usual, which the control
 * cases prove (so a sheet that never closes would also fail).
 */
@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupSheetDismissUiTest {
    @AfterTest
    fun tearDown() {
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    private class Probe {
        var sheet: SheetState? = null
        var hide: (() -> Unit)? = null
        var back: (() -> Unit)? = null
        var dismissed = 0
    }

    private fun androidx.compose.ui.test.ComposeUiTest.show(
        working: Boolean,
        probe: Probe,
    ) = setContent {
        val sheetState = rememberBackupSheetState(mayHide = !working)
        val scope = rememberCoroutineScope()
        probe.sheet = sheetState
        probe.hide = {
            scope.launch {
                try {
                    sheetState.hide()
                } catch (_: CancellationException) {
                }
            }
        }
        BackupRestoreSheet(sheetState = sheetState, mayHide = !working, onDismiss = { probe.dismissed++ }) {
            Text("sheet body")
            // The sheet is a dialog window with its own back dispatcher: take the one its content sees.
            val owner = LocalOnBackPressedDispatcherOwner.current
            probe.back = { owner?.onBackPressedDispatcher?.onBackPressed() }
        }
    }

    @Test
    fun aSheetThatIsWorkingCannotBeSwipedAway() =
        runComposeUiTest {
            val probe = Probe()
            show(working = true, probe = probe)
            waitForIdle()
            probe.hide!!.invoke()
            waitForIdle()
            assertNotEquals(SheetValue.Hidden, probe.sheet!!.currentValue)
            assertEquals(0, probe.dismissed)
        }

    @Test
    fun anIdleSheetClosesWhenSwipedAway() =
        runComposeUiTest {
            val probe = Probe()
            show(working = false, probe = probe)
            waitForIdle()
            probe.hide!!.invoke()
            waitForIdle()
            assertEquals(SheetValue.Hidden, probe.sheet!!.currentValue)
        }

    @Test
    fun aSheetThatIsWorkingIgnoresSystemBack() =
        runComposeUiTest {
            val probe = Probe()
            show(working = true, probe = probe)
            waitForIdle()
            runOnUiThread { probe.back!!.invoke() }
            waitForIdle()
            assertEquals(0, probe.dismissed, "Back must not dismiss while working")
            assertNotEquals(SheetValue.Hidden, probe.sheet!!.currentValue)
        }

    @Test
    fun anIdleSheetClosesOnSystemBack() =
        runComposeUiTest {
            val probe = Probe()
            show(working = false, probe = probe)
            waitForIdle()
            runOnUiThread { probe.back!!.invoke() }
            waitForIdle()
            assertEquals(1, probe.dismissed)
        }
}
