package com.payslipmax.pdfparser.onboarding

import platform.Foundation.NSUserDefaults

class IosOnboardingStorage : OnboardingStorage {
    private val keyHasCompletedOnboarding = "has_completed_onboarding"
    private val keyHasSeenUploadCoachmark = "has_seen_upload_coachmark"

    private val defaults get() = NSUserDefaults.standardUserDefaults

    override fun getHasCompletedOnboarding(): Boolean = defaults.boolForKey(keyHasCompletedOnboarding)

    override fun saveHasCompletedOnboarding(completed: Boolean) {
        defaults.setBool(completed, forKey = keyHasCompletedOnboarding)
    }

    override fun getHasSeenUploadCoachmark(): Boolean = defaults.boolForKey(keyHasSeenUploadCoachmark)

    override fun saveHasSeenUploadCoachmark(seen: Boolean) {
        defaults.setBool(seen, forKey = keyHasSeenUploadCoachmark)
    }
}

actual fun provideOnboardingStorage(): OnboardingStorage = IosOnboardingStorage()
