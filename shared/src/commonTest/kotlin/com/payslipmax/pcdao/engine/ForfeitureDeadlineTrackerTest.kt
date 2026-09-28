package com.payslipmax.pcdao.engine

import com.payslipmax.pcdao.model.DiscrepancySeverity
import com.payslipmax.pcdao.model.DiscrepancyType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ForfeitureDeadlineTrackerTest {
    private val tracker = ForfeitureDeadlineTracker()

    @Test
    fun testSprClaimOnTrackDay40() {
        val result =
            tracker.evaluateClaim(
                claimType = MilitaryClaimType.SPR_DECLARATION,
                daysElapsed = 40,
                estimatedAmount = 150000.0,
            )

        assertEquals(DeadlineStatus.ON_TRACK, result.status)
        assertEquals(60, result.deadlineDays)
        assertEquals(40, result.daysElapsed)
        assertEquals(20, result.daysRemaining)
        assertFalse(result.condonationRequired)
        assertNull(result.discrepancy)
    }

    @Test
    fun testSprClaimExpiringSoonDay55() {
        val result =
            tracker.evaluateClaim(
                claimType = MilitaryClaimType.SPR_DECLARATION,
                daysElapsed = 55,
                estimatedAmount = 120000.0,
            )

        assertEquals(DeadlineStatus.EXPIRING_SOON, result.status)
        assertEquals(5, result.daysRemaining)
        assertFalse(result.condonationRequired)

        val disc = result.discrepancy
        assertNotNull(disc)
        assertEquals(DiscrepancyType.FORFEITURE_RISK, disc.type)
        assertEquals(DiscrepancySeverity.WARNING, disc.severity)
        assertTrue(disc.title.contains("Expiring Soon"))
        assertTrue(disc.recommendedAction.contains("5 days"))
    }

    @Test
    fun testSprClaimTimeBarredDay65RequiresCondonation() {
        val result =
            tracker.evaluateClaim(
                claimType = MilitaryClaimType.SPR_DECLARATION,
                daysElapsed = 65,
                estimatedAmount = 180000.0,
            )

        assertEquals(DeadlineStatus.TIME_BARRED_FORFEITED, result.status)
        assertEquals(0, result.daysRemaining)
        val instructions = result.condonationInstructions
        assertNotNull(instructions)
        assertTrue(instructions.contains("Condonation of Delay"))

        val disc = result.discrepancy
        assertNotNull(disc)
        assertEquals(DiscrepancyType.FORFEITURE_RISK, disc.type)
        assertEquals(DiscrepancySeverity.CRITICAL, disc.severity)
        assertEquals(-180000.0, disc.netDue)
        assertTrue(disc.authority.contains("HRA_SPR_001") || disc.authority.contains("PCDA(O)"))
        assertTrue(disc.recommendedAction.contains("Area HQ") || disc.recommendedAction.contains("Condonation"))
    }

    @Test
    fun testLtcClaimWithAdvanceDay35Barred() {
        val claim =
            MilitaryClaimItem(
                claimId = "LTC_2026_01",
                claimType = MilitaryClaimType.LTC_CLAIM,
                daysElapsed = 35,
                hasAdvanceBeenDrawn = true,
                advanceAmount = 40000.0,
                estimatedClaimAmount = 45000.0,
            )

        val request = ForfeitureAuditRequest(claims = listOf(claim))
        val result = tracker.audit(request)

        assertEquals(1, result.timeBarredCount)
        assertEquals(1, result.discrepancies.size)

        val disc = result.discrepancies.first()
        assertEquals(DiscrepancyType.FORFEITURE_RISK, disc.type)
        assertEquals(DiscrepancySeverity.CRITICAL, disc.severity)
        // Recovery of advance with 18% penal interest when advance taken
        assertEquals(40000.0, disc.drawnAmount)
        assertEquals(-47200.0, disc.netDue)
        assertTrue(disc.explanation.contains("penal interest") || disc.explanation.contains("refund"))
    }

    @Test
    fun testLtcClaimWithoutAdvanceUses60DaysLimit() {
        val claim =
            MilitaryClaimItem(
                claimId = "LTC_2026_NO_ADV",
                claimType = MilitaryClaimType.LTC_CLAIM,
                daysElapsed = 50,
                hasAdvanceBeenDrawn = false,
                estimatedClaimAmount = 30000.0,
            )

        val result = tracker.audit(ForfeitureAuditRequest(claims = listOf(claim)))
        assertEquals(0, result.timeBarredCount)
        assertEquals(1, result.expiringSoonCount)

        val item = result.claimResults.first()
        assertEquals(60, item.deadlineDays)
        assertEquals(10, item.daysRemaining)
        assertEquals(DeadlineStatus.EXPIRING_SOON, item.status)
    }

    @Test
    fun testTemporaryDutyWithoutAdvanceDay70BarredUnderGfr290() {
        val result =
            tracker.evaluateClaim(
                claimType = MilitaryClaimType.TEMPORARY_DUTY,
                daysElapsed = 70,
                estimatedAmount = 25000.0,
            )

        assertEquals(DeadlineStatus.TIME_BARRED_FORFEITED, result.status)
        assertTrue(result.condonationRequired)
        val disc = result.discrepancy
        assertNotNull(disc)
        assertTrue(disc.authority.contains("Rule 290 GFR-2017") || disc.authority.contains("GFR"))
    }

    @Test
    fun testCompositeTransferGrantDay170ExpiringAndDay190Barred() {
        val expResult =
            tracker.evaluateClaim(
                claimType = MilitaryClaimType.COMPOSITE_TRANSFER_GRANT,
                daysElapsed = 170,
                estimatedAmount = 80000.0,
            )
        assertEquals(DeadlineStatus.EXPIRING_SOON, expResult.status)
        assertEquals(10, expResult.daysRemaining)

        val barResult =
            tracker.evaluateClaim(
                claimType = MilitaryClaimType.COMPOSITE_TRANSFER_GRANT,
                daysElapsed = 190,
                estimatedAmount = 80000.0,
            )
        assertEquals(DeadlineStatus.TIME_BARRED_FORFEITED, barResult.status)
        assertEquals(180, barResult.deadlineDays)
        assertTrue(barResult.condonationRequired)
    }

    @Test
    fun testCalendarDateCalculations() {
        val claim =
            MilitaryClaimItem(
                claimId = "SPR_DATE_TEST",
                claimType = MilitaryClaimType.SPR_DECLARATION,
                eventDate = "2026-08-01",
                estimatedClaimAmount = 50000.0,
            )

        // 50 days elapsed between 2026-08-01 and 2026-09-20
        val request =
            ForfeitureAuditRequest(
                claims = listOf(claim),
                asOfDate = "2026-09-20",
            )

        val result = tracker.audit(request)
        val item = result.claimResults.first()
        assertEquals(50, item.daysElapsed)
        assertEquals(10, item.daysRemaining)
        assertEquals(DeadlineStatus.EXPIRING_SOON, item.status)
    }

    @Test
    fun testMultiClaimAuditAggregation() {
        val claims =
            listOf(
                MilitaryClaimItem(
                    claimId = "C1",
                    claimType = MilitaryClaimType.SPR_DECLARATION,
                    daysElapsed = 30,
                ),
                MilitaryClaimItem(
                    claimId = "C2",
                    claimType = MilitaryClaimType.LTC_CLAIM,
                    daysElapsed = 25,
                    hasAdvanceBeenDrawn = true,
                    estimatedClaimAmount = 20000.0,
                ),
                MilitaryClaimItem(
                    claimId = "C3",
                    claimType = MilitaryClaimType.TEMPORARY_DUTY,
                    daysElapsed = 65,
                    estimatedClaimAmount = 15000.0,
                ),
            )

        val result = tracker.audit(ForfeitureAuditRequest(claims = claims))
        assertEquals(1, result.onTrackCount)
        assertEquals(1, result.expiringSoonCount)
        assertEquals(1, result.timeBarredCount)
        assertTrue(result.hasUrgentDeadlines)
        assertEquals(2, result.discrepancies.size)
        assertEquals(15000.0, result.totalForfeitureExposure)
    }
}
