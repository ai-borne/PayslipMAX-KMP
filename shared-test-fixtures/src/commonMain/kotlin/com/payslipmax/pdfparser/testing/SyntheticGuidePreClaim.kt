package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.guide.data.GuideJson
import com.payslipmax.pdfparser.guide.model.GuidePreClaim

/**
 * A small, valid TA/DA pre-claim section for logic and UI tests, shaped like `docs/Plan/rule_cards/preclaim.json` as bundle.py ships it
 * but with its own words, so an edit to the real worksheet never breaks them. The picker links to [PICKER_CARD], a card of
 * [SyntheticGuideBundle]; `SyntheticGuideBundle.withPreClaim()` joins the two.
 */
object SyntheticGuidePreClaim {
    const val PICKER_CARD = "RB-T9"

    val JSON: String =
        """
        {"limits":{"label":60,"hint":160,"text_chars":200},
         "upload":{"files":1,"more_than_kb":50,"less_than_mb":2,"format":"PDF"},
         "options":{
          "mode":{"card_linked":false,"options":[{"id":"rail","label":"Rail"},{"id":"road","label":"Road"}]},
          "rule":{"card_linked":true,"options":[{"id":"r1","label":"Rule one","use_when":"The ordinary case.","card":"$PICKER_CARD"}]}},
         "moves":{
          "PD":{"title":"Permanent duty","fields":[
            {"id":"account","label":"Account","kind":"text","source":"portal_filled"},
            {"id":"sanction_id","label":"Sanction","kind":"text","source":"officer_types","max_chars":40}],
           "documents":[{"id":"order","label":"The posting order"}],"notes":["The portal sets the advance."]},
          "TD":{"title":"Temporary duty","fields":[
            {"id":"rule","label":"Rule","kind":"choice","source":"officer_types","options":"mode"},
            {"id":"tick","label":"A tick","kind":"tick","source":"officer_types","unverified":true,"hint":"Not confirmed on the portal."}],
           "documents":[{"id":"signal","label":"The signal"}]},
          "LTC":{"title":"Leave travel","fields":[
            {"id":"rule","label":"Rule","kind":"choice","source":"officer_types","options":"rule"},
            {"id":"fare","label":"Fare","kind":"amount","source":"officer_types","hint":"Your own ticket cost."}],
           "documents":[{"id":"memo","label":"The memo"},{"id":"certificate","label":"The certificate","when":"Needed when you travel."}]}}}
        """.trimIndent()

    val preClaim: GuidePreClaim = GuideJson.decodeFromString(GuidePreClaim.serializer(), JSON)
}
