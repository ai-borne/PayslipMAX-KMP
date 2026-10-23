package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.guide.data.GuideJson
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.Part2Lookup

/**
 * A small, valid Part II lookup section for logic and UI tests, shaped like `part2_nav.py` ships it but with invented names, so an edit
 * to the real manual data never breaks them. Its limits are small on purpose (the section carries its own). Serial [NO_GROUP_SERIAL]
 * prints no group, as 3.28 and 11.8 do in ODP-2026; [VARIANT_SERIAL] carries two codes. `SyntheticGuideBundle.withPart2()` joins it
 * to the synthetic bundle.
 */
object SyntheticGuidePart2 {
    const val NO_GROUP_SERIAL = "3.28"
    const val VARIANT_SERIAL = "3.27"

    val JSON: String =
        """
        {"version":1,
         "limits":{"text":300,"title_words":7,"ribbons_min":1,"ribbons_max":3,"cases_per_ribbon":3,"case_serials_min":2,"case_serials_max":4},
         "groups":{"A":"Money","AM":"Money, wanted by MISO","B":"Claims","C":"No money","CM":"No money, wanted by MISO"},
         "serials":[
          {"serial":"1.1","chapter":1,"codes":["FIRSTCODE"],"group":"A","name":"Granted the first thing","page":28},
          {"serial":"1.2","chapter":1,"codes":["SECONDCODE"],"group":"C","name":"Reported for the second thing","page":29},
          {"serial":"2.1","chapter":2,"codes":["THIRDCODE"],"group":"AM","name":"Released from the third thing","page":39},
          {"serial":"3.1","chapter":3,"codes":["FOURTH11"],"group":"CM","name":"Granted the fourth thing","page":46},
          {"serial":"$VARIANT_SERIAL","chapter":3,"codes":["VARIANT1","VARIANT2"],"group":"B","name":"Granted a variant thing","page":57},
          {"serial":"$NO_GROUP_SERIAL","chapter":3,"codes":["STOPCODE"],"name":"Ceased to draw the variant thing","page":57}],
         "ribbons":[
          {"id":"leave","title":"The first ribbon","built":true,"cases":[
           {"id":"leave_one","title":"First case","serials":["1.1","1.2"],"also":["3.1"],"lookup":[]},
           {"id":"leave_two","title":"Second case","serials":["2.1","3.1"],"also":[],"lookup":["3.1"]}]},
          {"id":"records","title":"The second ribbon","cases":[
           {"id":"records_one","title":"Third case","serials":["$VARIANT_SERIAL","$NO_GROUP_SERIAL"],"also":["1.1"],"lookup":["$VARIANT_SERIAL","$NO_GROUP_SERIAL"]}]}]}
        """.trimIndent()

    val lookup: Part2Lookup = GuideJson.decodeFromString(Part2Lookup.serializer(), JSON)

    /** [SyntheticGuideBundle] with this section attached. */
    fun bundle(): GuideBundle = (GuideBundleParser.parse(SyntheticGuideBundle.JSON) as GuideLoadResult.Loaded).bundle.copy(part2 = lookup)
}
