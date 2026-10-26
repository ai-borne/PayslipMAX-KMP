package com.payslipmax.pdfparser.part2.reminders

import android.content.Context
import com.payslipmax.pdfparser.crypto.ContextHolder

/** Private SharedPreferences (`allowBackup` is off, so they stay on this device). With no context the answer is "asked": never nag on a guess. */
class AndroidReminderPromptStore : ReminderPromptStore {
    private val prefs get() = ContextHolder.context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun wasAsked(): Boolean = prefs?.getBoolean(KEY, false) ?: true

    override fun markAsked() {
        prefs?.edit()?.putBoolean(KEY, true)?.apply()
    }

    companion object {
        const val PREFS = "payslipmax_part2_reminder_prefs"
        const val KEY = "reminder_permission_asked"
    }
}

actual fun provideReminderPromptStore(): ReminderPromptStore = AndroidReminderPromptStore()
