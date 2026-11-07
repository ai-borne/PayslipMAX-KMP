package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.summary.TdSummaryBuilder
import com.payslipmax.pdfparser.insights.timeline.PayLevel
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Contract between the shipped TD summary card (plan 24) and the app, on the real bundle: one card carries the table, it is the first card
 * of the TD daily-allowance case, and the table the app draws from the shipped figures is what PCDA(O) prints at DA 50 to 99%. No rupee
 * figure is in production Kotlin, so a wrong figure or a missing row fails here. Run on each platform
 * (`GuideBundleAndroidContractTest`, `GuideLoaderIosPerfTest`).
 */
object GuideTdSummaryContract {
    fun assertTheSummaryCardAndItsTableMatchTheApprovedData(bundle: GuideBundle) {
        val marked = bundle.cards.filter { it.summary.isNotEmpty() }
        assertEquals(listOf("RB-TD-SUMMARY" to "td"), marked.map { it.id to it.summary }, "exactly one card carries the TD table")
        assertEquals("td-da", marked.single().nav)
        val case = bundle.nav.flatMap { it.cases }.single { it.id == "td-da" }
        assertEquals("RB-TD-SUMMARY", case.cards.first(), "the summary leads the TD daily-allowance case")

        val figures = assertNotNull(bundle.figures)
        assertEquals(50, figures.daStep.tableDaPercent, "the free table is drawn at the DA 50 to 99% step")
        val summary = assertNotNull(TdSummaryBuilder.buildFor(figures, null), "a table without a payslip")
        assertEquals(25 to (50 to 99), summary.stepPercent to (summary.daBandFrom to summary.daBandTo))
        // Hotel ceiling a night and food a day at DA 50 to 99%, in paise: 2812.50 / 5625 / 9375 and 1125 / 1250 / 1500 (PCDA(O) FAQ for food).
        assertEquals(listOf(281250L, 562500L, 937500L), summary.levelGroups.map { it.hotelPaise })
        assertEquals(listOf(112500L, 125000L, 150000L), summary.levelGroups.map { it.foodPaise })
        // CEA and hostel subsidy are not on the TD card (owner, 2026-11-07), so the bundle ships no figure for them.
        assertEquals(emptySet(), figures.figures.keys.intersect(setOf("cea", "hostel_subsidy")))
        for (level in PayLevel.entries) {
            // No level Pay Audit knows may lack a row, or that officer's own row would silently never highlight.
            assertNotNull(summary.groupFor(level.label), "a table row for level ${level.label}")
        }
    }
}
