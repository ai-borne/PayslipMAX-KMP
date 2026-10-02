package com.payslipmax.pdfparser.repository

import androidx.room.Room
import androidx.room.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.payslipmax.pdfparser.database.DismissedDraftEntity
import com.payslipmax.pdfparser.database.PayslipDatabase
import com.payslipmax.pdfparser.database.PayslipDatabaseConstructor
import com.payslipmax.pdfparser.database.PayslipPdfEntity
import com.payslipmax.pdfparser.database.RepresentationDraftEntity
import com.payslipmax.pdfparser.database.toCorrectionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: the fake DAO has no transactions, and the Android twin of this test (`PayslipBackupServiceRoomTest`)
 * runs on the JVM, so neither shows that a restore is atomic on Kotlin/Native, where the same `@Transaction`
 * methods run on the bundled SQLite driver. This runs the production schema in an in-memory database on iOS
 * and shows that (a) REPLACE really empties every table and (b) a write that fails part-way rolls the whole
 * restore back, leaving the device's data as it was.
 */
class PayslipBackupServiceRoomIosTest {
    private lateinit var database: PayslipDatabase
    private lateinit var service: PayslipBackupService

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<PayslipDatabase>(factory = { PayslipDatabaseConstructor.initialize() })
                .setDriver(BundledSQLiteDriver())
                .build()
        service = PayslipBackupService(database.payslipDao(), Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() = database.close()

    @Test
    fun replaceEmptiesEveryUserTableOfTheRealDatabase() =
        runTest {
            val dao = database.payslipDao()
            dao.seedDeviceMonth("01/2023")

            service.restore(backupOf("08/2024"), BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()

            assertEquals(listOf("08/2024"), dao.getAllPayslips().first().map { it.dateStr })
            assertEquals(listOf("08/2024"), dao.getAllPdfs().map { it.dateStr })
            assertTrue(dao.getAllLedgerRecords().first().isEmpty())
            assertTrue(dao.getAllFinancialInsights().first().isEmpty())
            assertTrue(dao.getAllRepresentationDrafts().first().isEmpty())
            assertTrue(dao.getAllDismissedDrafts().isEmpty())
            assertTrue(dao.getAllCorrections().first().isEmpty())
        }

    @Test
    fun aWriteThatFailsPartWayRollsTheWholeReplaceRestoreBack() =
        runTest {
            val dao = database.payslipDao()
            dao.seedDeviceMonth("01/2023")
            // The last write of a restore is the correction insert; make it fail so everything before it
            // has already run (the wipe, payslips, PDFs, letters) when the failure hits.
            database.useWriterConnection {
                it.usePrepared(
                    "CREATE TRIGGER fail_correction BEFORE INSERT ON payslip_corrections " +
                        "BEGIN SELECT RAISE(ABORT, 'simulated failure'); END",
                ) { statement -> statement.step() }
            }
            val backup =
                backupBytes(
                    PortableBackup(
                        version = 3,
                        encryptedPayslips = listOf(goodEntity("08/2024")),
                        pdfs = listOf(PayslipPdfEntity("08/2024", byteArrayOf(9))),
                        settings = null,
                        drafts = listOf(RepresentationDraftEntity("d-new", "08/2024", "MISSING_HRA", "PCDA_O_PUNE", "s", "b", 1L)),
                        dismissedDrafts = listOf(DismissedDraftEntity("08/2024", "SALARY_DROP")),
                        corrections = listOf(mapOf("basicPay" to 2.0).toCorrectionEntity("08/2024", BACKUP_PASSWORD)),
                    ),
                )

            val result = service.restore(backup, BACKUP_PASSWORD, RestoreMode.REPLACE)

            assertTrue(result.isFailure)
            assertEquals(listOf("01/2023"), dao.getAllPayslips().first().map { it.dateStr })
            assertEquals(listOf("01/2023"), dao.getAllPdfs().map { it.dateStr })
            assertEquals(1, dao.getAllLedgerRecords().first().size)
            assertEquals(1, dao.getAllFinancialInsights().first().size)
            assertEquals(listOf("d-01/2023"), dao.getAllRepresentationDrafts().first().map { it.id })
            assertEquals(1, dao.getAllDismissedDrafts().size)
            assertEquals(1, dao.getAllCorrections().first().size)
        }
}
