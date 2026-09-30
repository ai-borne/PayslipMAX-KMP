package com.payslipmax.pdfparser.onboarding

import platform.Foundation.NSUserDefaults

class IosOnboardingStorage : OnboardingStorage {
    private val keyHasCompletedOnboarding = "has_completed_onboarding"
    private val keyHasSeenUploadCoachmark = "has_seen_upload_coachmark"
    private val keyHasSeenPcdaoAuditIntro = "has_seen_pcdao_audit_intro"

    private val defaults get() = NSUserDefaults.standardUserDefaults

    override fun getHasCompletedOnboarding(): Boolean = defaults.boolForKey(keyHasCompletedOnboarding)

    override fun saveHasCompletedOnboarding(completed: Boolean) {
        defaults.setBool(completed, forKey = keyHasCompletedOnboarding)
    }

    override fun getHasSeenUploadCoachmark(): Boolean = defaults.boolForKey(keyHasSeenUploadCoachmark)

    override fun saveHasSeenUploadCoachmark(seen: Boolean) {
        defaults.setBool(seen, forKey = keyHasSeenUploadCoachmark)
    }

    override fun getHasSeenPcdaoAuditIntro(): Boolean = defaults.boolForKey(keyHasSeenPcdaoAuditIntro)

    override fun saveHasSeenPcdaoAuditIntro(seen: Boolean) {
        defaults.setBool(seen, forKey = keyHasSeenPcdaoAuditIntro)
    }
}

actual fun provideOnboardingStorage(): OnboardingStorage = IosOnboardingStorage()
