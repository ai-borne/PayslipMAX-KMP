package com.payslipmax.pdfparser.guide.data

import android.content.Context
import com.payslipmax.pdfparser.crypto.ContextHolder
import com.payslipmax.pdfparser.guide.domain.GuidePins

private const val PREFS_NAME = "payslipmax_guide_prefs"

class AndroidGuidePinsStorage : GuidePinsStorage {
    private fun prefs() = ContextHolder.context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): GuidePins = GuidePins.fromStored(prefs()?.getString(GUIDE_PINS_KEY, null))

    override fun save(pins: GuidePins) {
        prefs()?.edit()?.putString(GUIDE_PINS_KEY, pins.toStored())?.apply()
    }
}

actual fun provideGuidePinsStorage(): GuidePinsStorage = AndroidGuidePinsStorage()
