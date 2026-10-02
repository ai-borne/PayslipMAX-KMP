package com.payslipmax.pdfparser.repository

import android.content.Context
import androidx.room.useWriterConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.payslipmax.pdfparser.crypto.ContextHolder
import com.payslipmax.pdfparser.database.PayslipDatabase
import com.payslipmax.pdfparser.database.PayslipPdfEntity
import com.payslipmax.pdfparser.database.getDatabaseBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: the fake DAO cannot prove atomicity, because it has no transactions. These tests run the restore
 * against the real production Room database to show that (a) REPLACE really empties every table, and
 * (b) a write that fails part-way through rolls the whole restore back, leaving the device's data as it was.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PayslipBackupServiceRoomTest {
    private lateinit var database: PayslipDatabase
    private lateinit var service: PayslipBackupService

    @Before
    fun setUp() {
        ContextHolder.context = ApplicationProvider.getApplicationContext<Context>()
        // Production builder so the schema under test is the shipped one; only the driver is swapped
        // because the bundled native driver cannot load on the host JVM.
        database = getDatabaseBuilder().setDriver(AndroidSQLiteDriver()).build()
        service = PayslipBackupService(database.payslipDao(), Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        database.close()
        ContextHolder.context = null
    }

    @Test
    fun `REPLACE empties every user table of the real database`() =
        runBlocking {
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
    fun `a write that fails part-way rolls the whole REPLACE restore back`() =
        runBlocking {
            val dao = database.payslipDao()
            dao.seedDeviceMonth("01/2023")
            // The last write of a restore is the PDF insert; make it fail so everything before it has
            // already run (the wipe and the payslip insert) when the failure hits.
            database.useWriterConnection {
                it.usePrepared(
                    "CREATE TRIGGER fail_pdf BEFORE INSERT ON payslip_pdfs " +
                        "BEGIN SELECT RAISE(ABORT, 'simulated failure'); END",
                ) { statement -> statement.step() }
            }
            val backup =
                backupBytes(
                    PortableBackup(2, listOf(goodEntity("08/2024")), listOf(PayslipPdfEntity("08/2024", byteArrayOf(9))), null),
                )

            val result = service.restore(backup, BACKUP_PASSWORD, RestoreMode.REPLACE)

            assertTrue(result.isFailure)
            assertEquals(listOf("01/2023"), dao.getAllPayslips().first().map { it.dateStr })
            assertEquals(listOf("01/2023"), dao.getAllPdfs().map { it.dateStr })
            assertEquals(1, dao.getAllLedgerRecords().first().size)
            assertEquals(1, dao.getAllFinancialInsights().first().size)
            assertEquals(1, dao.getAllRepresentationDrafts().first().size)
            assertEquals(1, dao.getAllDismissedDrafts().size)
            assertEquals(1, dao.getAllCorrections().first().size)
        }
}
