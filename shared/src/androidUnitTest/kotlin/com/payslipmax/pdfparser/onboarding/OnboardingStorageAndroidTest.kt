package com.payslipmax.pdfparser.onboarding

import com.payslipmax.pdfparser.crypto.ContextHolder
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class OnboardingStorageAndroidTest {
    @After
    fun tearDown() {
        ContextHolder.context = null
    }

    @Test
    fun nullContextReturnsSafeDefaults() {
        ContextHolder.context = null
        val storage = AndroidOnboardingStorage()

        assertFalse(storage.getHasCompletedOnboarding())
        assertFalse(storage.getHasSeenUploadCoachmark())
    }

    @Test
    fun realContextRoundTripsOnboardingCompletedFlagIndependentlyOfCoachmarkFlag() {
        ContextHolder.context = RuntimeEnvironment.getApplication()
        val storage = AndroidOnboardingStorage()

        storage.saveHasCompletedOnboarding(true)

        assertTrue(storage.getHasCompletedOnboarding())
        assertFalse(storage.getHasSeenUploadCoachmark())
    }

    @Test
    fun realContextRoundTripsCoachmarkFlagIndependentlyOfOnboardingCompletedFlag() {
        ContextHolder.context = RuntimeEnvironment.getApplication()
        val storage = AndroidOnboardingStorage()

        storage.saveHasSeenUploadCoachmark(true)

        assertTrue(storage.getHasSeenUploadCoachmark())
        assertFalse(storage.getHasCompletedOnboarding())
    }
}
