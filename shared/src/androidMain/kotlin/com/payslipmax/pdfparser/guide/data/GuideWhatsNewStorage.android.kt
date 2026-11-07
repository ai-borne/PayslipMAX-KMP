package com.payslipmax.pdfparser.guide.data

import android.content.Context
import com.payslipmax.pdfparser.crypto.ContextHolder

private const val PREFS_NAME = "payslipmax_guide_prefs"

class AndroidGuideWhatsNewStorage : GuideWhatsNewStorage {
    private fun prefs() = ContextHolder.context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): String? = prefs()?.getString(GUIDE_WHATS_NEW_SEEN_KEY, null)

    override fun save(date: String) {
        prefs()?.edit()?.putString(GUIDE_WHATS_NEW_SEEN_KEY, date)?.apply()
    }
}

actual fun provideGuideWhatsNewStorage(): GuideWhatsNewStorage = AndroidGuideWhatsNewStorage()
