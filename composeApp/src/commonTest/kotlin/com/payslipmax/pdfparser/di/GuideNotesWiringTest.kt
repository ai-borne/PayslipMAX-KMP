package com.payslipmax.pdfparser.di

import com.payslipmax.pdfparser.database.PayslipDao
import com.payslipmax.pdfparser.guide.data.GuideNotesRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

/**
 * WHY: the personal-notes repository must be one instance (every screen sees one list) over the same [PayslipDao] the backup
 * service exports from, and must store through the production encryption. A binding wired to another DAO would keep notes
 * out of the `.pcda` backup without any other test noticing. Kept apart from [AppKoinModulesTest] on purpose: that class
 * runs ViewModels whose background collection races its `resetMain`, and adding a test there changed which later test
 * the leaked error landed in. This class touches no Main dispatcher.
 */
class GuideNotesWiringTest {
    private val dao = FakePayslipDao()
    private val koin = koinApplication { modules(guideModule, module { single<PayslipDao> { dao } }) }.koin

    @Test
    fun theNotesRepositoryIsOneInstanceOverTheDaoTheBackupReadsAndStoresEncrypted() =
        runBlocking {
            val repository = koin.get<GuideNotesRepository>()
            assertSame(repository, koin.get<GuideNotesRepository>())

            repository.save("RB-TD-001", "private words", "rev00001")

            val row = dao.getAllGuideNotes().first().single()
            assertEquals("RB-TD-001", row.cardId)
            assertFalse(row.ciphertext.contains("private"), "stored encrypted through the production wiring")
            assertEquals("private words", repository.observe().first().notes.single().text)
        }
}
