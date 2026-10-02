package com.payslipmax.pdfparser.ui.screens

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** A release build registers nothing, so Settings shows no developer-only section there. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeveloperToolsRegistryTest {
    @AfterTest
    fun tearDown() = DeveloperToolsRegistry.clear()

    @Test
    fun anEmptyRegistryRendersNothing() =
        runComposeUiTest {
            DeveloperToolsRegistry.clear()
            setContent { DeveloperToolsRegistry.Render() }
            assertEquals(0, DeveloperToolsRegistry.count())
        }

    @Test
    fun aRegisteredSectionIsRendered() =
        runComposeUiTest {
            DeveloperToolsRegistry.register("t") { Text("registered section") }
            setContent { DeveloperToolsRegistry.Render() }
            onNodeWithText("registered section").assertIsDisplayed()
        }
}
