package com.payslipmax.pdfparser.ui.screens

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The sandbox exposes destructive actions (clear all data, seed mock data, forced crashes). The
 * 7-tap unlock alone must never be enough to reach it in an App Store / Play Store build.
 */
class DeveloperSandboxGateTest {
    @Test
    fun releaseBuildStaysHiddenEvenWhenDevModeUnlocked() {
        assertFalse(shouldShowDeveloperSandbox(devModeEnabled = true, isDebug = false, isTestFlight = false))
    }

    @Test
    fun debugBuildShowsSandboxOnceUnlocked() {
        assertTrue(shouldShowDeveloperSandbox(devModeEnabled = true, isDebug = true, isTestFlight = false))
    }

    @Test
    fun testFlightBuildShowsSandboxOnceUnlocked() {
        assertTrue(shouldShowDeveloperSandbox(devModeEnabled = true, isDebug = false, isTestFlight = true))
    }

    @Test
    fun lockedDevModeHidesSandboxInEveryBuildType() {
        listOf(false to false, true to false, false to true, true to true).forEach { (debug, testFlight) ->
            assertFalse(
                shouldShowDeveloperSandbox(devModeEnabled = false, isDebug = debug, isTestFlight = testFlight),
                "devMode off must hide sandbox (debug=$debug, testFlight=$testFlight)",
            )
        }
    }
}
