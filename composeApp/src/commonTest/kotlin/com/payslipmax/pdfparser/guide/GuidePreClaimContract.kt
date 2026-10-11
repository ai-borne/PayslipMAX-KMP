package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.guide.model.GuideBundle
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Contract between the shipped pre-claim worksheet (`preclaim.json`, as bundled; plan 22, P3) and the app, on the real bundle. It pins what
 * must reach the app intact: the portal's field order per move (an officer copies values box by box), who fills each box, the
 * upload rule, and that every LTC rule-picker option links to a card the Guide really shows. Run on each platform
 * (`GuideBundleAndroidContractTest`, `GuideLoaderIosPerfTest`).
 */
object GuidePreClaimContract {
    private val tdOrder =
        listOf(
            "pcda_account", "officer_name", "spr", "marital_status", "basic_pay", "npa", "msp", "sanction_id", "sanction_date",
            "from_station", "to_station", "andaman_nicobar", "from_date", "to_date", "long_course", "tr_rule", "mode_of_journey",
            "total_fare", "total_absence", "food_charges", "rma", "accommodation", "amount_required", "dts_advance_certificate",
        )

    fun assertPreClaimWorksheetReachesTheApp(bundle: GuideBundle) {
        val section = assertNotNull(bundle.preclaim, "the bundle ships the pre-claim worksheet")
        assertEquals(setOf("PD", "TD", "LTC"), section.moves.keys)
        assertEquals(Triple(60, 160, 200), Triple(section.limits.label, section.limits.hint, section.limits.textChars))
        assertEquals(tdOrder, section.moves.getValue("TD").fields.map { it.id }, "TD fields follow the portal's order")
        assertEquals(15, section.moves.getValue("PD").fields.size)
        assertEquals(28, section.moves.getValue("LTC").fields.size)
        assertEquals(listOf(1, 50, 2, "PDF"), listOf(section.upload.files, section.upload.moreThanKb, section.upload.lessThanMb, section.upload.format))

        // The officer types only what the portal leaves open: locked and greyed boxes must never be offered for copying.
        for (move in section.moves.values) {
            assertEquals(listOf("portal_filled", "portal_filled"), move.fields.take(2).map { it.source }, "account and name are locked")
            assertEquals(40, move.fields.first { it.id == "sanction_id" }.maxChars, "the portal's sanction id limit")
            assertTrue(move.documents.isNotEmpty())
        }
        val td = section.moves.getValue("TD").fields.associateBy { it.id }
        assertEquals(listOf("portal_filled", "portal_filled"), listOf("total_absence", "amount_required").map { td.getValue(it).source })
        assertEquals(listOf("app_suggests", "app_suggests", "app_suggests"), listOf("food_charges", "rma", "accommodation").map { td.getValue(it).source })
        assertEquals("tick", td.getValue("dts_advance_certificate").kind)
        assertTrue(td.getValue("long_course").unverified, "the long-course tick's meaning is not confirmed on the portal")
    }

    fun assertRulePickerLinksToRealCards(bundle: GuideBundle) {
        val section = assertNotNull(bundle.preclaim)
        val picker = section.options.getValue("ltc_rule")
        assertTrue(picker.cardLinked)
        assertEquals(8, picker.options.size)
        val liveCards = bundle.cards.filterNot { it.isReplaced }.map { it.id }.toSet()
        for (option in picker.options) {
            assertTrue(option.card in liveCards, "picker option ${option.id} points at a card the Guide shows")
            assertTrue(option.useWhen.isNotEmpty(), "picker option ${option.id} says when to use it")
        }
        assertEquals(listOf("tr4i", "tr4ii", "tr119", "tr124"), section.options.getValue("td_rule").options.map { it.id })
        assertTrue(section.options.getValue("td_rule").options.none { it.card.isNotEmpty() }, "the TD rule list links to no card")
        val ltc = section.moves.getValue("LTC").fields.associateBy { it.id }
        assertEquals(listOf("ltc_rule", "mode_of_journey"), listOf("tr_rule", "mode_of_journey").map { ltc.getValue(it).options })
    }
}
