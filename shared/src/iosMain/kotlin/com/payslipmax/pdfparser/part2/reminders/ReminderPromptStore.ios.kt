package com.payslipmax.pdfparser.part2.reminders

import platform.Foundation.NSUserDefaults

/** The app's own user defaults, like the onboarding flags. */
class IosReminderPromptStore(
    private val key: String = KEY,
) : ReminderPromptStore {
    private val defaults get() = NSUserDefaults.standardUserDefaults

    override fun wasAsked(): Boolean = defaults.boolForKey(key)

    override fun markAsked() {
        defaults.setBool(true, forKey = key)
    }

    companion object {
        const val KEY = "reminder_permission_asked"
    }
}

actual fun provideReminderPromptStore(): ReminderPromptStore = IosReminderPromptStore()
