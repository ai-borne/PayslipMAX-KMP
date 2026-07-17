package com.payslipmax.pdfparser.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.crypto.getLegacyFallbackKey
import com.payslipmax.pdfparser.domain.CorrectionType
import com.payslipmax.pdfparser.domain.EntryCategory
import com.payslipmax.pdfparser.domain.SingleCorrection
import com.payslipmax.pdfparser.parser.PayslipPatternConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Phase 5 — user-supplied per-field corrections, stored **separately** from the parsed payslip so the
 * original parse is never mutated (SSOT: corrections are merged on read). One row per payslip, holding
 * an AES-256-encrypted JSON `List<SingleCorrection>`, encrypted at rest with [CryptoHelper] exactly like
 * [EncryptedPayslipEntity] (no plaintext financial data on disk).
 */
@Serializable
@Entity(tableName = "payslip_corrections")
data class PayslipCorrectionEntity(
    @PrimaryKey val dateStr: String,
    // AES-256 encrypted JSON List<SingleCorrection> (Hex-encoded)
    val ciphertext: String,
)

private val jsonDecoder = Json { ignoreUnknownKeys = true }

fun List<SingleCorrection>.toCorrectionEntity(
    dateStr: String,
    password: String = CryptoHelper.getDatabaseSecretKey(),
): PayslipCorrectionEntity {
    val jsonString = Json.encodeToString(this)
    val encryptedBytes = CryptoHelper.encrypt(jsonString.encodeToByteArray(), password).getOrThrow()
    return PayslipCorrectionEntity(dateStr = dateStr, ciphertext = encryptedBytes.toHex())
}

fun PayslipCorrectionEntity.toCorrectionList(password: String = CryptoHelper.getDatabaseSecretKey()): List<SingleCorrection> {
    val encryptedBytes = ciphertext.hexToByteArray()
    val decryptedBytes =
        try {
            CryptoHelper.decrypt(encryptedBytes, password).getOrThrow()
        } catch (e: Exception) {
            CryptoHelper.decrypt(encryptedBytes, CryptoHelper.getLegacyFallbackKey()).getOrThrow()
        }
    val jsonStr = decryptedBytes.decodeToString()
    return try {
        jsonDecoder.decodeFromString<List<SingleCorrection>>(jsonStr)
    } catch (e: Exception) {
        val legacyMap = jsonDecoder.decodeFromString<Map<String, Double>>(jsonStr)
        legacyMap.map { (key, value) ->
            val isEarning = isEarningsField(key)
            SingleCorrection(
                fieldKey = key,
                codeHead = getCodeHeadForField(key, isEarning),
                amount = value,
                category = if (isEarning) EntryCategory.EARNING else EntryCategory.DEDUCTION,
                type = if (value == 0.0) CorrectionType.DELETED else CorrectionType.EDITED,
                originalAmount = null,
                originalCodeHead = null,
                timestamp = 0L,
            )
        }
    }
}

fun PayslipCorrectionEntity.toCorrectionMap(password: String = CryptoHelper.getDatabaseSecretKey()): Map<String, Double> {
    return toCorrectionList(password).associate { it.fieldKey to it.amount }
}

fun Map<String, Double>.toCorrectionEntity(
    dateStr: String,
    password: String = CryptoHelper.getDatabaseSecretKey(),
): PayslipCorrectionEntity {
    val list =
        this.map { (key, value) ->
            val isEarning = isEarningsField(key)
            SingleCorrection(
                fieldKey = key,
                codeHead = getCodeHeadForField(key, isEarning),
                amount = value,
                category = if (isEarning) EntryCategory.EARNING else EntryCategory.DEDUCTION,
                type = if (value == 0.0) CorrectionType.DELETED else CorrectionType.EDITED,
                originalAmount = null,
                originalCodeHead = null,
                timestamp = 0L,
            )
        }
    return list.toCorrectionEntity(dateStr, password)
}

private fun isEarningsField(key: String): Boolean {
    val stdKey = PayslipPatternConfig.creditKeysMapping[key] ?: key
    if (stdKey in PayslipPatternConfig.creditKeysMapping.values) return true
    return stdKey.startsWith("arrears") || stdKey.startsWith("adj") || stdKey == "miscEarnings" || stdKey == "medicalAllowance"
}

private fun getCodeHeadForField(
    key: String,
    isEarning: Boolean,
): String {
    val mapping = if (isEarning) PayslipPatternConfig.creditKeysMapping else PayslipPatternConfig.debitKeysMapping
    return mapping.entries.find { it.value == key }?.key ?: key
}
