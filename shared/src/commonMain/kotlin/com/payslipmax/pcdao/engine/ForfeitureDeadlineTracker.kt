package com.payslipmax.pcdao.engine

import com.payslipmax.pcdao.model.AuditDiscrepancy
import com.payslipmax.pcdao.model.DiscrepancySeverity
import com.payslipmax.pcdao.model.DiscrepancyType

class ForfeitureDeadlineTracker {
    fun audit(request: ForfeitureAuditRequest): ForfeitureDeadlineResult {
        val claimResults = request.claims.map { evaluateSingleClaim(it, request.asOfDate) }
        val discrepancies = claimResults.mapNotNull { it.discrepancy }
        val timeBarredCount = claimResults.count { it.status == DeadlineStatus.TIME_BARRED_FORFEITED }
        val expiringSoonCount = claimResults.count { it.status == DeadlineStatus.EXPIRING_SOON }
        val onTrackCount = claimResults.count { it.status == DeadlineStatus.ON_TRACK }

        val totalExposure =
            claimResults
                .filter { it.status == DeadlineStatus.TIME_BARRED_FORFEITED }
                .sumOf { r ->
                    val claim = request.claims.firstOrNull { it.claimId == r.claimId }
                    if (claim != null && claim.hasAdvanceBeenDrawn) {
                        claim.advanceAmount * 1.18
                    } else {
                        claim?.estimatedClaimAmount ?: 0.0
                    }
                }

        return ForfeitureDeadlineResult(
            claimResults = claimResults,
            discrepancies = discrepancies,
            timeBarredCount = timeBarredCount,
            expiringSoonCount = expiringSoonCount,
            onTrackCount = onTrackCount,
            totalForfeitureExposure = totalExposure,
        )
    }

    fun evaluateClaim(
        claimType: MilitaryClaimType,
        daysElapsed: Int,
        estimatedAmount: Double = 0.0,
    ): ForfeitureClaimResult {
        val claim =
            MilitaryClaimItem(
                claimId = "${claimType.name}_EVAL",
                claimType = claimType,
                daysElapsed = daysElapsed,
                estimatedClaimAmount = estimatedAmount,
            )
        return evaluateSingleClaim(claim, null)
    }

    fun evaluateClaimByCode(
        code: String,
        daysElapsed: Int,
        estimatedAmount: Double = 0.0,
    ): ForfeitureClaimResult? {
        val type = MilitaryClaimType.fromCode(code) ?: return null
        return evaluateClaim(type, daysElapsed, estimatedAmount)
    }

    private fun evaluateSingleClaim(
        claim: MilitaryClaimItem,
        asOfDate: String?,
    ): ForfeitureClaimResult {
        val deadline = resolveDeadlineDays(claim)
        val elapsed = resolveDaysElapsed(claim, asOfDate)
        val remaining = (deadline - elapsed).coerceAtLeast(0)

        val status =
            when {
                elapsed > deadline || (deadline - elapsed) <= 0 -> DeadlineStatus.TIME_BARRED_FORFEITED
                (deadline - elapsed) in 1..15 -> DeadlineStatus.EXPIRING_SOON
                else -> DeadlineStatus.ON_TRACK
            }

        val condonationRequired = status == DeadlineStatus.TIME_BARRED_FORFEITED
        val instructions = if (condonationRequired) buildCondonationInstructions(claim.claimType) else null
        val discrepancy = createDiscrepancy(claim, elapsed, remaining, status, instructions)

        return ForfeitureClaimResult(
            claimId = claim.claimId,
            claimType = claim.claimType,
            deadlineDays = deadline,
            daysElapsed = elapsed,
            daysRemaining = remaining,
            status = status,
            condonationRequired = condonationRequired,
            condonationInstructions = instructions,
            discrepancy = discrepancy,
        )
    }

    private fun resolveDeadlineDays(claim: MilitaryClaimItem): Int =
        if (claim.claimType == MilitaryClaimType.LTC_CLAIM && !claim.hasAdvanceBeenDrawn) {
            60
        } else {
            claim.claimType.defaultDeadlineDays
        }

    private fun resolveDaysElapsed(
        claim: MilitaryClaimItem,
        asOfDate: String?,
    ): Int {
        if (claim.daysElapsed != null) return claim.daysElapsed
        if (claim.eventDate != null && asOfDate != null) {
            return PcdaoDateUtils.daysBetween(claim.eventDate, asOfDate).coerceAtLeast(0)
        }
        return 0
    }

