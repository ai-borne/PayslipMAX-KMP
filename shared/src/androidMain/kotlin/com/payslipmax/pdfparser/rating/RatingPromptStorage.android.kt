package com.payslipmax.pdfparser.rating

import android.content.Context
import com.payslipmax.pdfparser.crypto.ContextHolder

class AndroidRatingPromptStorage : RatingPromptStorage {
    private val prefsName = "payslipmax_rating_prefs"
    private val keyCleanSuccessCount = "clean_success_count"
    private val keyLastPromptTimestampMs = "last_prompt_timestamp_ms"
    private val keyHasEverPrompted = "has_ever_prompted"

    override fun getCleanSuccessCount(): Int {
        val ctx = ContextHolder.context ?: return 0
        val prefs = ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        return prefs.getInt(keyCleanSuccessCount, 0)
    }

    override fun saveCleanSuccessCount(count: Int) {
        val ctx = ContextHolder.context ?: return
        val prefs = ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit().putInt(keyCleanSuccessCount, count).apply()
    }

    override fun getLastPromptTimestampMs(): Long? {
        val ctx = ContextHolder.context ?: return null
        val prefs = ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        if (!prefs.contains(keyLastPromptTimestampMs)) return null
        return prefs.getLong(keyLastPromptTimestampMs, 0L)
    }

    override fun saveLastPromptTimestampMs(timestampMs: Long) {
        val ctx = ContextHolder.context ?: return
        val prefs = ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit().putLong(keyLastPromptTimestampMs, timestampMs).apply()
    }

    override fun getHasEverPrompted(): Boolean {
        val ctx = ContextHolder.context ?: return false
        val prefs = ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        return prefs.getBoolean(keyHasEverPrompted, false)
    }

    override fun saveHasEverPrompted(hasPrompted: Boolean) {
        val ctx = ContextHolder.context ?: return
        val prefs = ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(keyHasEverPrompted, hasPrompted).apply()
    }
}

actual fun provideRatingPromptStorage(): RatingPromptStorage = AndroidRatingPromptStorage()
