package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.*
import com.payslipmax.pdfparser.ui.theme.AppStrings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UploadWidgetTest {
    @AfterTest
    fun tearDown() {
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun passwordVisibilityTogglesOnIconClick() =
        runComposeUiTest {
            setContent {
                UploadWidget(
                    isLoading = false,
                    error = null,
                    success = false,
                    onPickPdfTrigger = {},
                    onClearError = {},
                    onDismiss = {},
                )
            }

            // Password starts hidden -- toggle offers to reveal it.
            onNodeWithText(AppStrings.showPasswordToggle).assertExists()
            onNodeWithText(AppStrings.hidePasswordToggle).assertDoesNotExist()

            onNodeWithText(AppStrings.showPasswordToggle).performClick()

            // After the click, the toggle now offers to hide the revealed password.
            onNodeWithText(AppStrings.hidePasswordToggle).assertExists()
            onNodeWithText(AppStrings.showPasswordToggle).assertDoesNotExist()
        }
}
