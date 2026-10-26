package com.payslipmax.pdfparser

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.payslipmax.pdfparser.nav.AppNavState
import com.payslipmax.pdfparser.nav.PendingPart2Link
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.testing.FakePart2Repository
import com.payslipmax.pdfparser.testing.WithTestKoin
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * WHY: the effect is the app's one call. A reminder tap must close whatever detail is open, land on the Guide tab at Due, and do nothing at
 * all (not even ask Koin) while Part II is off.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Part2LinkEffectUiTest {
    @get:Rule
    val composeRule: AndroidComposeTestRule<*, ComponentActivity> = createAndroidComposeRule<ComponentActivity>()

    private val repository = FakePart2Repository()
    private val pending = PendingPart2Link(repository)
    private val nav = AppNavState(currentTab = Screen.Settings).also { it.push(Screen.PremiumFeatures) }
    private val guide = GuideNavState()

    private fun show(enabled: Boolean) {
        runBlocking { repository.saveDraft(Part2Draft("d1", LeaveEventInput(emptyList()), emptyMap(), 1, 1, null)) }
        pending.offer("RETURN", "d1")
        composeRule.setContent { WithTestKoin(module { single { pending } }) { Part2LinkEffect(nav, guide, enabled) } }
        composeRule.waitForIdle()
    }

    @Test
    fun aTapClosesTheOpenDetailAndLandsOnTheGuideTabAtDue() {
        show(enabled = true)

        assertEquals(Screen.Guide, nav.currentTab)
        assertNull(nav.activeDetail)
        assertEquals(Part2Destination.Due, guide.current)
    }

    @Test
    fun withPartTwoOffNothingHappensAndTheTapIsNotTaken() {
        show(enabled = false)

        assertEquals(Screen.PremiumFeatures, nav.activeDetail)
        assertEquals(Screen.Settings, nav.currentTab)
        assertEquals("d1", pending.link.value?.draftId)
    }
}
