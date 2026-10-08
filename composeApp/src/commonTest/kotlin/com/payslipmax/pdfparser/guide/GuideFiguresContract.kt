package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.guide.domain.GuideProfile
import com.payslipmax.pdfparser.guide.domain.GuideProfileBuilder
import com.payslipmax.pdfparser.guide.domain.PersonalFigure
import com.payslipmax.pdfparser.guide.domain.PersonalFigureResolver
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.insights.timeline.PayLevel
import com.payslipmax.pdfparser.insights.timeline.TptaCityClass
import com.payslipmax.pdfparser.testing.SyntheticGuideFigures
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Contract between the shipped "your figure" data (`figures.json`, as bundled) and the app, on the real bundle. It states
 * the amounts the approved letters imply, so a wrong figure in the data fails here even though no rupee figure is in
 * production Kotlin. It also pins the one place two sources could drift: Pay Audit's own Transport Allowance bases.
 * Run on each platform (`GuideBundleAndroidContractTest`, `GuideLoaderIosPerfTest`).
 */
object GuideFiguresContract {
    private val personalCards = setOf("RB-SS-T181", "RB-SS-T254", "RB-SS-P051-rates", "RB-SS-P116-rates")

    private fun profile(
        level: String? = "14",
        basic: Double? = 144200.0,
        da: Int? = 60,
        tpta: String? = null,
        hra: Double? = null,
    ) = GuideProfile(2026, 6, level, basic, da, tpta, hra)

    private fun GuideBundle.figureFor(
        card: String,
        profile: GuideProfile,
    ) = PersonalFigureResolver.resolve(card, figures, profile)

    fun assertShippedFiguresMatchTheApprovedLetters(bundle: GuideBundle) {
        val figures = assertNotNull(bundle.figures, "the bundle ships figures")
        assertEquals(setOf("food_rate", "ctg", "transport_allowance", "hra"), figures.figures.keys)
        assertEquals(SyntheticGuideFigures.figures, figures, "the unit-test copy of the figures must equal what ships, or its oracle proves nothing")
        assertEquals(personalCards, figures.figures.values.map { it.card }.toSet())
        assertEquals(personalCards, bundle.cards.filter { it.personal.isNotEmpty() }.map { it.id }.toSet(), "every personal card has a figure")
        assertEquals(50 to 25, figures.daStep.perDaPercent to figures.daStep.increasePercent)

        // Food (MoD 15-09-2017): the FAQ's printed rates at DA 60%.
        assertEquals(1125L, (bundle.figureFor("RB-SS-T181", profile(level = "11")) as PersonalFigure.FoodRate).amount)
        assertEquals(1250L, (bundle.figureFor("RB-SS-T181", profile(level = "12A")) as PersonalFigure.FoodRate).amount)
        assertEquals(1500L, (bundle.figureFor("RB-SS-T181", profile(level = "14")) as PersonalFigure.FoodRate).amount)
        // CTG: 80% of basic.
        assertEquals(80000L, (bundle.figureFor("RB-SS-T254", profile(basic = 100000.0)) as PersonalFigure.Ctg).amount)
        // Transport: base plus DA.
        assertEquals(11520L, (bundle.figureFor("RB-SS-P051-rates", profile(level = "12A", tpta = "HIGHER")) as PersonalFigure.Transport).amount)
        assertEquals(25200L, (bundle.figureFor("RB-SS-P051-rates", profile(level = "14")) as PersonalFigure.Transport).amount)
        // HRA: class X at DA 60% is 30%.
        val hra = assertIs<PersonalFigure.Hra>(bundle.figureFor("RB-SS-P116-rates", profile(basic = 100000.0, hra = 30000.0)))
        assertEquals("X" to 30, hra.hraClass to hra.percent)
    }

    /** No officer level Pay Audit knows is left without a food rate or a Transport Allowance rate. */
    fun assertEveryPayLevelHasARate(bundle: GuideBundle) {
        for (level in PayLevel.entries) {
            assertNotNull(bundle.figureFor("RB-SS-T181", profile(level = level.label)), "no food rate for level ${level.label}")
            val classes = if (level <= PayLevel.L13A) listOf("HIGHER", "OTHER") else listOf(null)
            for (city in classes) {
                assertNotNull(bundle.figureFor("RB-SS-P051-rates", profile(level = level.label, tpta = city)), "no transport rate for ${level.label} $city")
            }
        }
    }

    /** The bundle's Transport Allowance bases must equal the ones Pay Audit reads payslips with, so the two cannot drift. */
    fun assertTransportBasesMatchPayAudit(bundle: GuideBundle) {
        val bands = assertNotNull(bundle.figures).figures.getValue("transport_allowance").bands
        for (city in TptaCityClass.entries) {
            val band = bands.single { it.cityClass == city.name }
            assertEquals(city.baseRate, band.base.toDouble(), "Pay Audit's ${city.name} base and the Guide's must be one number")
        }
    }

    /** A realistic history (about 12 years of months) through the profile builder and all four cards, for the Native timing. */
    fun realisticHistory(months: Int = 140): List<ParsedPayslip> =
        (0 until months).map { n ->
            ParsedPayslip(
                file = "t.pdf",
                year = 2015 + n / 12,
                monthNum = n % 12 + 1,
                monthName = "",
                dateStr = "${n % 12 + 1}/${2015 + n / 12}",
                officer = Officer("N", "A", "P"),
                earnings =
                    Earnings(
                        basicPay = 121200.0,
                        militaryServicePay = 15500.0,
                        dearnessAllowance = 82020.0,
                        transportAllowance = 7200.0,
                        transportAllowanceDa = 4320.0,
                        houseRentAllowance = 36360.0,
                    ),
                deductions = Deductions(),
                ledgerBalances = LedgerBalances(),
                summary = PayslipSummary(0.0, 0.0, 0.0),
                taxAndSavings = null,
            )
        }

    fun assertHistoryResolvesAllFourCards(
        bundle: GuideBundle,
        history: List<ParsedPayslip>,
    ) {
        val built = assertNotNull(GuideProfileBuilder.from(history))
        for (card in personalCards) assertTrue(bundle.figureFor(card, built) != null, "$card resolves from a real-shaped history")
    }
}
