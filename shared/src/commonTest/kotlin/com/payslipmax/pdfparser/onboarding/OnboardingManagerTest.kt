package com.payslipmax.pdfparser.onboarding

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingManagerTest {
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
    fun shouldShowOnboardingTrueWhenUnset() {
        val manager = OnboardingManager(FakeOnboardingStorage())

        assertTrue(manager.shouldShowOnboarding())
    }

    @Test
    fun shouldShowOnboardingFalseAfterCompleted() {
        val storage = FakeOnboardingStorage()
        val manager = OnboardingManager(storage)

        manager.onOnboardingCompleted()

        assertFalse(manager.shouldShowOnboarding())
    }

    @Test
    fun shouldShowCoachmarkTrueByDefault() {
        val manager = OnboardingManager(FakeOnboardingStorage())

        assertTrue(manager.shouldShowCoachmark())
    }

    @Test
    fun shouldShowCoachmarkFalseAfterDismissed() {
        val storage = FakeOnboardingStorage()
        val manager = OnboardingManager(storage)

        manager.onCoachmarkDismissed()

        assertFalse(manager.shouldShowCoachmark())
    }

    @Test
    fun coachmarkFlagIsIndependentOfOnboardingCompletedFlag() {
        val storage = FakeOnboardingStorage()
        val manager = OnboardingManager(storage)

        manager.onOnboardingCompleted()

        assertTrue(manager.shouldShowCoachmark())
        assertFalse(manager.shouldShowOnboarding())
    }
}
