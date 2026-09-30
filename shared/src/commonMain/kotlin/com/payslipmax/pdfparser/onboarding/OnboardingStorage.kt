package com.payslipmax.pdfparser.onboarding

/**
 * Persistence abstraction for onboarding-carousel and upload-coachmark completion state.
 */
interface OnboardingStorage {
    fun getHasCompletedOnboarding(): Boolean

    fun saveHasCompletedOnboarding(completed: Boolean)

    fun getHasSeenUploadCoachmark(): Boolean

    fun saveHasSeenUploadCoachmark(seen: Boolean)

    fun getHasSeenPcdaoAuditIntro(): Boolean = false

    fun saveHasSeenPcdaoAuditIntro(seen: Boolean) {}
}

/**
 * Expect function providing platform-specific persistent storage for [OnboardingStorage].
 */
expect fun provideOnboardingStorage(): OnboardingStorage
