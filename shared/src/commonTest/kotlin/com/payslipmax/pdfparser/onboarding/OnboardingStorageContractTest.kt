package com.payslipmax.pdfparser.onboarding

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingStorageContractTest {
    private class FakeOnboardingStorage(
        private var hasCompletedOnboarding: Boolean = false,
        private var hasSeenUploadCoachmark: Boolean = false,
    ) : OnboardingStorage {
        override fun getHasCompletedOnboarding(): Boolean = hasCompletedOnboarding

        override fun saveHasCompletedOnboarding(completed: Boolean) {
            hasCompletedOnboarding = completed
        }

        override fun getHasSeenUploadCoachmark(): Boolean = hasSeenUploadCoachmark

        override fun saveHasSeenUploadCoachmark(seen: Boolean) {
            hasSeenUploadCoachmark = seen
        }
    }

    @Test
    fun defaultsAreFalse() {
        val storage = FakeOnboardingStorage()

        assertFalse(storage.getHasCompletedOnboarding())
        assertFalse(storage.getHasSeenUploadCoachmark())
    }

    @Test
    fun onboardingCompletedFlagRoundTripsIndependentlyOfCoachmarkFlag() {
        val storage = FakeOnboardingStorage()

        storage.saveHasCompletedOnboarding(true)

        assertTrue(storage.getHasCompletedOnboarding())
        assertFalse(storage.getHasSeenUploadCoachmark())
    }

    @Test
    fun coachmarkFlagRoundTripsIndependentlyOfOnboardingCompletedFlag() {
        val storage = FakeOnboardingStorage()

        storage.saveHasSeenUploadCoachmark(true)

        assertTrue(storage.getHasSeenUploadCoachmark())
        assertFalse(storage.getHasCompletedOnboarding())
    }
}
