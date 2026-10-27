package com.payslipmax.pdfparser.part2.domain

/** How much of an order a cancel slip covers: [ALL] occurrences or [ONE] occurrence (note 7 p.227). [ALL] sorts first. */
enum class CancelScope { ALL, ONE }

/** Where the cancelling of a published order stands, worked out and never stored. [scope] says whether the whole order or one occurrence. */
sealed interface CancelState {
    val scope: CancelScope

    /** A slip that cancels the order was handed to the clerk (the print finished at [printedAt]). */
    data class SlipPrinted(
        override val scope: CancelScope,
        val printedAt: Long,
    ) : CancelState

    /** The published cancel order, [order], is in the vault and linked to the slip: the proof. */
    data class CancelledBy(
        override val scope: CancelScope,
        val order: OrderRef,
    ) : CancelState
}

/**
 * What the vault says of an order someone has cancelled (DECISIONS Round 22). A slip is matched to the order by the number and date it
 * quotes (trimmed), as the clerk's cancel will be; a slip not yet printed has cancelled nothing. Nothing is stored, so the facts can never
 * disagree with the drafts and orders they come from, and the reminder planner can read the same answer.
 */
object CancelStatus {
    fun of(
        order: VaultOrder,
        drafts: List<Part2Draft>,
        orders: List<VaultOrder>,
    ): CancelState? {
        val target = VaultOrders.quotable(listOf(order)).firstOrNull() ?: return null
        val others = orders.filter { other -> other.id != order.id }
        return drafts
            .mapNotNull { stateOf(it, target, others) }
            .minWithOrNull(compareBy<CancelState> { it.scope }.thenBy { it !is CancelState.CancelledBy }.thenByDescending { (it as? CancelState.SlipPrinted)?.printedAt ?: 0L })
    }

    /** [drafts] without the void ones: a draft whose published order was cancelled in full. */
    fun liveDrafts(
        drafts: List<Part2Draft>,
        orders: List<VaultOrder>,
    ): List<Part2Draft> {
        val ordersOf = orders.groupBy { it.draftId }
        val cancels = drafts.filter { CancelDrafts.isCancel(it.event) }
        return drafts.filterNot { draft -> ordersOf[draft.id].orEmpty().any { of(it, cancels, orders)?.scope == CancelScope.ALL } }
    }

    private fun stateOf(
        draft: Part2Draft,
        target: OrderRef,
        orders: List<VaultOrder>,
    ): CancelState? {
        val scopes = draft.event.requests.filter { it.code in CancelRules.CODES && quotes(it, target) }.map { if (it.code == CancelRules.ALL_OCCURRENCES) CancelScope.ALL else CancelScope.ONE }
        val scope = scopes.minOrNull() ?: return null
        val proof = VaultOrders.quotable(orders.filter { it.draftId == draft.id }).firstOrNull()
        return when {
            proof != null -> CancelState.CancelledBy(scope, proof)
            draft.printed != null -> CancelState.SlipPrinted(scope, draft.printed.at)
            else -> null
        }
    }

    private fun quotes(
        request: OccurrenceRequest,
        target: OrderRef,
    ): Boolean {
        val ref = (request.values[CancelRules.CANCELLED] as? SlotValue.RefValue)?.ref ?: return false
        return ref.orderNo.trim() == target.orderNo.trim() && ref.date == target.date
    }
}
