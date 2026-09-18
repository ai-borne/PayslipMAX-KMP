package com.payslipmax.pdfparser.onboarding

/**
 * Decides whether the first-run onboarding carousel and the one-time upload-FAB coachmark should
 * show. Every install is treated as fresh (no legacy-user migration short-circuit) since the app
 * is still in closed testing with no real-world install base.
 */
class OnboardingManager(
    private val storage: OnboardingStorage = provideOnboardingStorage(),
) {
    fun shouldShowOnboarding(): Boolean = !storage.getHasCompletedOnboarding()

    fun onOnboardingCompleted() {
        storage.saveHasCompletedOnboarding(true)
    }

    fun shouldShowCoachmark(): Boolean = !storage.getHasSeenUploadCoachmark()

    fun onCoachmarkDismissed() {
        storage.saveHasSeenUploadCoachmark(true)
    }
}
