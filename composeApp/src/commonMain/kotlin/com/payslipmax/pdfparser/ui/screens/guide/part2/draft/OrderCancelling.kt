package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.guide.model.Part2Lookup
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.Part2WriteResult
import com.payslipmax.pdfparser.part2.domain.CancelDrafts
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.Part2Rules
import com.payslipmax.pdfparser.part2.domain.VaultOrders

/** Why a cancel could not start; each has its words in `Part2CancelStrings`. */
enum class CancelRefusal { NOT_READY, GONE, NOT_QUOTABLE, NOTHING_TO_COPY, CANCEL_SLIP, SAVE_FAILED }

/** What starting a cancel did: the cancel slip's draft to open, or why nothing was made. */
sealed interface CancelOutcome {
    data class Started(val draftId: String) : CancelOutcome

    data class Refused(val why: CancelRefusal) : CancelOutcome
}

/**
 * Starts the cancelling of a vault order (Part II plan, phase 6c; para 11, p.8): a cancel slip quoting the order's number and date and, with
 * [cancel]'s `redraft`, a corrected draft copied from the draft the order came from, the two pointing at each other. Both are saved before
 * anything opens, so a restored stack finds them; either both exist or neither does. The order and the old draft are never touched. Reads
 * and writes only through [Part2Repository]; nothing about the order is logged. Who may cancel is decided by the screens (it makes drafts).
 */
class OrderCancelling(
    private val section: Part2Lookup?,
    private val repository: Part2Repository,
    private val rules: Part2Rules,
    private val now: () -> Long,
    private val newId: () -> String,
) {
    suspend fun cancel(
        orderId: String,
        redraft: Boolean,
    ): CancelOutcome {
        val lookup = section ?: return CancelOutcome.Refused(CancelRefusal.NOT_READY)
        val order = repository.order(orderId) ?: return CancelOutcome.Refused(CancelRefusal.GONE)
        val ref = VaultOrders.quotable(listOf(order)).firstOrNull() ?: return CancelOutcome.Refused(CancelRefusal.NOT_QUOTABLE)
        val source = if (redraft) order.draftId?.let { repository.draft(it) } ?: return CancelOutcome.Refused(CancelRefusal.NOTHING_TO_COPY) else null
        if (source != null && CancelDrafts.isCancel(source.event)) return CancelOutcome.Refused(CancelRefusal.CANCEL_SLIP)
        val builder = LeaveDraftStateBuilder(lookup, rules)
        val stamp = now()

        fun draft(
            id: String,
            event: LeaveEventInput,
            partner: String?,
        ) = Part2Draft(id, event, builder.templateRevs(event), stamp, stamp, null, partner)

        val cancelId = newId()
        val fix = source?.let { draft(newId(), CancelDrafts.redraftEvent(it.event), cancelId) }
        if (fix != null && repository.saveDraft(fix) != Part2WriteResult.SAVED) return CancelOutcome.Refused(CancelRefusal.SAVE_FAILED)
        if (repository.saveDraft(draft(cancelId, CancelDrafts.cancelEvent(ref), fix?.id)) != Part2WriteResult.SAVED) {
            fix?.let { repository.deleteDraft(it.id) }
            return CancelOutcome.Refused(CancelRefusal.SAVE_FAILED)
        }
        return CancelOutcome.Started(cancelId)
    }
}
