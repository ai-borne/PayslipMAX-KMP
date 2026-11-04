package com.payslipmax.pdfparser.repository.backup

import com.payslipmax.pdfparser.crypto.Secret
import com.payslipmax.pdfparser.database.AppSettingsEntity
import com.payslipmax.pdfparser.database.PayslipDao
import com.payslipmax.pdfparser.repository.MemoryBackupSource
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
    val plain = openArchive(MemoryBackupSource(archive), Secret.of(password)).use { it.document.decodeToString() }
    return Json.parseToJsonElement(plain).jsonObject
}

/** What [dao] exports today, as raw JSON. */
internal suspend fun exportedDocument(
    dao: PayslipDao,
    password: String,
): JsonObject = decryptedDocument(PayslipBackupService(dao, Dispatchers.Unconfined).export(Secret.of(password)).getOrThrow(), password)

/** Every class reachable from [root] in the wire format, by serial name (nullability dropped), each with its decoded keys. */
internal fun wireClasses(root: SerialDescriptor): Map<String, Set<String>> = classesIn(root) { it.keys { true } }

/** Every class reachable from [root], each with the keys a reader cannot do without (those with no default value). */
@OptIn(ExperimentalSerializationApi::class)
internal fun requiredKeys(root: SerialDescriptor): Map<String, Set<String>> = classesIn(root) { d -> d.keys { !d.isElementOptional(it) } }

private fun SerialDescriptor.keys(keep: (Int) -> Boolean): Set<String> = (0 until elementsCount).filter(keep).map { getElementName(it) }.toSet()

/** Walks the serializer descriptors from [root] (no reflection) and describes every class it reaches once, by serial name. */
@OptIn(ExperimentalSerializationApi::class)
private fun <T> classesIn(
    root: SerialDescriptor,
    describe: (SerialDescriptor) -> T,
): Map<String, T> {
    val found = linkedMapOf<String, T>()

    fun visit(descriptor: SerialDescriptor) {
        val name = descriptor.serialName.removeSuffix("?")
        if (descriptor.kind == StructureKind.CLASS) {
            if (name in found) return
            found[name] = describe(descriptor)
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
