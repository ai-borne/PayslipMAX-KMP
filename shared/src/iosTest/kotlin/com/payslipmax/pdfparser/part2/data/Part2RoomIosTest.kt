package com.payslipmax.pdfparser.part2.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.database.PayslipDatabase
import com.payslipmax.pdfparser.database.PayslipDatabaseConstructor
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * WHY: the JVM test (`Part2RoomTest`) cannot show that the vault works on Kotlin/Native, where the key comes from the iOS Keychain,
 * AES-GCM runs through CommonCrypto, a BLOB column goes through the bundled SQLite driver and Room's generated code is compiled by KSP
 * for the simulator. This runs the production schema in an in-memory database with the real Keychain key.
 */
class Part2RoomIosTest {
    private lateinit var database: PayslipDatabase
    private lateinit var repository: RoomPart2Repository
    private val jpeg = SyntheticPart2Drafts.jpeg(40_000, 5)

    @BeforeTest
    fun setUp() {
        database = newDatabase()
        repository = RoomPart2Repository(database.part2Dao(), dispatcher = Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() = database.close()

    private fun newDatabase(): PayslipDatabase =
        Room
            .inMemoryDatabaseBuilder<PayslipDatabase>(factory = { PayslipDatabaseConstructor.initialize() })
            .setDriver(BundledSQLiteDriver())
            .build()

    private suspend fun fill(): String {
        repository.saveDraft(SyntheticPart2Drafts.fullDraft())
        repository.saveOrder(SyntheticPart2Drafts.order())
        return assertNotNull(repository.addPage("order-1", jpeg).pageId)
    }

    @Test
    fun aDraftAnOrderAndAPageRoundTripUnderTheKeychainKey() =
        runTest {
            val pageId = fill()

            assertEquals(SyntheticPart2Drafts.fullDraft(), repository.draft("draft-1"))
            assertContentEquals(jpeg, repository.readPage(pageId) { it.copyOf() })
        }

    @Test
    fun theStoredRowsHoldNoPlaintext() =
        runTest {
            val pageId = fill()

            val dao = database.part2Dao()
            val text = dao.allDrafts().single().ciphertext.decodeToString() + dao.allOrders().single().ciphertext.decodeToString()
            assertFalse(text.contains(SyntheticPart2Drafts.MARKER_TEXT) || text.contains(SyntheticPart2Drafts.MARKER_ORDER_NO) || text.contains("SYNCODE"))
            assertNotNull(dao.page(pageId))
        }

    @Test
    fun deletingAnOrderDeletesItsPagesAndTheCapsHold() =
        runTest {
            fill()
            repeat(Part2Limits.PAGES_PER_ORDER_MAX - 1) { assertEquals(Part2WriteResult.SAVED, repository.addPage("order-1", SyntheticPart2Drafts.jpeg(100, it)).result) }
            assertEquals(Part2WriteResult.TOO_MANY, repository.addPage("order-1", SyntheticPart2Drafts.jpeg(100)).result)

            repository.deleteOrder("order-1")

            assertEquals(0, database.part2Dao().allPageRefs().size)
        }

    @Test
    fun aBackupCarriesTheVaultToAFreshDatabaseUnderTheBackupPasswordNotTheDeviceKey() =
        runTest {
            val pageId = fill()
            val service = PayslipBackupService(database.payslipDao(), Dispatchers.Unconfined, part2Backup = Part2Backup(database.part2Dao()))
            val archive = service.export(BACKUP_PASSWORD).getOrThrow()
            assertTrue(CryptoHelper.decrypt(archive, CryptoHelper.getDatabaseSecretKey()).isFailure, "the archive is under the password")

            val fresh = newDatabase()
            try {
                PayslipBackupService(fresh.payslipDao(), Dispatchers.Unconfined, part2Backup = Part2Backup(fresh.part2Dao()))
                    .restore(archive, BACKUP_PASSWORD, RestoreMode.REPLACE)
                    .getOrThrow()

                val restored = RoomPart2Repository(fresh.part2Dao(), dispatcher = Dispatchers.Unconfined)
                assertEquals(SyntheticPart2Drafts.fullDraft(), restored.draft("draft-1"))
                assertContentEquals(jpeg, restored.readPage(pageId) { it.copyOf() })
                fresh.payslipDao().clearAllUserData()
                assertEquals(0, fresh.part2Dao().allDrafts().size)
            } finally {
                fresh.close()
            }
        }

    @Test
    fun theSealOverheadIsWhatTheKeychainBackedHelperAdds() {
        assertEquals(Part2Limits.SEAL_OVERHEAD_BYTES, CryptoHelper.encrypt(ByteArray(0), "k").getOrThrow().size)
    }
}
