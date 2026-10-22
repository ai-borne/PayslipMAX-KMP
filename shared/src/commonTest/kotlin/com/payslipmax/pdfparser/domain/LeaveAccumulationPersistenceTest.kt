package com.payslipmax.pdfparser.domain

import com.payslipmax.pdfparser.database.toDomain
import com.payslipmax.pdfparser.database.toEncryptedEntity
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * The leave table lives inside the encrypted payslip JSON, with no Room migration. That only holds if a payslip saved before
 * this field existed still decodes under the strict decoder, and the table survives encryption and corrections unchanged.
 */
class LeaveAccumulationPersistenceTest {
    private val password = "unit-test-password"

    private val leave =
        LeaveAccumulation(
            listOf(
                LeaveEntry("2024-12-31", LeaveCasCode.ACCRUED, 19, 301, "381", "2025-05-02"),
                LeaveEntry("2020-12-01", LeaveCasCode.LTC_ENCASHED, 10, null, "84", "2021-02-04"),
            ),
        )

    private fun payslip(leaveAccumulation: LeaveAccumulation? = null) =
        ParsedPayslip(
            file = "12 Dec 2025.pdf",
            year = 2025,
            monthNum = 12,
            monthName = "December",
            dateStr = "12/2025",
            officer = Officer("Name", "Acc", "PAN"),
            earnings = Earnings(basicPay = 144700.0, dearnessAllowance = 92916.0),
            deductions = Deductions(),
            ledgerBalances = LedgerBalances(0.0, 0.0, 0.0, 0.0),
            summary = PayslipSummary(237616.0, 0.0, 237616.0),
            taxAndSavings = null,
            leaveAccumulation = leaveAccumulation,
        )

    @Test
    fun leaveTableSurvivesEncryptedStorageRoundTrip() {
        val stored = payslip(leave).toEncryptedEntity(password)

        assertEquals(leave, stored.toDomain(password).leaveAccumulation)
    }

    @Test
    fun payslipStoredBeforeTheFieldExistedStillDecodes() {
        val oldJson = Json.encodeToString(ParsedPayslip.serializer(), payslip())
        assertFalse("leaveAccumulation" in oldJson, "a null table must not be written, so old and new rows look the same")

        assertNull(Json.decodeFromString(ParsedPayslip.serializer(), oldJson).leaveAccumulation)
    }

    @Test
    fun correctionsNeverTouchTheLeaveTable() {
        val corrected =
            payslip(leave).applyCorrections(
                listOf(
                    SingleCorrection(
                        fieldKey = "basicPay",
                        codeHead = "BPAY",
                        amount = 1.0,
                        category = EntryCategory.EARNING,
                        type = CorrectionType.EDITED,
                        originalAmount = 144700.0,
                        originalCodeHead = null,
                        timestamp = 0L,
                    ),
                ),
            )

        assertEquals(leave, corrected.leaveAccumulation)
    }
}