    private fun createDiscrepancy(
        claim: MilitaryClaimItem,
        daysElapsed: Int,
        daysRemaining: Int,
        status: DeadlineStatus,
        condonationInstructions: String?,
    ): AuditDiscrepancy? {
        if (status == DeadlineStatus.ON_TRACK) return null

        val (auth, ruleId) = resolveAuthorityAndRule(claim.claimType)
        return if (status == DeadlineStatus.TIME_BARRED_FORFEITED) {
            val netDue = if (claim.hasAdvanceBeenDrawn) -(claim.advanceAmount * 1.18) else -claim.estimatedClaimAmount
            AuditDiscrepancy(
                id = "TIME_BARRED_${claim.claimType.name}_${claim.claimId}",
                title = "${claim.claimType.displayName} Time-Barred (Statutory Forfeiture Risk)",
                type = DiscrepancyType.FORFEITURE_RISK,
                severity = DiscrepancySeverity.CRITICAL,
                drawnAmount = if (claim.hasAdvanceBeenDrawn) claim.advanceAmount else 0.0,
                entitledAmount = claim.estimatedClaimAmount,
                netDue = netDue,
                authority = auth,
                explanation =
                    "${claim.claimType.displayName} has elapsed $daysElapsed days, exceeding the statutory deadline of " +
                        "${resolveDeadlineDays(claim)} days. " +
                        if (claim.hasAdvanceBeenDrawn) {
                            "Advance of ₹${claim.advanceAmount.toLong()} is subject to immediate summary recovery with 18% penal interest."
                        } else {
                            "The claim is legally deemed forfeited unless condonation is obtained."
                        },
                recommendedAction = condonationInstructions ?: "Submit Condonation of Delay application through commanding officer.",
                relevantRuleId = ruleId,
            )
        } else {
            AuditDiscrepancy(
                id = "EXPIRING_SOON_${claim.claimType.name}_${claim.claimId}",
                title = "${claim.claimType.displayName} Expiring Soon ($daysRemaining Days Remaining)",
                type = DiscrepancyType.FORFEITURE_RISK,
                severity = DiscrepancySeverity.WARNING,
                drawnAmount = 0.0,
                entitledAmount = claim.estimatedClaimAmount,
                netDue = -claim.estimatedClaimAmount,
                authority = auth,
                explanation =
                    "${claim.claimType.displayName} has only $daysRemaining days left before the ${resolveDeadlineDays(claim)}-day " +
                        "statutory time-bar expires.",
                recommendedAction = "Submit claim to Task Desk immediately within remaining $daysRemaining days to avoid forfeiture.",
                relevantRuleId = ruleId,
            )
        }
    }

    private fun resolveAuthorityAndRule(claimType: MilitaryClaimType): Pair<String, String> =
        when (claimType) {
            MilitaryClaimType.SPR_DECLARATION ->
                Pair("PCDA(O) FAQ p. 26; Rule HRA_SPR_001; GoI MoD Orders", "HRA_SPR_001")
            MilitaryClaimType.LTC_CLAIM ->
                Pair("Travel Regulations Rule 177; Rule 290 GFR-2017; PCDA(O) FAQ", "LTC_177_002")
            MilitaryClaimType.TEMPORARY_DUTY ->
                Pair("Rule 290 GFR-2017; Travel Regulations TR-230/290", "TRAVEL_TD_001")
            MilitaryClaimType.COMPOSITE_TRANSFER_GRANT ->
                Pair("GoI MoD letter No. 19030/1/2017-E.IV dated 15 June 2021", "TRANSFER_CTG_001")
        }

    private fun buildCondonationInstructions(claimType: MilitaryClaimType): String =
        when (claimType) {
            MilitaryClaimType.SPR_DECLARATION ->
                "Submit Application for Condonation of Delay to Area HQ / PCDA(O) Pune enclosing Part II Order copy, " +
                    "posting order, reason for delay certificate, and Non-Allotment Certificate."
            MilitaryClaimType.LTC_CLAIM ->
                "Submit Condonation Application to Competent Authority (GOC / Area Commander) under TR 177 / GFR 290 " +
                    "enclosing boarding passes, tickets, and justification for delay."
            MilitaryClaimType.TEMPORARY_DUTY ->
                "Submit Ex-post Facto Sanction / Condonation Application to Competent Financial Authority (CFA) citing " +
                    "Rule 290 GFR-2017 with movement order and attendance certificate."
            MilitaryClaimType.COMPOSITE_TRANSFER_GRANT ->
                "Submit Delay Condonation Appeal to PCDA(O) Pune countersigned by Station Commander / Col Q with " +
                    "family shifting proof and self-declaration certificate."
        }
}
