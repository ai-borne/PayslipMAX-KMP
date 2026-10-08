package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.data.GuidePinsStorage
import com.payslipmax.pdfparser.guide.domain.GuidePins
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The user's pinned cards, held in memory and saved to [storage] on every change (a Koin single, like the other Guide
 * holders). It touches only plain card ids: no card text, no profile, and nothing is sent anywhere. Callers on the UI thread.
 */
class GuidePinsModel(private val storage: GuidePinsStorage) {
    private val state = MutableStateFlow(storage.load())
    val pins: StateFlow<GuidePins> = state

    fun toggle(cardId: String) = change { it.toggle(cardId) }

    /** Drops pins for cards the loaded bundle no longer holds, and saves only when something was dropped. */
    fun retainKnown(isKnown: (String) -> Boolean) = change { it.retainKnown(isKnown) }

    private fun change(transform: (GuidePins) -> GuidePins) {
        val next = transform(state.value)
        if (next == state.value) return
        state.value = next
        storage.save(next)
    }
}
