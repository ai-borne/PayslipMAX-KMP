package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pcdao.engine.FixationOption
import com.payslipmax.pcdao.engine.PayFixationResult
import com.payslipmax.pcdao.model.AuditDiscrepancy
import com.payslipmax.pcdao.model.DiscrepancySeverity
import com.payslipmax.pcdao.model.DiscrepancyType
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PcdaoAuditGatingUiTest {
    @AfterTest
    fun tearDown() {
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    private fun sampleDiscrepancy() =
        AuditDiscrepancy(
            id = "CEA_UNCLAIMED",
            title = "Children Education Allowance (CEA) Unclaimed",
            explanation = "CEA of ₹2,250/mo per child is unclaimed.",
            entitledAmount = 54000.0,
            drawnAmount = 0.0,
            netDue = 54000.0,
            type = DiscrepancyType.UNDERPAYMENT,
            severity = DiscrepancySeverity.CRITICAL,
            authority = "MoD Letter No 1(23)/2017/D(Pay/Services) dated 15 Sep 2017",
            recommendedAction = "Submit CEA claim for 2 children",
        )

    private fun sampleFixationResult() =
        PayFixationResult(
            fromLevel = "10",
            fromStage = 8,
            fromBasicPay = 69000,
            toLevel = "11",
            promotionDate = "2026-03-15",
            dniMonth = 7,
            opt1FixedPay = 71500,
            opt1InitialStage = 2,
            opt2PreDniPay = 69000,
            opt2PostDniFixedPay = 73600,
            opt2DniStage = 3,
            opt1Total36Months = 2650000,
            opt2Total36Months = 2712800,
            cumulativeDelta = 62800,
            recommendedOption = FixationOption.OPTION_2,
            recommendationSummary = "Option 2 yields higher cumulative pay",
            statutoryElectionDeadline = "2026-04-14",
            statutoryWarning = AppStringsPcdao.fixationUrgentCallout,
            monthlyTrajectory = emptyList(),
        )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun lockedTeaserBannerCallsUpgradeClick() =
        runComposeUiTest {
            var upgradeCalled = false
            setContent {
                AuditTeaserBanner(
                    claimsCount = 3,
                    unclaimedTotal = 75000.0,
                    onUpgradeClick = { upgradeCalled = true },
                )
            }

            onNodeWithText(AppStringsPcdao.teaserUpgradeButton).performClick()
            assertEquals(true, upgradeCalled)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun lockedRedressalButtonCallsRedressalClick() =
        runComposeUiTest {
            var redressalClicked = false
            setContent {
                FeedHeaderSection(
                    selectedFilter = FindingFilter.ALL,
                    onFilterSelected = {},
                    onRedressalClick = { redressalClicked = true },
                    isUnlocked = false,
                )
            }

            onNodeWithText(AppStringsPcdao.proRedressalLocked).performClick()
            assertEquals(true, redressalClicked)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun lockedDiscrepancyCardMathDiffCallsUpgradeClick() =
        runComposeUiTest {
            var upgradeCalled = false
            setContent {
                AuditDiscrepancyCard(
                    discrepancy = sampleDiscrepancy(),
                    isUnlocked = false,
                    onUpgradeClick = { upgradeCalled = true },
                )
            }

            onNodeWithText(AppStringsPcdao.proUnlockMathBadge).performClick()
            assertEquals(true, upgradeCalled)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun lockedDiscrepancyCardAuthorityCallsUpgradeClick() =
        runComposeUiTest {
            var upgradeCalled = false
            setContent {
                AuditDiscrepancyCard(
                    discrepancy = sampleDiscrepancy(),
                    isUnlocked = false,
                    onUpgradeClick = { upgradeCalled = true },
                )
            }

            onNodeWithText(AppStringsPcdao.proAuthorityLocked).performClick()
            assertEquals(true, upgradeCalled)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun lockedPayFixationCardOptionBoxCallsUpgradeClick() =
        runComposeUiTest {
            var upgradeCalled = false
            setContent {
                PayFixationCard(
                    result = sampleFixationResult(),
                    isUnlocked = false,
                    onUpgradeClick = { upgradeCalled = true },
                )
            }

            // Click the locked option trajectory
            onAllNodesWithText(AppStringsPcdao.fixationOptionLocked, substring = true).onFirst().performClick()
            assertEquals(true, upgradeCalled)
        }
}
