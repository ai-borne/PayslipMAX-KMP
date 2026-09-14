package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.ui.PayslipUiState
import com.payslipmax.pdfparser.ui.theme.GemmaModelStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests visibility, dismissal, and copy resolution rules for [BaseModelDownloadBanner]:
 * 1. Waiting for Wi-Fi displays clean informational copy.
 * 2. Active downloading shows progress percentage without jargon.
 * 3. Harmless developer sideload errors (Error -15) are ignored.
 * 4. Actual download errors display gentle paused state without raw stack traces.
 * 5. Dismissal flag hides the banner across all states.
 */
class BaseModelDownloadBannerTest {
    @Test
    fun bannerIsVisibleWhenWaitingForWifi() {
        val state = PayslipUiState(isWaitingForWifi = true)
        assertTrue(isModelBannerVisible(state), "Banner should be visible when waiting for Wi-Fi")
    }

    @Test
    fun bannerSuppressesErrorMinus15() {
        val state =
            PayslipUiState(
                modelDownloadError = "Local AI model requires Google Play installation (Error -15: Unrecognized install)",
            )
        assertFalse(isModelBannerVisible(state), "Banner should suppress -15 error on non-Play sideloads")
    }

    @Test
    fun bannerIsVisibleOnGenuineError() {
        val state = PayslipUiState(modelDownloadError = "No space left on device")
        assertTrue(isModelBannerVisible(state), "Banner should be visible on genuine error")
    }

    @Test
    fun bannerIsHiddenWhenDismissedAcrossAllStates() {
        val downloading = PayslipUiState(isDownloadingModel = true, isModelBannerDismissed = true)
        assertFalse(isModelBannerVisible(downloading), "Banner must hide when dismissed during download")

        val waiting = PayslipUiState(isWaitingForWifi = true, isModelBannerDismissed = true)
        assertFalse(isModelBannerVisible(waiting), "Banner must hide when dismissed while waiting for Wi-Fi")

        val error = PayslipUiState(modelDownloadError = "Network timeout", isModelBannerDismissed = true)
        assertFalse(isModelBannerVisible(error), "Banner must hide when dismissed during error")
    }

    @Test
    fun resolveTitleForWaitingForWifi() {
        val state = PayslipUiState(isWaitingForWifi = true)
        val title = resolveBannerTitle(state, hasError = false)
        assertEquals(GemmaModelStrings.gemmaModelWaitingForWifiTitle, title)
    }

    @Test
    fun resolveTitleForActiveDownloadingWithProgress() {
        val state = PayslipUiState(isDownloadingModel = true, modelDownloadProgress = 0.65f)
        val title = resolveBannerTitle(state, hasError = false)
        assertEquals("${GemmaModelStrings.gemmaModelDownloadingTitle} (65%)", title)
    }

    @Test
    fun resolveTitleForPausedError() {
        val state = PayslipUiState(modelDownloadError = "Network connection failed")
        val title = resolveBannerTitle(state, hasError = true)
        assertEquals(GemmaModelStrings.gemmaModelPausedTitle, title)
    }

    @Test
    fun resolveSubtitleForPausedErrorNeverShowsRawTechnicalException() {
        val rawError = "java.io.IOException: SSL handshake aborted: failure in SSL library, cert expired"
        val state = PayslipUiState(modelDownloadError = rawError)
        val subtitle = resolveBannerSubtitle(state, hasError = true)

        assertEquals(GemmaModelStrings.gemmaModelPausedSubtitle, subtitle)
        assertFalse(subtitle.contains("java.io.IOException"), "Must never expose technical raw stack trace to user")
    }
}
