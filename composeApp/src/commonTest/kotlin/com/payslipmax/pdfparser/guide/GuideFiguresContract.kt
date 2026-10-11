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
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.guide.preclaim.CtgCase
import com.payslipmax.pdfparser.guide.preclaim.CtgEstimate
import com.payslipmax.pdfparser.guide.preclaim.LimitNote
import com.payslipmax.pdfparser.guide.preclaim.LodgingEstimate
import com.payslipmax.pdfparser.guide.preclaim.NilReason
import com.payslipmax.pdfparser.guide.preclaim.NotPayable
import com.payslipmax.pdfparser.guide.preclaim.PerDayEstimate
import com.payslipmax.pdfparser.guide.preclaim.PerKmEstimate
import com.payslipmax.pdfparser.guide.preclaim.PermanentDutyRequest
import com.payslipmax.pdfparser.guide.preclaim.PreClaimCalculator
import com.payslipmax.pdfparser.guide.preclaim.PreClaimElement
import com.payslipmax.pdfparser.guide.preclaim.PreClaimNote
import com.payslipmax.pdfparser.guide.preclaim.PreClaimProfile
import com.payslipmax.pdfparser.guide.preclaim.PreClaimRules
import com.payslipmax.pdfparser.guide.preclaim.TemporaryDutyRequest
import com.payslipmax.pdfparser.insights.timeline.PayLevel
import com.payslipmax.pdfparser.insights.timeline.TptaCityClass
import com.payslipmax.pdfparser.testing.SyntheticGuideFigures
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Contract between the shipped "your figure" data (`figures.json`, as bundled) and the app, on the real bundle. It states
 * the amounts the approved letters imply, so a wrong figure in the data fails here even though no rupee figure is in
 * production Kotlin. It also pins the one place two sources could drift: Pay Audit's own Transport Allowance bases.
 * Run on each platform (`GuideBundleAndroidContractTest`, `GuideLoaderIosPerfTest`).
 */
object GuideFiguresContract {
    private val personalCards = setOf("RB-SS-T181", "RB-SS-T254", "RB-SS-P051-rates", "RB-SS-P116-rates")
    private val preClaimKeys = setOf("rma", "baggage_road", "td_hotel", "td_taxi", "td_food_hours")

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
        assertEquals(setOf("food_rate", "ctg", "transport_allowance", "hra") + preClaimKeys, figures.figures.keys)
        assertEquals(SyntheticGuideFigures.figures, figures, "the unit-test copy of the figures must equal what ships, or its oracle proves nothing")
        assertEquals(personalCards, figures.figures.values.map { it.card }.filter { it.isNotEmpty() }.toSet())
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

    /**
     * The TA/DA pre-claim figures (plan 22, P2) as the owner approved them on 2026-10-11 (register 23). Bases are the handbook
     * figures before the DA step; no amount is pre-multiplied. A figure with no card must never reach a card's "your figure" line.
     */
    fun assertPreClaimFiguresMatchTheApprovedRegister(bundle: GuideBundle) {
        val figures = assertNotNull(bundle.figures)
        assertEquals("2024-01-01", figures.daStep.firstStepEffectiveFrom, "the owner's step date for the TD lump sums")
        for (key in preClaimKeys) {
            val figure = figures.figures.getValue(key)
            assertEquals("" to "preclaim", figure.card to figure.use, key)
            assertTrue(figure.effectiveFrom.isNotEmpty(), "$key shows a date")
        }

        fun base(
            key: String,
            level: String,
        ) = figures.figures.getValue(key).bands.firstOrNull { level in it.levels }

        assertEquals(listOf(7500, 4500, 2250), listOf("14", "12A", "11").map { base("td_hotel", it)?.base })
        assertEquals(338, base("td_taxi", "9")?.base)
        for (level in listOf("12", "13B", "14", "18")) {
            val band = assertNotNull(base("td_taxi", level), "taxi band for $level")
            assertEquals(0, band.base, "taxi at level $level has no rupee cap")
            assertTrue(band.limit.isNotEmpty(), "taxi at level $level carries a limit text instead")
        }
        for (level in PayLevel.entries) {
            // No level Pay Audit knows may fall out of a band, or the worksheet would show a silent nil for that officer.
            assertEquals(50, base("baggage_road", level.label)?.base, "road baggage at ${level.label}")
            assertTrue((base("td_hotel", level.label)?.base ?: 0) > 0, "hotel ceiling at ${level.label}")
            assertNotNull(base("td_taxi", level.label), "taxi band at ${level.label}")
        }
        assertNull(base("baggage_road", "9"), "road baggage starts at level 10 in the handbook")

        val rma = figures.figures.getValue("rma")
        assertEquals("2024-09-03", rma.effectiveFrom)
        assertEquals(34, rma.rates.size)
        assertEquals(listOf(12, 20, 30), listOf("Punjab", "Maharashtra", "Assam").map { rma.rates[it] })
        assertTrue(rma.rates.values.all { it > 0 }, "no state may be a silent nil")
        assertTrue("Lakshadweep" !in rma.rates, "the Army has no presence there, so the picker never lists it")
    }

