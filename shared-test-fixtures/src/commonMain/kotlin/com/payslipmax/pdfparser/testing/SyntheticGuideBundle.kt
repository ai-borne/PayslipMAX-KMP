package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.GuideFigures

/**
 * A small, valid Claim Guide bundle for logic and UI tests, so a rate edit in the real cards never breaks
 * them. It carries one card of each kind the UI treats differently: a personal card with placeholders, an
 * unverified card, an Open Source Info card, a DoPT OM card, a card with no cite, rate and amended chips, an "also relevant here" link, and one case
 * with more than 7 cards across several facets.
 */
object SyntheticGuideBundle {
    const val RATES_AS_OF = "2026-01"
    const val PERSONAL_CARD = "RB-T1"
    const val UNVERIFIED_CARD = "RB-T2"
    const val NO_CITE_CARD = "RB-T3"
    const val AMENDED_CARD = "RB-T4"
    const val OPEN_SOURCE_CARD = "RB-T6"
    const val DOPT_OM_CARD = "RB-T8"
    const val BIG_CASE = "td-da"

    val JSON: String =
        """
        {"version":1,"generated":"2026-10-07","rates_as_of":"$RATES_AS_OF",
        "limits":{"answer":25,"bullets":3,"bullet_words":12,"visible":90,"details":120,"title":14},
        "nav":[
         {"id":"travel","title":"Travel","cases":[
          {"id":"$BIG_CASE","title":"Daily allowance on duty","sub":"Rule 114",
           "cards":["RB-T1","RB-T2","RB-T3","RB-T4","RB-T5","RB-T6","RB-T7","RB-T8"],"also":[]},
          {"id":"ltc-home","title":"Home town LTC","sub":"Rule 177A","cards":["RB-T9","RB-T10"],"also":["RB-T1"]}]},
         {"id":"pay","title":"Pay","cases":[
          {"id":"pay-hra","title":"House rent","sub":"","cards":["RB-P1","RB-P2","RB-P3"],"also":[]}]}],
        "facets":{"Q":"Who qualifies","H":"How much","C":"How to claim","L":"Limits and traps"},
        "cards":[
         ${card("RB-T1", BIG_CASE, "H", chips = "\"RATES\"", personal = "level:food_rate")},
         ${card("RB-T2", BIG_CASE, "H", unverified = true)},
         ${card("RB-T3", BIG_CASE, "C", cite = "", chips = "\"GUIDANCE\"")},
         ${card("RB-T4", BIG_CASE, "Q", chips = "\"AMENDED\"")},
         ${card("RB-T5", BIG_CASE, "Q")},
         ${card("RB-T6", BIG_CASE, "L", basis = "open_source")},
         ${card("RB-T7", BIG_CASE, "H")},
         ${card("RB-T8", BIG_CASE, "C", basis = "dopt_om")},
         ${card("RB-T9", "ltc-home", "Q")},
         ${card("RB-T10", "ltc-home", "H")},
         ${card("RB-P1", "pay-hra", "Q")},
         ${card("RB-P2", "pay-hra", "H")},
         ${card("RB-P3", "pay-hra", "L")}]}
        """.trimIndent()

    /**
     * The same bundle with the approved food-rate figure on its personal card, as the real bundle ships it. Built by copy,
     * not parsed from [JSON], so the plain synthetic bundle stays a bundle with no figures.
     */
    fun withFigures(): GuideBundle {
        val bundle = (GuideBundleParser.parse(JSON) as GuideLoadResult.Loaded).bundle
        val food = SyntheticGuideFigures.figures.figures.getValue("food_rate").copy(card = PERSONAL_CARD)
        return bundle.copy(figures = GuideFigures(SyntheticGuideFigures.figures.daStep, mapOf("food_rate" to food)))
    }

    /** The same bundle with the synthetic pre-claim worksheet, whose rule picker links to [SyntheticGuidePreClaim.PICKER_CARD]. */
    fun withPreClaim(): GuideBundle = (GuideBundleParser.parse(JSON) as GuideLoadResult.Loaded).bundle.copy(preclaim = SyntheticGuidePreClaim.preClaim)

    /** The same bundle with the synthetic Part II section ([SyntheticGuidePart2]). */
    fun withPart2(): GuideBundle = SyntheticGuidePart2.bundle()

    private fun card(
        id: String,
        nav: String,
        facet: String,
        cite: String = "Rule 114 TR",
        chips: String = "",
        personal: String = "",
        key: String = "A short key point for $id",
        unverified: Boolean = false,
        basis: String = "",
    ): String =
        """{"id":"$id","title":"Synthetic card $id?",""" +
            """"answer":"A one-line answer for $id.","key":["$key"],"attach":["A form"],"watch":[],""" +
            """"cite":"$cite","details":"Longer details for $id.","chips":[$chips],"personal":"$personal",""" +
            """"facet":"$facet","nav":"$nav","unverified":$unverified,"basis":"$basis"}"""
}
