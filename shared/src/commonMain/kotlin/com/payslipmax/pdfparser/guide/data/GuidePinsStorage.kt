package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.domain.GuidePins

/**
 * Where pinned card ids are kept on the device (the `OnboardingStorage` pattern). Not a database and not part of the
 * encrypted `.pcda` backup: pins are plain card ids, a device-only convenience (owner decision 2026-10-07).
 */
interface GuidePinsStorage {
    fun load(): GuidePins

    fun save(pins: GuidePins)
}

/** The preference key for [GuidePins.toStored]; a future format gets a new key. */
internal const val GUIDE_PINS_KEY = "guide_pins_v1"

expect fun provideGuidePinsStorage(): GuidePinsStorage

/** Keeps pins in memory only; the default where nothing needs them to outlive the process (for example a screen test). */
class InMemoryGuidePinsStorage : GuidePinsStorage {
    private var pins = GuidePins.Empty

    override fun load(): GuidePins = pins

    override fun save(pins: GuidePins) {
        this.pins = pins
    }
}
