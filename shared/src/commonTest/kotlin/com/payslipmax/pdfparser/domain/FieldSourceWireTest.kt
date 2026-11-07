package com.payslipmax.pdfparser.domain

import com.payslipmax.pdfparser.database.toDomain
import com.payslipmax.pdfparser.database.toEncryptedEntity
import com.payslipmax.pdfparser.repository.backup.BackupJson
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY (docs/Plan/14a P5, D16, characterization): `ParsedPayslip.fieldSource` is written into every payslip on the device and in every
 * backup, and shipped apps (1.5.0 and earlier) read it with a strict decoder. Making its reader tolerant of a value a later app adds must
 * not change one byte of what is written for the values this app knows, or an older app would stop reading this app's payslips. So
 * today's encoding is pinned here, byte for byte, before the serializer changes, and both readers (backup and device) must give the same
 * payslip back.
 */
class FieldSourceWireTest {
    private val payslip =
        ParsedPayslip(
            file = "f.pdf",
            year = 2024,
            monthNum = 8,
            monthName = "August",
            dateStr = "08/2024",
            officer = Officer("N", "A", "P"),
            earnings = Earnings(),
            deductions = Deductions(),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(1.0, 2.0, 3.0),
            taxAndSavings = null,
        )

    private val head =
        """{"file":"f.pdf","year":2024,"monthNum":8,"monthName":"August","dateStr":"08/2024",""" +
            """"officer":{"name":"N","accountNo":"A","pan":"P"},"earnings":{},"deductions":{},"ledgerBalances":{},""" +
            """"summary":{"grossPay":1.0,"totalDeductions":2.0,"netRemittance":3.0},"taxAndSavings":null"""

    private fun written(sources: Map<String, FieldSource>) = Json.encodeToString(ParsedPayslip.serializer(), payslip.copy(fieldSource = sources))

    @Test
    fun theKnownValuesAreExactlyTheOnesPinnedHere() {
        // A new value must have its encoding pinned below before it ships; this fails until it is.
        assertEquals(listOf("GEOMETRY", "GEMMA_FALLBACK"), FieldSource.entries.map { it.name })
    }

    @Test
    fun eachKnownValueIsWrittenExactlyAsBefore() {
        assertEquals("""$head,"fieldSource":{"basicPay":"GEOMETRY"}}""", written(mapOf("basicPay" to FieldSource.GEOMETRY)))
        assertEquals("""$head,"fieldSource":{"basicPay":"GEMMA_FALLBACK"}}""", written(mapOf("basicPay" to FieldSource.GEMMA_FALLBACK)))
    }

    @Test
    fun aMapOfBothValuesKeepsItsOrderAndAnEmptyMapIsNotWritten() {
        val both = linkedMapOf("incomeTax" to FieldSource.GEMMA_FALLBACK, "basicPay" to FieldSource.GEOMETRY)
        assertEquals("""$head,"fieldSource":{"incomeTax":"GEMMA_FALLBACK","basicPay":"GEOMETRY"}}""", written(both))
        assertEquals("$head}", written(emptyMap()))
    }

    @Test
    fun theBackupReaderAndTheDeviceReaderGiveTheSamePayslipBack() {
        val both = payslip.copy(fieldSource = linkedMapOf("incomeTax" to FieldSource.GEMMA_FALLBACK, "basicPay" to FieldSource.GEOMETRY))

        assertEquals(both, BackupJson.decode(ParsedPayslip.serializer(), Json.encodeToString(ParsedPayslip.serializer(), both)))
        assertEquals(both, both.toEncryptedEntity(DEVICE_KEY).toDomain(DEVICE_KEY))
    }

    private companion object {
        const val DEVICE_KEY = "device-key-for-the-test"
    }
}
