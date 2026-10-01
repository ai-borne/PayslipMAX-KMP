package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.onboarding.OnboardingManager
import com.payslipmax.pdfparser.onboarding.OnboardingStorage
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
    private class FakeStorage(var introSeen: Boolean = false) : OnboardingStorage {
        override fun getHasCompletedOnboarding() = true

        override fun saveHasCompletedOnboarding(completed: Boolean) = Unit

        override fun getHasSeenUploadCoachmark() = true

        override fun saveHasSeenUploadCoachmark(seen: Boolean) = Unit

        override fun getHasSeenPayAuditIntro() = introSeen

        override fun saveHasSeenPayAuditIntro(seen: Boolean) {
            introSeen = seen
        }
    }

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
            val storage = FakeStorage()
            setContent { PayAuditIntroGate(OnboardingManager(storage)) }

            onNodeWithText(PayAuditEntryStrings.orientationTitle).assertExists()
            onNodeWithTag("pay_audit_orientation_got_it").performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()

            onNodeWithText(PayAuditEntryStrings.orientationTitle).assertDoesNotExist()
            assertTrue(storage.introSeen)
        }

    @Test
    fun gateStaysClosedOnceTheIntroHasBeenSeen() =
        runComposeUiTest {
            setContent { PayAuditIntroGate(OnboardingManager(FakeStorage(introSeen = true))) }

            onNodeWithText(PayAuditEntryStrings.orientationTitle).assertDoesNotExist()
        }
}
