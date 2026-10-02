package com.payslipmax.pdfparser.onboarding

/**
 * Persistence abstraction for onboarding-carousel, upload-coachmark and Pay Audit orientation completion state.
 */
interface OnboardingStorage {
    fun getHasCompletedOnboarding(): Boolean

    fun saveHasCompletedOnboarding(completed: Boolean)

    fun getHasSeenUploadCoachmark(): Boolean

    fun saveHasSeenUploadCoachmark(seen: Boolean)

    fun getHasSeenPayAuditIntro(): Boolean

    fun saveHasSeenPayAuditIntro(seen: Boolean)
}

/**
 * Expect function providing platform-specific persistent storage for [OnboardingStorage].
 */
expect fun provideOnboardingStorage(): OnboardingStorage
