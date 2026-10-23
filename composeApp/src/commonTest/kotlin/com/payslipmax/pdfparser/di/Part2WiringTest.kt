package com.payslipmax.pdfparser.di

import com.payslipmax.pdfparser.database.PayslipDao
import com.payslipmax.pdfparser.parser.PdfParser
import com.payslipmax.pdfparser.part2.data.Part2Backup
import com.payslipmax.pdfparser.part2.data.Part2Dao
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.RoomPart2Repository
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2LookupViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2Drafting
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2DraftsViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2Models
import com.payslipmax.pdfparser.ui.screens.guide.part2.isPart2Enabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

/**
 * WHY: two promises that no other test notices if a binding changes. (1) The Part II repository exists exactly where the feature
 * does, so a flag-off build binds nothing new for the screens. (2) The production backup service is built WITH its Part II backup,
 * flag or no flag: leaving the argument out would still compile, and every backup would silently drop the vault.
 */
class Part2WiringTest {
    private val partDao = GuideKoinTestDao.dao.part2
    private val leaves =
        module {
            single<PayslipDao> { GuideKoinTestDao.dao }
            single<Part2Dao> { partDao }
            single<PdfParser> { FakePdfParser() }
        }

    @Test
    fun theRepositoryIsBoundExactlyWhereTheFeatureIsEnabled() {
        val koin = koinApplication { modules(appKoinModules(platformModules = listOf(leaves))) }.koin

        assertEquals(isPart2Enabled(), koin.getOrNull<Part2Repository>() != null)
    }

    @Test
    fun theLookupModelIsBoundExactlyWhereTheFeatureIsEnabled() {
        val koin = koinApplication { modules(appKoinModules(platformModules = listOf(leaves))) }.koin

        assertEquals(isPart2Enabled(), koin.getOrNull<Part2LookupViewModel>() != null)
    }

    @Test
    fun theModuleGivesOneLookupModelSoTheQuerySurvivesATabSwitchLikeTheGuidesOwnSearch() {
        val guide = module { single { GuideViewModel(FakeGuideRepository(), FakeCrashReporter()) } }
        val koin = koinApplication { modules(part2Module, leaves, guide) }.koin

        assertSame(koin.get<Part2LookupViewModel>(), koin.get<Part2LookupViewModel>())
    }

    @Test
    fun thePartTwoModelsAreBoundExactlyWhereTheFeatureIsEnabled() {
        val koin = koinApplication { modules(appKoinModules(platformModules = listOf(leaves))) }.koin

        assertEquals(isPart2Enabled(), koin.getOrNull<Part2Models>() != null)
    }

    @Test
    fun theModuleHandsTheGuideOneHolderOverTheSameLookupDraftsListAndDraftMaker() {
        // why: the Guide tab is given one `Part2Models`; if it held its own copies, the drafts list and the open draft would see different state.
        val guide = module { single { GuideViewModel(FakeGuideRepository(), FakeCrashReporter()) } }
        val koin = koinApplication { modules(part2Module, leaves, guide) }.koin
        val models = koin.get<Part2Models>()

        assertSame(koin.get<Part2LookupViewModel>(), models.lookup)
        assertSame(koin.get<Part2DraftsViewModel>(), models.drafts)
        assertSame(koin.get<Part2Drafting>(), models.drafting)
        assertSame(models, koin.get<Part2Models>())
        assertEquals(null, models.drafting.open("any-id"), "the Guide has not loaded, so there is nothing to draft from and no model")
    }

    @Test
    fun theModuleGivesOneRepositoryOverTheTablesTheBackupReadsAndStoresEncrypted() =
        runBlocking {
            val koin = koinApplication { modules(part2Module, leaves) }.koin
            val repository = koin.get<Part2Repository>()
            assertSame(repository, koin.get<Part2Repository>())

            repository.saveDraft(SyntheticPart2Drafts.fullDraft("wired"))

            assertFalse(partDao.draft("wired")!!.ciphertext.decodeToString().contains(SyntheticPart2Drafts.MARKER_TEXT))
            assertEquals(SyntheticPart2Drafts.fullDraft("wired"), repository.draft("wired"))
            partDao.deleteDraft("wired")
        }

    @Test
    fun theProductionBackupServiceCarriesTheVault() =
        runBlocking {
            val koin = koinApplication { modules(appKoinModules(platformModules = listOf(leaves))) }.koin
            RoomPart2Repository(partDao, dispatcher = Dispatchers.Unconfined).saveDraft(SyntheticPart2Drafts.fullDraft("carried"))

            val archive = koin.get<PayslipBackupService>().export("pw-for-test").getOrThrow()
            val elsewhere = FakePayslipDao()
            PayslipBackupService(elsewhere, Dispatchers.Unconfined, part2Backup = Part2Backup(elsewhere.part2))
                .restore(archive, "pw-for-test", RestoreMode.REPLACE)
                .getOrThrow()

            assertEquals(listOf("carried"), elsewhere.part2.allDrafts().map { it.id })
            partDao.deleteDraft("carried")
        }
}
