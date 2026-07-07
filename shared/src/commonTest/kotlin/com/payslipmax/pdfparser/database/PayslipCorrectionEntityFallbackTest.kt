package com.payslipmax.pdfparser.database

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.domain.CorrectionType
import com.payslipmax.pdfparser.domain.EntryCategory
import com.payslipmax.pdfparser.domain.SingleCorrection
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PayslipCorrectionEntityFallbackTest {
    @Test
    fun testSerializeDeserializeNewFormat() {
        val dateStr = "07/2026"
        val corrections =
            listOf(
                SingleCorrection(
                    fieldKey = "basicPay",
                    codeHead = "BPAY",
                    amount = 145000.0,
                    category = EntryCategory.EARNING,
                    type = CorrectionType.EDITED,
                    originalAmount = 140000.0,
                    originalCodeHead = "BPAY",
                    timestamp = 999999L,
                ),
                SingleCorrection(
                    fieldKey = "incomeTax",
                    codeHead = "ITAX",
                    amount = 0.0,
                    category = EntryCategory.DEDUCTION,
                    type = CorrectionType.DELETED,
                    originalAmount = 40000.0,
                    originalCodeHead = "ITAX",
                    timestamp = 999999L,
                ),
            )

        // Encrypt and store
        val entity = corrections.toCorrectionEntity(dateStr)
        assertEquals(dateStr, entity.dateStr)

        // Decrypt and retrieve
        val decodedList = entity.toCorrectionList()
        assertEquals(2, decodedList.size)

        val basicPayCorr = decodedList.find { it.fieldKey == "basicPay" }
        assertNotNull(basicPayCorr)
        assertEquals(145000.0, basicPayCorr.amount)
        assertEquals(CorrectionType.EDITED, basicPayCorr.type)

        val itaxCorr = decodedList.find { it.fieldKey == "incomeTax" }
        assertNotNull(itaxCorr)
        assertEquals(0.0, itaxCorr.amount)
        assertEquals(CorrectionType.DELETED, itaxCorr.type)
    }

    @Test
    fun testLegacyMapFallbackDecoding() {
        val dateStr = "08/2026"
        val password = CryptoHelper.getDatabaseSecretKey()
        val legacyMap =
            mapOf(
                "basicPay" to 145000.0,
                "incomeTax" to 0.0,
                "CUSTOM_ALLOWANCE" to 3500.0,
            )

        // Replicate legacy map serialization & encryption
        val legacyMapSerializer = MapSerializer(String.serializer(), Double.serializer())
        val jsonString = Json.encodeToString(legacyMapSerializer, legacyMap)
        val encryptedBytes = CryptoHelper.encrypt(jsonString.encodeToByteArray(), password).getOrThrow()

        val entity = PayslipCorrectionEntity(dateStr = dateStr, ciphertext = encryptedBytes.toHex())

        // Decrypt and decode using new list-based decoder (with fallback)
        val decodedList = entity.toCorrectionList()
        assertEquals(3, decodedList.size)

        val basicPayCorr = decodedList.find { it.fieldKey == "basicPay" }
        assertNotNull(basicPayCorr)
        assertEquals(145000.0, basicPayCorr.amount)
        assertEquals(EntryCategory.EARNING, basicPayCorr.category)
        assertEquals(CorrectionType.EDITED, basicPayCorr.type)
        assertEquals("Basic Pay", basicPayCorr.codeHead) // Reverse lookup successful

        val itaxCorr = decodedList.find { it.fieldKey == "incomeTax" }
        assertNotNull(itaxCorr)
        assertEquals(0.0, itaxCorr.amount)
        assertEquals(EntryCategory.DEDUCTION, itaxCorr.category)
        assertEquals(CorrectionType.DELETED, itaxCorr.type)
        assertEquals("Incm Tax", itaxCorr.codeHead) // Reverse lookup successful

        val customCorr = decodedList.find { it.fieldKey == "CUSTOM_ALLOWANCE" }
        assertNotNull(customCorr)
        assertEquals(3500.0, customCorr.amount)
        assertEquals(EntryCategory.DEDUCTION, customCorr.category) // Fallback defaults to Deduction since it's not a known earning field
        assertEquals(CorrectionType.EDITED, customCorr.type)
        assertEquals("CUSTOM_ALLOWANCE", customCorr.codeHead) // Defaults to key
    }
}
