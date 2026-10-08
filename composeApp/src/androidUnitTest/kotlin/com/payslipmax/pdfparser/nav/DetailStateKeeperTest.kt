package com.payslipmax.pdfparser.nav

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.Screen
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * E7: Pay Audit keeps its month when a Guide card is opened over it and closed again, but a fresh visit starts empty.
 * `rememberSaveable` stands in for Pay Audit's saved month; the keeper is what App wraps every inline detail in.
 */
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class DetailStateKeeperTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun theDetailUnderneathKeepsItsStateAndAFreshVisitStartsEmpty() =
        runComposeUiTest {
            var visits = 0
            val nav = AppNavState(Screen.Insights, listOf(Screen.PayAudit))
            setContent {
                val keeper = rememberDetailStateKeeper(nav.detailStack)
                if (nav.activeDetail != null) {
                    keeper.Provide {
                        if (nav.activeDetail == Screen.PayAudit) {
                            // The initializer runs only when nothing was saved: a new visit gets the next number.
                            val visit = rememberSaveable { mutableIntStateOf(++visits) }
                            Text("audit visit ${visit.intValue}")
                        } else {
                            Text("card")
                        }
                    }
                }
            }
            onNodeWithText("audit visit 1").assertExists()

            nav.push(Screen.GuideCard)
            waitForIdle()
            onNodeWithText("card").assertExists()
            nav.pop()
            waitForIdle()
            onNodeWithText("audit visit 1").assertExists()

            nav.pop()
            waitForIdle()
            nav.push(Screen.PayAudit)
            waitForIdle()
            onNodeWithText("audit visit 2").assertExists()
        }

    @Test
    fun theSameScreenPushedTwiceNeverSharesAKey() {
        assertEquals(3, detailStateKeys(listOf(Screen.PayAudit, Screen.GuideCard, Screen.PayAudit)).toSet().size)
    }
}
