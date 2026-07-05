package com.payslipmax.pdfparser.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.crypto.getLegacyFallbackKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Phase 5 — user-supplied per-field corrections, stored **separately** from the parsed payslip so the
 * original parse is never mutated (SSOT: corrections are merged on read). One row per payslip, holding
 * an AES-256-encrypted JSON `Map<fieldKey, Double>`, encrypted at rest with [CryptoHelper] exactly like
 * [EncryptedPayslipEntity] (no plaintext financial data on disk).
 */
@Serializable
@Entity(tableName = "payslip_corrections")
data class PayslipCorrectionEntity(
    @PrimaryKey val dateStr: String,
    // AES-256 encrypted JSON Map<String, Double> (Hex-encoded)
    val ciphertext: String,
)

private val correctionMapSerializer = MapSerializer(String.serializer(), Double.serializer())

fun Map<String, Double>.toCorrectionEntity(
    dateStr: String,
    password: String = CryptoHelper.getDatabaseSecretKey(),
): PayslipCorrectionEntity {
    val jsonString = Json.encodeToString(correctionMapSerializer, this)
    val encryptedBytes = CryptoHelper.encrypt(jsonString.encodeToByteArray(), password).getOrThrow()
    return PayslipCorrectionEntity(dateStr = dateStr, ciphertext = encryptedBytes.toHex())
}

fun PayslipCorrectionEntity.toCorrectionMap(password: String = CryptoHelper.getDatabaseSecretKey()): Map<String, Double> {
    val encryptedBytes = ciphertext.hexToByteArray()
    val decryptedBytes =
        try {
            CryptoHelper.decrypt(encryptedBytes, password).getOrThrow()
        } catch (e: Exception) {
            CryptoHelper.decrypt(encryptedBytes, CryptoHelper.getLegacyFallbackKey()).getOrThrow()
        }
    return Json.decodeFromString(correctionMapSerializer, decryptedBytes.decodeToString())
}
