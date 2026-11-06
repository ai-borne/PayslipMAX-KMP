package com.payslipmax.pdfparser.guide.data

import platform.Foundation.NSUserDefaults

class IosGuideWhatsNewStorage(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : GuideWhatsNewStorage {
    override fun load(): String? = defaults.stringForKey(GUIDE_WHATS_NEW_SEEN_KEY)

    override fun save(date: String) {
        defaults.setObject(date, forKey = GUIDE_WHATS_NEW_SEEN_KEY)
    }
}

actual fun provideGuideWhatsNewStorage(): GuideWhatsNewStorage = IosGuideWhatsNewStorage()
