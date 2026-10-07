package com.payslipmax.pdfparser.guide

import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test

/**
 * The real bundle is read through Android assets, exactly as production reads it (compose resources are
 * packaged as assets), so a resource that is not packaged fails here rather than as an empty Guide tab.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GuideBundleAndroidContractTest {
    @Test
    fun theShippedBundleLoadsAndMatchesTheCompiledDataset() =
        runTest {
            val text = GuideBundleContract.readShippedBundleText()

            val bundle = GuideBundleContract.parseShippedBundle(text)
            GuideBundleContract.assertMatchesCompiledDataset(bundle)
            GuideBundleContract.assertTilesMatchDataset(bundle)
            GuideBundleContract.assertFeedsAndCardsMatchDataset(bundle)
            GuideBundleContract.assertSearchMatchesDataset(bundle)
            GuideBundleContract.assertTrustAndPreviewMatchDataset(bundle)
            GuideBundleContract.assertPaywallOnlyWhenNoUnverifiedRateCard(bundle)
        }
}
