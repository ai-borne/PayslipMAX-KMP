package com.payslipmax.pdfparser.ui.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.ui.theme.AppStrings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScreenBackHeaderTest {
    @AfterTest
    fun tearDown() {
        // RobolectricTestRunner instantiates PayslipApplication, whose onCreate starts Koin.
        // Stop it after each test so a started Koin doesn't leak into the next test class
        // (which would fail its own start with KoinApplicationAlreadyStartedException).
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rendersTitleAndSubtitle() =
        runComposeUiTest {
            setContent {
                ScreenBackHeader(
                    title = "My Title",
                    subtitle = "My Subtitle",
                    onBack = {},
                )
            }

            onNodeWithText("My Title").assertIsDisplayed()
            onNodeWithText("My Subtitle").assertIsDisplayed()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun omitsSubtitleWhenNull() =
        runComposeUiTest {
            setContent {
                ScreenBackHeader(
                    title = "Only Title",
                    onBack = {},
                )
            }

            // Renders with only a title; no crash / no subtitle slot required when subtitle is null.
            onNodeWithText("Only Title").assertIsDisplayed()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun backButtonUsesBtnBackAsContentDescription() =
        runComposeUiTest {
            setContent {
                ScreenBackHeader(
                    title = "Title",
                    onBack = {},
                )
            }

            // The single SSOT back string doubles as the icon's accessibility label.
            onNodeWithContentDescription(AppStrings.btnBack).assertIsDisplayed()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun onBackFiresExactlyOncePerTap() =
        runComposeUiTest {
            var backCount = 0
            setContent {
                ScreenBackHeader(
                    title = "Title",
                    onBack = { backCount++ },
                )
            }

            onNodeWithContentDescription(AppStrings.btnBack).performClick()

            // A single tap must fire onBack exactly once -- guards against a future refactor
            // wiring the callback to both the icon and a wrapping clickable.
            assertEquals(1, backCount)
        }
}