    /**
     * The permanent-duty estimates (plan 22, P4) on the real shipped bundle, for the owner's issued advance voucher: basic 149000,
     * DA 60%, level 14, 2881 km. The amounts are what the approved rules imply; Native must compute the same whole-number sums.
     */
    fun assertPermanentDutyEstimatesMatchTheApprovedRules(bundle: GuideBundle) {
        val figures = assertNotNull(bundle.figures)
        val profile = PreClaimProfile.from(profile(level = "14", basic = 149000.0, da = 60).copy(mspAmount = 15500.0, npaAmount = 0.0))

        fun estimates(case: CtgCase) = PreClaimCalculator.permanentDuty(figures, PermanentDutyRequest(profile, case, roadKm = 2881, originState = "Assam"))

        val (ctg, baggage, conveyance) = estimates(CtgCase.NORMAL)
        assertEquals(119200L, assertIs<CtgEstimate>(ctg).rupees, "the voucher's CTG")
        assertEquals(6250L to 180063L, assertIs<PerKmEstimate>(baggage).let { it.ratePaise to it.rupees }, "Rs 62.50 a km, total rounded once")
        assertEquals(3000L to 86430L, assertIs<PerKmEstimate>(conveyance).let { it.ratePaise to it.rupees }, "Assam Rs 30 a km")
        assertEquals(149000L, assertIs<CtgEstimate>(estimates(CtgCase.ISLAND)[0]).rupees, "island: 100% of basic")
        assertEquals(39733L, assertIs<CtgEstimate>(estimates(CtgCase.LOCAL_MOVE)[0]).rupees, "local move: one-third of the 80%")
    }

    /**
     * The temporary-duty estimates (plan 22, P5) on the real shipped bundle, for the owner's filed claim A reduced to numbers (level
     * 13A, DA 46%, 57 days, nights at Rs 4500), and the same officer at DA 60% on a level 11 pay level for the half-rupee ceilings.
     * It also pins the one rule id the calculator reads by name, so renaming that option in `preclaim.json` fails here.
     */
    fun assertTemporaryDutyEstimatesMatchTheApprovedRules(bundle: GuideBundle) {
        val figures = assertNotNull(bundle.figures)
        val tdRule = assertNotNull(bundle.preclaim).options.getValue("td_rule").options.associateBy { it.id }
        assertTrue("DA not admissible" in tdRule.getValue(PreClaimRules.NO_DA_RULE_OPTION_ID).label, "the id the calculator reads is still Rule 124")

        fun request(
            level: String,
            da: Int,
            rule: String,
            to: CalendarDate,
            nights: Int,
            nightly: Long,
        ) = TemporaryDutyRequest(
            profile = PreClaimProfile(level, 150000, da, null, null),
            trRuleId = rule,
            from = CalendarDate(2024, 2, 1),
            to = to,
            minutesAway = null,
            nights = nights,
            nightlyRateRupees = nightly,
            holdsRmaSanction = false,
            roadKm = null,
            originState = null,
        )

        val claimA = PreClaimCalculator.temporaryDuty(figures, request("13A", 46, "tr119", CalendarDate(2024, 3, 28), 57, 4500))
        assertEquals(57, claimA.days)
        assertEquals(57000L, assertIs<PerDayEstimate>(claimA.outcomes[0]).rupees, "57 x Rs 1000")
        assertEquals(256500L, assertIs<LodgingEstimate>(claimA.outcomes[1]).rupees, "57 x Rs 4500")
        assertEquals("AC taxi, up to 50 km a day within the city", assertIs<LimitNote>(claimA.outcomes[2]).text)
        assertEquals(NotPayable(PreClaimElement.CAR_CONVEYANCE, NilReason.NO_RULE_40_SANCTION), claimA.outcomes[3])
        assertTrue(claimA.notes.isEmpty())

        val level11 = PreClaimCalculator.temporaryDuty(figures, request("11", 60, "tr119", CalendarDate(2024, 2, 2), 2, 3000))
        assertEquals(2250L, assertIs<PerDayEstimate>(level11.outcomes[0]).rupees, "2 x Rs 1125")
        assertEquals(281_250L, assertIs<LodgingEstimate>(level11.outcomes[1]).ceilingPaise, "Rs 2812.50, exact")
        assertEquals(5625L, assertIs<LodgingEstimate>(level11.outcomes[1]).rupees, "the bill is capped at the ceiling")
        assertEquals(42_250L, assertIs<PerDayEstimate>(level11.outcomes[2]).ratePaise, "Rs 422.50, exact")

        val sameDay = request("13A", 60, "tr119", CalendarDate(2024, 2, 1), 0, 1).copy(minutesAway = 300)
        assertEquals(375L, assertIs<PerDayEstimate>(PreClaimCalculator.temporaryDuty(figures, sameDay).outcomes[0]).rupees, "30% of Rs 1250")

        val course = PreClaimCalculator.temporaryDuty(figures, request("13A", 60, PreClaimRules.NO_DA_RULE_OPTION_ID, CalendarDate(2024, 3, 28), 0, 1))
        assertEquals(List(3) { NilReason.RULE_124_NO_DA }, course.outcomes.take(3).map { assertIs<NotPayable>(it).reason })
        val long = PreClaimCalculator.temporaryDuty(figures, request("13A", 60, "tr119", CalendarDate(2024, 8, 1), 0, 1))
        assertEquals(setOf(PreClaimNote.OVER_180_DAYS), long.notes)
    }

    /** Only the four personal cards ever get a "your figure" line, whatever else the bundle ships (the pre-claim rates have no card). */
    fun assertOnlyPersonalCardsResolveAFigure(bundle: GuideBundle) {
        val resolved = bundle.cards.filter { bundle.figureFor(it.id, profile(level = "12A", basic = 100000.0, tpta = "HIGHER", hra = 30000.0)) != null }.map { it.id }.toSet()
        assertEquals(personalCards, resolved)
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
