package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.guide.data.GuidePinsStorage
import com.payslipmax.pdfparser.guide.domain.GuidePins

/** In-memory [GuidePinsStorage]. [stored] is the raw string, so a test can plant a damaged value or read what was saved. */
class FakeGuidePinsStorage(
    var stored: String? = null,
) : GuidePinsStorage {
    var saves: Int = 0
        private set

    override fun load(): GuidePins = GuidePins.fromStored(stored)

    override fun save(pins: GuidePins) {
        stored = pins.toStored()
        saves++
    }
}
