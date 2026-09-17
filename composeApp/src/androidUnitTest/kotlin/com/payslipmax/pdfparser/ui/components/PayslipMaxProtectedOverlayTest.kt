package com.payslipmax.pdfparser.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
class PayslipMaxProtectedOverlayTest {
    @AfterTest
    fun tearDown() {
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    @Test
    fun appProtectedTitleMatchesParityValue() {
        assertEquals("PayslipMax Protected", AppStrings.appProtectedTitle)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rendersProtectedTitle() =
        runComposeUiTest {
            setContent {
                PayslipMaxProtectedOverlay()
            }

            onNodeWithText(AppStrings.appProtectedTitle).assertIsDisplayed()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rendersShieldIconWithContentDescription() =
        runComposeUiTest {
            setContent {
                PayslipMaxProtectedOverlay()
            }

            onNodeWithContentDescription(AppStrings.appProtectedShieldDesc).assertIsDisplayed()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun propagatesModifierToRoot() =
        runComposeUiTest {
            setContent {
                PayslipMaxProtectedOverlay(
                    modifier = Modifier.testTag("privacy_overlay_root"),
                )
            }

            onNodeWithTag("privacy_overlay_root").assertIsDisplayed()
        }
}
