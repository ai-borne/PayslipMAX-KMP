package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.onboarding.OnboardingStorage

/** In-memory [OnboardingStorage]; defaults to a fresh install (nothing seen yet). */
class FakeOnboardingStorage(
    private var hasCompletedOnboarding: Boolean = false,
    private var hasSeenUploadCoachmark: Boolean = false,
    private var hasSeenPayAuditIntro: Boolean = false,
) : OnboardingStorage {
    override fun getHasCompletedOnboarding(): Boolean = hasCompletedOnboarding

    override fun saveHasCompletedOnboarding(completed: Boolean) {
        hasCompletedOnboarding = completed
    }

    override fun getHasSeenUploadCoachmark(): Boolean = hasSeenUploadCoachmark

    override fun saveHasSeenUploadCoachmark(seen: Boolean) {
        hasSeenUploadCoachmark = seen
    }

    override fun getHasSeenPayAuditIntro(): Boolean = hasSeenPayAuditIntro

    override fun saveHasSeenPayAuditIntro(seen: Boolean) {
        hasSeenPayAuditIntro = seen
    }
}
