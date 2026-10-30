package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.guide.data.GuideBundleValidator
import com.payslipmax.pdfparser.guide.model.GuideBundle
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Contract between the shipped Part II section (the files in `part2/`, as bundled; Orders Bank phase 1) and the app, on the real bundle: the
 * manual's 303 serials and 321 codes, the 10 ribbons, a column row for every serial with the four data columns, the owner's 30
 * plain-English pages over 48 serials, and a bundle the validator accepts. Run on each platform (`GuideBundleAndroidContractTest`,
 * `GuideLoaderIosPerfTest`).
 */
object Part2CasualtyContract {
    fun assertCasualtyDataReachesTheApp(bundle: GuideBundle) {
        val section = assertNotNull(bundle.part2, "the bundle ships the Part II section")
        assertEquals(303, section.serials.size)
        assertEquals(321, section.serials.flatMap { it.codes }.toSet().size)
        assertEquals(10, section.ribbons.size)
        assertEquals(section.serials.map { it.serial }, section.columns.map { it.serial }, "a column row for every serial, in order")
        assertTrue(section.columns.all { it.data.size == 4 }, "From, To and four data columns each")
        assertEquals(30, section.about.size)
        assertEquals(48, section.about.flatMap { it.serials }.toSet().size)
        assertTrue(section.related.isNotEmpty())
        assertEquals(emptyList(), GuideBundleValidator.validate(bundle), "the shipped bundle passes the validator")
    }
}
