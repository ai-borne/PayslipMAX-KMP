package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.guide.data.GuideJson
import com.payslipmax.pdfparser.guide.domain.GuideProfile
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.GuidePreClaim

/**
 * A pre-claim worksheet with the shape of the real one (the portal's question fields, the amount boxes, the TD rule and LTC rule
 * pickers) in its own words, plus the full figures, for the worksheet ViewModel's tests. [SyntheticGuidePreClaim] stays the small
 * section the validator and UI tests use. The ids are the real ones because the ViewModel reads them; the real bundle's own ids are
 * pinned by `GuidePreClaimContract`.
 */
object SyntheticPreClaimWorksheet {
    private const val QUESTIONS =
        """{"id":"spr","label":"SPR","kind":"tick","source":"officer_types"},
           {"id":"re_employed","label":"Re-employed","kind":"tick","source":"officer_types"},
           {"id":"marital_status","label":"Marital status","kind":"text","source":"officer_types","unverified":true,"hint":"Values not confirmed."},
           {"id":"spouse_in_army","label":"Spouse in Army","kind":"tick","source":"officer_types"}"""
    private const val PAY =
        """{"id":"basic_pay","label":"Basic pay","kind":"amount","source":"officer_types"},
           {"id":"npa","label":"NPA","kind":"amount","source":"officer_types"},
           {"id":"msp","label":"MSP","kind":"amount","source":"officer_types"}"""

    val JSON: String =
        """
        {"limits":{"label":60,"hint":160,"text_chars":200},
         "upload":{"files":1,"more_than_kb":50,"less_than_mb":2,"format":"PDF"},
         "options":{
          "td_rule":{"card_linked":false,"options":[
            {"id":"tr119","label":"Rule 119","use_when":"Ordinary temporary duty."},
            {"id":"tr124","label":"Rule 124 (no DA)","use_when":"A course."}]},
          "ltc_rule":{"card_linked":true,"options":[
            {"id":"ltc_home","label":"Home town","use_when":"Your yearly trip.","card":"${SyntheticGuidePreClaim.PICKER_CARD}"},
            {"id":"ltc_any","label":"Any station","use_when":"The alternate-year trip.","card":"RB-T10"}]}},
         "moves":{
          "PD":{"title":"Permanent duty","fields":[
            {"id":"pcda_account","label":"Account","kind":"text","source":"portal_filled"},
            $QUESTIONS,$PAY,
            {"id":"sanction_id","label":"Sanction","kind":"text","source":"officer_types","max_chars":40},
            {"id":"andaman_nicobar","label":"Destination is Andaman Nicobar","kind":"tick","source":"officer_types"}],
           "documents":[{"id":"order","label":"The posting order"}],"notes":["The portal sets the advance."]},
          "TD":{"title":"Temporary duty","fields":[
            {"id":"pcda_account","label":"Account","kind":"text","source":"portal_filled"},
            {"id":"spr","label":"SPR","kind":"tick","source":"officer_types"},
            {"id":"marital_status","label":"Marital status","kind":"text","source":"officer_types"},
            $PAY,
            {"id":"long_course","label":"Long course","kind":"tick","source":"officer_types","unverified":true,"hint":"Meaning not confirmed."},
            {"id":"tr_rule","label":"TR Rule","kind":"choice","source":"officer_types","options":"td_rule"},
            {"id":"total_absence","label":"Total absence","kind":"number","source":"portal_filled"},
            {"id":"food_charges","label":"Food","kind":"amount","source":"app_suggests"},
            {"id":"rma","label":"RMA","kind":"amount","source":"app_suggests"},
            {"id":"accommodation","label":"Accommodation","kind":"amount","source":"app_suggests"}],
           "documents":[{"id":"signal","label":"The signal"}]},
          "LTC":{"title":"Leave travel","fields":[
            {"id":"home_station","label":"Home station","kind":"text","source":"officer_types"},
            $QUESTIONS,
            {"id":"tr_rule","label":"TR Rule","kind":"choice","source":"officer_types","options":"ltc_rule"},
            {"id":"total_fare","label":"Fare","kind":"amount","source":"officer_types"}],
           "documents":[{"id":"memo","label":"The memo"}]}}}
        """.trimIndent()

    val preClaim: GuidePreClaim = GuideJson.decodeFromString(GuidePreClaim.serializer(), JSON)

    /** The synthetic cards with the full figures (food, CTG, the TD ceilings, RMA) and this worksheet. */
    fun bundle(): GuideBundle = SyntheticGuideBundle.withPreClaim().copy(figures = SyntheticGuideFigures.figures, preclaim = preClaim)

    /** Level 13A, DA 46%, the owner's claim A profile, with MSP and NPA as a payslip would show them. */
    val profile = GuideProfile(2026, 6, "13A", 149000.0, 46, null, null, mspAmount = 15500.0, npaAmount = 0.0)
}
