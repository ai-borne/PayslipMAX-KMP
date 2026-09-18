package com.payslipmax.pdfparser.onboarding

import android.content.Context
import com.payslipmax.pdfparser.crypto.ContextHolder

class AndroidOnboardingStorage : OnboardingStorage {
    private val prefsName = "payslipmax_onboarding_prefs"
    private val keyHasCompletedOnboarding = "has_completed_onboarding"
    private val keyHasSeenUploadCoachmark = "has_seen_upload_coachmark"

    override fun getHasCompletedOnboarding(): Boolean {
        val ctx = ContextHolder.context ?: return false
        val prefs = ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        return prefs.getBoolean(keyHasCompletedOnboarding, false)
    }

    override fun saveHasCompletedOnboarding(completed: Boolean) {
        val ctx = ContextHolder.context ?: return
        val prefs = ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(keyHasCompletedOnboarding, completed).apply()
    }

    override fun getHasSeenUploadCoachmark(): Boolean {
        val ctx = ContextHolder.context ?: return false
        val prefs = ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        return prefs.getBoolean(keyHasSeenUploadCoachmark, false)
    }

    override fun saveHasSeenUploadCoachmark(seen: Boolean) {
        val ctx = ContextHolder.context ?: return
        val prefs = ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(keyHasSeenUploadCoachmark, seen).apply()
    }
}

actual fun provideOnboardingStorage(): OnboardingStorage = AndroidOnboardingStorage()
