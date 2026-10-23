package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.VaultOrder
import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.PortableBackup
import com.payslipmax.pdfparser.repository.backupBytes
import com.payslipmax.pdfparser.repository.goodEntity
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

/** Helpers for the Part II backup tests: rows as the device stores them, rows as an archive carries them, and archives made by hand. */
internal val deviceKey get() = CryptoHelper.getDatabaseSecretKey()

internal suspend fun FakePayslipDao.putDraft(draft: Part2Draft) = part2.upsertDraft(draft.toEntity(deviceKey))

internal suspend fun FakePayslipDao.putOrder(order: VaultOrder) = part2.upsertOrder(order.toEntity(deviceKey))

internal suspend fun FakePayslipDao.putPage(
    id: String,
    orderId: String,
    position: Int,
    jpeg: ByteArray,
) = part2.upsertPage(sealPage(id, orderId, position, jpeg, deviceKey))

internal fun text(bytes: ByteArray): String = Base64.encode(bytes)

internal fun draftRow(
    draft: Part2Draft,
    password: String = BACKUP_PASSWORD,
) = Part2DraftBackup(draft.id, text(draft.toEntity(password).ciphertext))

internal fun orderRow(
    order: VaultOrder,
    password: String = BACKUP_PASSWORD,
) = Part2OrderBackup(order.id, text(order.toEntity(password).ciphertext))

/** A page record as an archive carries it: [sealed] is the ciphertext, made for the backup password unless a test crafts otherwise. */
internal class RawPage(
    val id: String,
    val orderId: String,
    val position: Int,
    val sealed: ByteArray,
)

internal fun pageRow(
    id: String,
    orderId: String,
    position: Int,
    jpeg: ByteArray,
    password: String = BACKUP_PASSWORD,
) = RawPage(id, orderId, position, sealPage(id, orderId, position, jpeg, password).ciphertext)

/**
 * An archive by hand. Without pages it is the one encrypted document; with pages it is a `PageArchive` whose trailer says [declared]
 * pages (the real count unless a test makes the file lie).
 */
internal fun archive(
    drafts: List<Part2DraftBackup> = emptyList(),
    orders: List<Part2OrderBackup> = emptyList(),
    pages: List<RawPage> = emptyList(),
    version: Int = PortableBackup.CURRENT_VERSION,
    declared: Int = pages.size,
): ByteArray {
    val backup = PortableBackup(version, listOf(goodEntity("08/2024")), emptyList(), null, part2Drafts = drafts, part2Orders = orders)
    if (pages.isEmpty()) return backupBytes(backup)
    val meta = CryptoHelper.encrypt(Json.encodeToString(PortableBackup.serializer(), backup).encodeToByteArray(), BACKUP_PASSWORD).getOrThrow()
    val writer = PageArchiveWriter(meta.size + pages.sumOf { it.sealed.size + 200 } + 400)
    writer.begin(meta)
    pages.forEach { writer.add(it.id, it.orderId, it.position, it.sealed) }
    return writer.finish(declared, BACKUP_PASSWORD)
}

/** The metadata document of either kind of archive. */
internal fun decodeArchive(archive: ByteArray): PortableBackup =
    Json.decodeFromString(
        PortableBackup.serializer(),
        CryptoHelper.decrypt(PageArchive.metaOf(archive) ?: archive, BACKUP_PASSWORD).getOrThrow().decodeToString(),
    )

/** A row no one can read: it costs nothing to make, so a test can ask for thousands. */
internal fun junkDraft(i: Int) = Part2DraftBackup("junk-$i", "!!")

internal fun junkPage(i: Int) = RawPage("junk-$i", "o1", 0, ByteArray(2))
