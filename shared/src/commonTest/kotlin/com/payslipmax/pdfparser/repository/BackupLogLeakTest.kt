package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.database.EncryptedPayslipEntity
import com.payslipmax.pdfparser.database.PayslipCorrectionEntity
import com.payslipmax.pdfparser.database.toDomain
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.telemetry.CrashReporter
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P6, section 4; P10 "From P1" item 2, "From P5" item 6): every `Logger` line is a Crashlytics breadcrumb, and a
 * `Logger.e` with an exception also sends that exception, message and all, as a non-fatal. A row that would not open used to log its
 * month and the crypto or kotlinx message (which can quote the payslip's JSON), once per skipped row since P5. Each of these sites now
 * logs one fixed line: no month, no exception text, no row, and no exception object at all.
 */
class BackupLogLeakTest {
    private val reporter = FakeCrashReporter()
    private val month = "03/2024"
    private val leakyText = "ABCDE1234F net 98765.43 BAD_DECRYPT"

    // Logger sends to the CrashReporter Koin binds, as in the app; here that is the fake.
    @BeforeTest
    fun setUp() {
        stopKoin()
        startKoin { modules(module { single<CrashReporter> { reporter } }) }
    }

    @AfterTest
    fun tearDown() = stopKoin()

    /** A payslip row and a corrections row for [month] that no key opens. */
    private suspend fun FakePayslipDao.seedUnreadableMonth() {
        insertPayslip(EncryptedPayslipEntity(month, 2024, 3, "March", "00".repeat(64)))
        insertCorrection(PayslipCorrectionEntity(month, "00".repeat(64)))
    }

    private fun assertNothingLeaked(vararg alsoAbsent: String) {
        assertTrue(reporter.logs.isNotEmpty(), "the site still leaves a breadcrumb")
        assertTrue(reporter.exceptions.isEmpty(), "no exception object is sent: its message can carry a row")
        val sent = reporter.logs + reporter.keys.values
        for (piece in listOf(month, "2024", "ABCDE1234F", "98765", "BAD_DECRYPT") + alsoAbsent) {
            assertTrue(sent.none { piece in it }, "'$piece' reached a breadcrumb: $sent")
        }
    }

    @Test
    fun aDeviceRowThatWillNotOpenLogsNeitherItsMonthNorTheCryptoMessage() {
        val failure = runCatching { EncryptedPayslipEntity(month, 2024, 3, "March", "00".repeat(64)).toDomain() }.exceptionOrNull()
        assertTrue(failure != null)

        assertNothingLeaked(failure.message!!)
    }

    @Test
    fun aBackupThatSkipsAnUnreadablePayslipAndCorrectionLogsFixedLinesOnly() =
        runTest {
            val dao = FakePayslipDao().apply { seedUnreadableMonth() }
            dao.seedDeviceMonth("08/2024")

            PayslipBackupService(dao, Dispatchers.Unconfined).export(BACKUP_PASSWORD).getOrThrow()

            assertNothingLeaked()
        }

    @Test
    fun theDeviceReadThatSkipsAnUnreadablePayslipLogsAFixedLineOnly() =
        runTest {
            val dao = FakePayslipDao().apply { seedUnreadableMonth() }

            assertEquals(emptyList(), PayslipRepository(dao, FakePdfParser(), Dispatchers.Unconfined).getAllPayslips().first())
            assertNothingLeaked()
        }

    @Test
    fun aRebuildThatFailsAfterARestoreLogsAFixedLineOnly() =
        runTest {
            val source = FakePayslipDao().apply { seedDeviceMonth("08/2024") }
            val target = FakePayslipDao()
            val file = PayslipBackupService(source, Dispatchers.Unconfined).export(BACKUP_PASSWORD).getOrThrow()
            reporter.logs.clear()

            PayslipBackupService(target, Dispatchers.Unconfined, PayslipRepository(target, FakePdfParser(), Dispatchers.Unconfined), failingIntelligence(target))
                .restore(file, BACKUP_PASSWORD)
                .getOrThrow()

            assertNothingLeaked(leakyText)
        }

    @Test
    fun anAuditRepairThatFailsLogsAFixedLineOnly() =
        runTest {
            val dao = FakePayslipDao()

            val ran = failingIntelligence(dao).repairAuditHistoryIfIncomplete(listOf(createMockPayslip(month)))

            assertEquals(false, ran)
            assertNothingLeaked(leakyText)
        }

    private fun failingIntelligence(dao: FakePayslipDao) =
        object : FinancialIntelligenceRepository(dao, Dispatchers.Unconfined) {
            override suspend fun rebuildAuditHistory(payslips: List<ParsedPayslip>) = error("$leakyText for $month")
        }
}
