package com.payslipmax.pdfparser.nav

import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.domain.ReminderKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** What a notification tap names: the kind of reminder and the draft it came from, nothing else. [toString] hides the id. */
data class Part2Link(
    val kind: ReminderKind,
    val draftId: String,
) {
    override fun toString(): String = "Part2Link($kind)"

    companion object {
        /** The one reading of a tap's two strings, for the activity's intent and the worker's input alike: a known kind and a plain id, or null. */
        fun parse(
            kind: String?,
            draftId: String?,
        ): Part2Link? {
            val known = ReminderKind.entries.firstOrNull { it.name == kind } ?: return null
            return if (draftId != null && Part2Limits.isPlainId(draftId)) Part2Link(known, draftId) else null
        }
    }
}

/**
 * The notification tap waiting to be opened (plan, Navigation and process death): Koin-scoped, never an `object` with a `var`, so it
 * outlives the lock screen. A tap that arrives while the app is locked is held here and opened by the unlocked app. Both extras are
 * untrusted (the launcher activity is exported, so any app can send them): [offer] keeps only a known kind and a plain id, and [take]
 * hands the link over only if the database still holds that draft. Anything else is dropped without a trace; nothing is logged.
 */
class PendingPart2Link(
    private val repository: Part2Repository,
) {
    private val held = MutableStateFlow<Part2Link?>(null)

    /** The link waiting to be opened, or null. Reading it does not consume it. */
    val link: StateFlow<Part2Link?> = held

    /** Holds the tap named by [kind] and [draftId] if both are well-formed; a malformed offer changes nothing, a good one replaces any older. */
    fun offer(
        kind: String?,
        draftId: String?,
    ) {
        held.value = Part2Link.parse(kind, draftId) ?: return
    }

    /** Takes the waiting link: it is returned only if its draft is still on the device, and it is cleared either way. */
    suspend fun take(): Part2Link? {
        val waiting = held.value ?: return null
        held.value = null
        return waiting.takeIf { repository.draft(it.draftId) != null }
    }

    override fun toString(): String = "PendingPart2Link()"
}
