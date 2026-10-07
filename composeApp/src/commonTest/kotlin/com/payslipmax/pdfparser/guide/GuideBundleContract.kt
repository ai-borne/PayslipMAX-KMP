package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.di.GUIDE_BUNDLE_PATH
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.guide.model.GuideBundle
import pdfparser.composeapp.generated.resources.Res
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Contract between the shipped bundle and the app, asserted on each platform after reading the real file
 * through the same compose resource path production uses (`GuideBundleAndroidContractTest`,
 * `GuideLoaderIosPerfTest`). Only these checks read the real 402 cards; logic tests use
 * `SyntheticGuideBundle`. When the dataset changes on purpose, update the counts here and in
 * `docs/Plan/rule_cards/tools/test_bundle.py` together.
 */
object GuideBundleContract {
    suspend fun readShippedBundleText(): String = Res.readBytes(GUIDE_BUNDLE_PATH).decodeToString()

    fun parseShippedBundle(text: String): GuideBundle = assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(text)).bundle

    fun assertMatchesCompiledDataset(bundle: GuideBundle) {
        assertEquals(GuideBundleParser.SUPPORTED_MAJOR, bundle.version)
        assertEquals(402, bundle.cards.size)
        assertEquals(220, bundle.cards.count { it.domain == "travel" })
        assertEquals(182, bundle.cards.count { it.domain == "pay" })
        assertEquals(9, bundle.nav.size)
        assertEquals(44, bundle.nav.sumOf { it.cases.size })
        assertEquals(setOf("Q", "H", "C", "L"), bundle.facets.keys)
        // Owner, 2026-10-06: all 36 open-point cards ship, flagged "Unverified point".
        assertEquals(36, bundle.cards.count { it.unverified })
        // Owner, 2026-10-07: "No official source" is exactly the 31 cards with no cite.
        assertEquals(31, bundle.cards.count { it.hasNoOfficialSource })
        // Owner, 2026-10-07: rates current to the DA 60% step.
        assertEquals("2026-01", bundle.ratesAsOf)
        // RP-088 HBA ships with the chip until the owner confirms the rate from a primary letter.
        assertEquals(true, bundle.cards.firstOrNull { it.topic == "RP-088" }?.unverified)
    }
}
