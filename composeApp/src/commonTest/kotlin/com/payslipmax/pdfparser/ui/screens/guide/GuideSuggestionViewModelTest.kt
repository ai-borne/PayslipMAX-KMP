package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.domain.GuideProfile
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticGuideRuleChange
import com.payslipmax.pdfparser.ui.theme.AppStringsSupport
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * M5: "Suggest a correction" from the view model's side. The email is built from the loaded bundle (card id, title and
 * revision, the Guide data date) plus the app version and the user's text, goes to support only through the platform seam,
 * and never carries a figure or a profile value even for a card that has a "your figure" line. Nothing goes to telemetry.
 */
class GuideSuggestionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val crash = FakeCrashReporter()
    private val sent = mutableListOf<Triple<String, String, String>>()
    private val platform = GuidePlatform(copy = {}, share = { _, _ -> }, email = { to, subject, body -> sent += Triple(to, subject, body) })
    private val profile = GuideProfile(2026, 6, level = "14", basicPay = 144200.0, daPercent = 60, tptaClass = null, hraAmount = null)

    private fun withRev(bundle: GuideBundle): GuideBundle = bundle.copy(cards = bundle.cards.map { it.copy(rev = "ab12cd34") })

    private fun model(bundle: GuideBundle? = null) =
        GuideViewModel(
            FakeGuideRepository(bundle?.let { GuideLoadResult.Loaded(withRev(it)) } ?: GuideLoadResult.Loaded(withRev(SyntheticGuideBundle.withFigures()))),
            crash,
            dispatcher,
            appVersion = { "9.9.9" },
        )

    private fun test(
        viewModel: GuideViewModel = model(),
        block: suspend TestScope.(GuideViewModel) -> Unit,
    ) = runTest(dispatcher) {
        viewModel.load()
        testScheduler.advanceUntilIdle()
        block(viewModel)
    }

    @Test
    fun beforeTheBundleLoadsThereIsNoEmailToBuild() =
        runTest(dispatcher) {
            assertNull(model().suggestionMail("RB-T5", "text"))
        }

    @Test
    fun theEmailIsExactlyThisTextForARealCard() =
        test { guide ->
            val mail = assertNotNull(guide.suggestionMail("RB-T5", "  The rate is wrong.  "))

            assertEquals("[Guide] RB-T5", mail.subject)
            assertEquals(
                "Guide correction suggestion\n\nCard: RB-T5\nTitle: Synthetic card RB-T5?\nGuide data: 2026-10-07\n" +
                    "Card revision: ab12cd34\nApp version: 9.9.9\n\nSuggestion:\nThe rate is wrong.",
                mail.body,
            )
        }

    @Test
    fun suggestHandsTheEmailToTheMailSeamOnceAddressedToSupport() =
        test { guide ->
            guide.suggest("RB-T5", "The rate is wrong.", platform)

            assertEquals(1, sent.size)
            assertEquals(AppStringsSupport.supportEmail, sent.single().first)
            assertEquals("[Guide] RB-T5", sent.single().second)
        }

    @Test
    fun anIdTheBundleDoesNotHoldSendsNothing() =
        test { guide ->
            assertNull(guide.suggestionMail("RB-NOPE", "text"))
            guide.suggest("RB-NOPE", "text", platform)

            assertTrue(sent.isEmpty())
        }

    @Test
    fun aReplacedCardCanBeSuggestedAboutByItsOwnId() =
        test(model(SyntheticGuideRuleChange.bundle())) { guide ->
            guide.suggest(SyntheticGuideRuleChange.OLD_CARD, "This old rule is still cited.", platform)

            assertEquals("[Guide] ${SyntheticGuideRuleChange.OLD_CARD}", sent.single().second)
            assertTrue(sent.single().third.contains("Card: ${SyntheticGuideRuleChange.OLD_CARD}\n"))
        }

    @Test
    fun aCardWithAFigureAndAProfileNeverPutsEitherInTheEmail() =
        test { guide ->
            val card = assertNotNull(guide.card(SyntheticGuideBundle.PERSONAL_CARD, unlocked = true, profile = profile))
            assertNotNull(card.full?.figure, "the card really resolves a figure for this profile, so the check below is not vacuous")

            guide.suggest(SyntheticGuideBundle.PERSONAL_CARD, "Please check this.", platform)

            val body = sent.single().third
            for (value in listOf("1500", "1,500", "144200", "144,200", "Level", "level 14", "60%")) {
                assertFalse(body.contains(value), "$value must not be in the email")
            }
            assertFalse(body.contains("A short key point"), "card text beyond the title is not in the email")
            assertFalse(body.contains("Rule 114 TR"), "the cite is not in the email")
        }

    @Test
    fun suggestingTellsTelemetryNothing() =
        test { guide ->
            guide.suggest("RB-T5", "The rate is wrong.", platform)

            assertTrue(crash.logs.isEmpty() && crash.keys.isEmpty() && crash.exceptions.isEmpty())
        }
}
