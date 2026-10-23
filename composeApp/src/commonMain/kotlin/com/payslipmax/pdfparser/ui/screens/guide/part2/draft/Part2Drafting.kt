package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.domain.LeaveEventEditor
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.Part2Rules
import com.payslipmax.pdfparser.rating.currentTimeMillis
import com.payslipmax.pdfparser.ui.screens.guide.GuideUiState
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2LookupViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Makes the models for one leave draft: a new one for a chosen code (with a fresh random id, which is all a restored stack remembers of
 * it) or one read back by id. Both read the loaded Guide's Part II section; with none (the Guide has not loaded, or the bundle ships no
 * Part II) there is nothing to draft from and the answer is null. The one place that wires a draft to the repository, the rules and the clock.
 */
class Part2Drafting
    @OptIn(ExperimentalUuidApi::class)
    constructor(
        private val guide: GuideViewModel,
        private val repository: Part2Repository,
        private val rules: Part2Rules = Part2Rules.shipped(),
        private val now: () -> Long = ::currentTimeMillis,
        private val newId: () -> String = { Uuid.random().toString() },
        private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    ) {
        /** A new draft for [code], not saved until the officer first changes something. */
        fun start(code: String): LeaveDraftViewModel? = make(newId(), LeaveEventEditor.start(code).takeIf { it.requests.isNotEmpty() } ?: return null)

        /** The draft saved under [id], read from the repository; its state is `GONE` when the repository no longer holds it. */
        fun open(id: String): LeaveDraftViewModel? = make(id, null)

        private fun make(
            id: String,
            start: LeaveEventInput?,
        ): LeaveDraftViewModel? {
            val section = ((guide.uiState.value as? GuideUiState.Ready)?.bundle)?.part2 ?: return null
            return LeaveDraftViewModel(id, section, repository, start, rules, now, dispatcher)
        }
    }

/** The models the Guide's Part II levels draw from: the lookup, the drafts list and the maker of one draft. Plain holder, no logic. */
class Part2Models(
    val lookup: Part2LookupViewModel,
    val drafts: Part2DraftsViewModel,
    val drafting: Part2Drafting,
)
