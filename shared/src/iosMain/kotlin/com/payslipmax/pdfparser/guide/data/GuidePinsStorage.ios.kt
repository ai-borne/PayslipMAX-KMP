package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.domain.GuidePins
import platform.Foundation.NSUserDefaults

class IosGuidePinsStorage(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : GuidePinsStorage {
    override fun load(): GuidePins = GuidePins.fromStored(defaults.stringForKey(GUIDE_PINS_KEY))

    override fun save(pins: GuidePins) {
        defaults.setObject(pins.toStored(), forKey = GUIDE_PINS_KEY)
    }
}

actual fun provideGuidePinsStorage(): GuidePinsStorage = IosGuidePinsStorage()
