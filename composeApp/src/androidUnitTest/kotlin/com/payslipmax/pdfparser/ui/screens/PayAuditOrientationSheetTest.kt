package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.onboarding.OnboardingManager
import com.payslipmax.pdfparser.testing.FakeOnboardingStorage
import com.payslipmax.pdfparser.ui.theme.PayAuditEntryStrings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * First-open orientation (ported from 1.0's onboarding sheet, rewritten for the timeline/evidence model).
 * It must explain the model the user will actually see, show once, and never reintroduce the dropped
 * situation tiles or "unclaimed ₹" counter.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalTestApi::class)
class PayAuditOrientationSheetTest {
    @Test
    fun sheetExplainsTimelineEvidenceAndExplainedChanges() =
        runComposeUiTest {
            setContent { PayAuditOrientationSheet(onDismiss = {}) }

            onNodeWithText(PayAuditEntryStrings.orientationTitle).assertExists()
            PayAuditEntryStrings.orientationPoints.forEach { (heading, body) ->
                onNodeWithText(heading).assertExists()
                onNodeWithText(body).assertExists()
            }
        }

    @Test
    fun copyDoesNotReintroduceTilesOrAnUnclaimedCounter() {
        val all = PayAuditEntryStrings.orientationPoints.flatMap { listOf(it.first, it.second) } + PayAuditEntryStrings.orientationTitle
        all.forEach { text ->
            assertFalse(text.contains("tile", ignoreCase = true) || text.contains("unclaimed", ignoreCase = true) || text.contains("₹"), text)
        }
    }

    @Test
    fun gotItDismisses() =
        runComposeUiTest {
            var dismissed = 0
            setContent { PayAuditOrientationSheet(onDismiss = { dismissed++ }) }

            onNodeWithTag("pay_audit_orientation_got_it").performSemanticsAction(SemanticsActions.OnClick)

            assertEquals(1, dismissed)
        }

    @Test
    fun gateShowsTheSheetOnFirstOpenAndRemembersTheDismissal() =
        runComposeUiTest {
            val storage = FakeOnboardingStorage(hasCompletedOnboarding = true, hasSeenUploadCoachmark = true)
            setContent { PayAuditIntroGate(OnboardingManager(storage)) }

            onNodeWithText(PayAuditEntryStrings.orientationTitle).assertExists()
            onNodeWithTag("pay_audit_orientation_got_it").performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()

            onNodeWithText(PayAuditEntryStrings.orientationTitle).assertDoesNotExist()
            assertTrue(storage.getHasSeenPayAuditIntro())
        }

    @Test
    fun gateStaysClosedOnceTheIntroHasBeenSeen() =
        runComposeUiTest {
            setContent { PayAuditIntroGate(OnboardingManager(FakeOnboardingStorage(hasCompletedOnboarding = true, hasSeenUploadCoachmark = true, hasSeenPayAuditIntro = true))) }

            onNodeWithText(PayAuditEntryStrings.orientationTitle).assertDoesNotExist()
        }
}
