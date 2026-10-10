package com.payslipmax.pdfparser.di

import com.payslipmax.pdfparser.database.PayslipDao
import com.payslipmax.pdfparser.parser.PdfParser
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.ui.screens.guide.GuideNotesViewModel
import com.payslipmax.pdfparser.ui.screens.guide.GuideSearchViewModel
import com.payslipmax.pdfparser.ui.screens.guide.isGuideEnabled
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * WHY: both platforms start Koin from [appKoinModules], and the notes model and the search model that reads it are new
 * bindings. A binding missing on one platform would otherwise surface only when the Guide first opens. The notes model
 * must also be one app-scoped instance, so every screen sees the same notes. In its own class, apart from [AppKoinModulesTest],
 * which builds PayslipViewModels; this builds nothing that needs a Main dispatcher.
 */
class GuideNotesGraphTest {
    private val leaves =
        module {
            single<PayslipDao> { GuideKoinTestDao.dao }
            single<PdfParser> { FakePdfParser() }
        }
    private val koin = koinApplication { modules(appKoinModules(platformModules = listOf(leaves))) }.koin

    @Test
    fun theNotesAndSearchModelsResolveExactlyWhereTheGuideIsEnabled() {
        assertEquals(isGuideEnabled(), koin.getOrNull<GuideNotesViewModel>() != null)
        assertEquals(isGuideEnabled(), koin.getOrNull<GuideSearchViewModel>() != null)
    }

    @Test
    fun theNotesModelIsAppScopedSoEveryScreenSeesTheSameNotes() {
        if (!isGuideEnabled()) return

        assertSame(koin.get<GuideNotesViewModel>(), koin.get<GuideNotesViewModel>())
    }
}
