package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.data.GuideWhatsNewStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The newest change-log entry the user has seen, held in memory and saved to [storage] on every change (a Koin single, like the pins). It
 * decides only whether Guide Home shows the "new" banner: an entry is new until the user opens or dismisses it, and a NEWER entry is new
 * again. Dates are ISO (YYYY-MM-DD), so plain string order is date order. Nothing but a date is stored, and nothing is sent anywhere.
 */
class GuideWhatsNewSeenModel(private val storage: GuideWhatsNewStorage) {
    private val state = MutableStateFlow(storage.load())
    val seen: StateFlow<String?> = state

    /** The newest entry seen before this session began; the change log marks entries newer than it, since opening it marks everything seen. */
    val seenBeforeSession: String? = state.value

    /** True when [latestDate] is newer than anything seen; false when the bundle has no entry. */
    fun isNew(latestDate: String?): Boolean = latestDate != null && isNewerThan(latestDate, state.value)

    /** Records [date] as seen. An older date never moves the seen date back. */
    fun markSeen(date: String) {
        if (!isNewerThan(date, state.value)) return
        state.value = date
        storage.save(date)
    }

    companion object {
        /** The same rule for a screen that holds the collected [seen] date. */
        fun isNewerThan(
            latestDate: String,
            seen: String?,
        ): Boolean = seen == null || latestDate > seen
    }
}
