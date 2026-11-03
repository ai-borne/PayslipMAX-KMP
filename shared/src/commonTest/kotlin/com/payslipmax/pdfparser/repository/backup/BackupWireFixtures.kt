package com.payslipmax.pdfparser.repository.backup

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.database.AppSettingsEntity
import com.payslipmax.pdfparser.database.PayslipDao
import com.payslipmax.pdfparser.part2.data.PageArchive
import com.payslipmax.pdfparser.repository.PayslipBackupService
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/** The archive's document as raw JSON, so a test sees the keys on the wire and not what a decoder kept. */
internal fun decryptedDocument(
    archive: ByteArray,
    password: String,
): JsonObject {
    val meta = PageArchive.metaOf(archive) ?: archive
    val plain = CryptoHelper.decrypt(meta, password).getOrThrow().decodeToString()
    return Json.parseToJsonElement(plain).jsonObject
}

/** What [dao] exports today, as raw JSON. */
internal suspend fun exportedDocument(
    dao: PayslipDao,
    password: String,
): JsonObject = decryptedDocument(PayslipBackupService(dao, Dispatchers.Unconfined).export(password).getOrThrow(), password)

/** Every class reachable from [root] in the wire format, by serial name (nullability dropped), each with its decoded keys. */
@OptIn(ExperimentalSerializationApi::class)
internal fun wireClasses(root: SerialDescriptor): Map<String, Set<String>> {
    val found = linkedMapOf<String, Set<String>>()

    fun visit(descriptor: SerialDescriptor) {
        val name = descriptor.serialName.removeSuffix("?")
        if (descriptor.kind == StructureKind.CLASS) {
            if (name in found) return
            found[name] = (0 until descriptor.elementsCount).map { descriptor.getElementName(it) }.toSet()
        }
        (0 until descriptor.elementsCount).forEach { visit(descriptor.getElementDescriptor(it)) }
    }
    visit(root)
    return found
}

/** Device-only settings (D5) all set away from their defaults, so an export that leaked any of them would show its key. */
internal val LOCKED_PREMIUM_DEVICE =
    AppSettingsEntity(isPremiumEnabled = true, isLockEnabled = true, appPinHash = "device-pin-hash", isTelemetryEnabled = false)

/** The D5 fields: they belong to the device and its store account, never to a backup. */
internal val DEVICE_ONLY_KEYS = setOf("isPremiumEnabled", "isLockEnabled", "appPinHash", "isTelemetryEnabled")
