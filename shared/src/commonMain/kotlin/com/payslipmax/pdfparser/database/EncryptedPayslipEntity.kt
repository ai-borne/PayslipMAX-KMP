package com.payslipmax.pdfparser.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.crypto.PasswordKey
import com.payslipmax.pdfparser.crypto.SealKey
import com.payslipmax.pdfparser.crypto.getLegacyFallbackKey
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.logging.Logger
import com.payslipmax.pdfparser.repository.backup.BackupJson
import kotlinx.serialization.json.Json

@Entity(tableName = "encrypted_payslips")
data class EncryptedPayslipEntity(
    @PrimaryKey val dateStr: String,
    val year: Int,
    val monthNum: Int,
    val monthName: String,
    // AES-256 encrypted JSON string (encoded in Hex)
    val ciphertext: String,
)

private val HEX_CHARS = "0123456789abcdef".toCharArray()

fun ByteArray.toHex(): String {
    val result = StringBuilder(size * 2)
    for (b in this) {
        val i = b.toInt() and 0xFF
        result.append(HEX_CHARS[i shr 4])
        result.append(HEX_CHARS[i and 0x0F])
    }
    return result.toString()
}

fun String.hexToByteArray(): ByteArray {
    val result = ByteArray(length / 2)
    for (i in 0 until length step 2) {
        val firstDigit = toCharDigit(this[i])
        val secondDigit = toCharDigit(this[i + 1])
        result[i / 2] = ((firstDigit shl 4) + secondDigit).toByte()
    }
    return result
}

private fun toCharDigit(ch: Char): Int {
    return when (ch) {
        in '0'..'9' -> ch - '0'
        in 'a'..'f' -> ch - 'a' + 10
        in 'A'..'F' -> ch - 'A' + 10
        else -> throw IllegalArgumentException("Invalid hex character: $ch")
    }
}

fun ParsedPayslip.toEncryptedEntity(password: String = CryptoHelper.getDatabaseSecretKey()): EncryptedPayslipEntity = toEncryptedEntity(PasswordKey(password))

/** The same row sealed with [key]: a password per row (the device, older backups) or a v6 backup's archive key. */
fun ParsedPayslip.toEncryptedEntity(key: SealKey): EncryptedPayslipEntity {
    val jsonString = Json.encodeToString(ParsedPayslip.serializer(), this)
    val jsonBytes = jsonString.encodeToByteArray()
    val encryptedBytes = key.seal(jsonBytes).getOrThrow()
    val ciphertext = encryptedBytes.toHex()
    return EncryptedPayslipEntity(
        dateStr = dateStr,
        year = year,
        monthNum = monthNum,
        monthName = monthName,
        ciphertext = ciphertext,
    )
}

fun EncryptedPayslipEntity.toDomain(password: String = CryptoHelper.getDatabaseSecretKey()): ParsedPayslip = toDomain(PasswordKey(password))

fun EncryptedPayslipEntity.toDomain(key: SealKey): ParsedPayslip {
    val encryptedBytes = ciphertext.hexToByteArray()
    val decryptedBytes =
        try {
            key.open(encryptedBytes).getOrThrow()
        } catch (primaryEx: Exception) {
            try {
                CryptoHelper.decrypt(encryptedBytes, CryptoHelper.getLegacyFallbackKey()).getOrThrow()
            } catch (legacyEx: Exception) {
                Logger.w("EncryptedPayslipEntity", "Cannot decrypt $dateStr with device key or legacy key - ${legacyEx.message}")
                throw legacyEx
            }
        }
    val jsonString = decryptedBytes.decodeToString()
    return BackupJson.decode(ParsedPayslip.serializer(), jsonString)
}
