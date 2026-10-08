package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.guide.data.GuideJson
import com.payslipmax.pdfparser.guide.model.GuideFigures

/**
 * The approved "your figure" data (docs/Plan/rule_cards/figures.json, as bundle.py ships it) for resolver and UI tests.
 * It is a test oracle: unit tests state the amounts a letter implies, so a wrong figure in the real bundle is caught
 * by `GuideBundleContract` and a wrong formula is caught here. Production code reads the bundle and holds no rupee figure.
 */
object SyntheticGuideFigures {
    const val FOOD_CARD = "RB-SS-T181"
    const val CTG_CARD = "RB-SS-T254"
    const val TRANSPORT_CARD = "RB-SS-P051-rates"
    const val HRA_CARD = "RB-SS-P116-rates"

    val JSON: String =
        """
        {"da_step":{"per_da_percent":50,"increase_percent":25},"figures":{"food_rate":{"card":"RB-SS-T181","mode":"da_step","assumption":"full day (over 12 hours away), before taxes","effective_from":"2017-07-01","bands":[{"levels":["9","10","10A","10B","11"],"base":900},{"levels":["12","12A","12B","13","13A","13B"],"base":1000},{"levels":["14","15","16","17","18"],"base":1200}]},"ctg":{"card":"RB-SS-T254","mode":"percent_of_basic","assumption":"move of 20 km or more, not to or from Andaman, Nicobar or Lakshadweep (100% there)","effective_from":"2017-07-01","percent":80},"transport_allowance":{"card":"RB-SS-P051-rates","mode":"plus_da","effective_from":"2017-07-01","bands":[{"levels":["10","10A","10B","11","12","12A","12B","13","13A"],"city_class":"HIGHER","base":7200},{"levels":["10","10A","10B","11","12","12A","12B","13","13A"],"city_class":"OTHER","base":3600},{"levels":["14","15","16","17","18"],"city_class":"ANY","base":15750}]},"hra":{"card":"RB-SS-P116-rates","mode":"rate_table","classes":{"X":[{"from_da_percent":0,"percent":24,"effective_from":"2017-07-01"},{"from_da_percent":25,"percent":27,"effective_from":"2021-07-01"},{"from_da_percent":50,"percent":30,"effective_from":"2024-01-01"}],"Y":[{"from_da_percent":0,"percent":16,"effective_from":"2017-07-01"},{"from_da_percent":25,"percent":18,"effective_from":"2021-07-01"},{"from_da_percent":50,"percent":20,"effective_from":"2024-01-01"}],"Z":[{"from_da_percent":0,"percent":8,"effective_from":"2017-07-01"},{"from_da_percent":25,"percent":9,"effective_from":"2021-07-01"},{"from_da_percent":50,"percent":10,"effective_from":"2024-01-01"}]}}}}
        """.trimIndent()

    val figures: GuideFigures = GuideJson.decodeFromString(GuideFigures.serializer(), JSON)
}
