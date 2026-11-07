package com.payslipmax.pdfparser.guide.data

/**
 * Where the date of the newest change-log entry the user has seen is kept on the device (the `GuidePinsStorage` pattern): one date
 * string (YYYY-MM-DD), in preferences. Not a database and not in the backup: it only decides whether Guide Home shows the "new" banner.
 */
interface GuideWhatsNewStorage {
    /** The newest entry date the user has seen, or null before the first time. */
    fun load(): String?

    fun save(date: String)
}

/** The preference key; a future format gets a new key. */
internal const val GUIDE_WHATS_NEW_SEEN_KEY = "guide_whats_new_seen_v1"

expect fun provideGuideWhatsNewStorage(): GuideWhatsNewStorage

/** Keeps the date in memory only; the default where nothing needs it to outlive the process (a screen test). */
class InMemoryGuideWhatsNewStorage : GuideWhatsNewStorage {
    private var date: String? = null

    override fun load(): String? = date

    override fun save(date: String) {
        this.date = date
    }
}
