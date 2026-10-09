package com.payslipmax.pdfparser.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.AppStringsSupport
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Characterization of Settings > Report an Issue, written before the dialog's layout moved into a shared composable (M5), so
 * that move cannot change what the support dialog shows or sends: its copy, the Send button staying off until there is text,
 * the 1000-character cap, and which callbacks run on Send and on Cancel.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReportIssueDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val sent = mutableListOf<String>()
    private var dismissed = 0

    private fun show() {
        composeRule.setContent { ReportIssueDialog(onDismiss = { dismissed++ }, onSend = { sent += it }) }
        composeRule.waitForIdle()
    }

    private fun type(text: String) {
        composeRule.onNode(hasSetTextAction()).performTextInput(text)
        composeRule.waitForIdle()
    }

    @Test
    fun itShowsItsTitleItsPrivacyNoticeItsFieldLabelAndBothButtons() {
        show()

        for (text in listOf(AppStringsSupport.reportIssueDialogTitle, AppStringsSupport.reportIssuePrivacyNotice, AppStringsSupport.reportIssueDescriptionLabel, AppStringsSupport.reportIssueSendBtn, AppStrings.btnCancel)) {
            composeRule.onNodeWithText(text).assertIsDisplayed()
        }
    }

    @Test
    fun sendStaysOffForEmptyAndBlankTextAndComesOnWithRealText() {
        show()
        composeRule.onNodeWithText(AppStringsSupport.reportIssueSendBtn).assertIsNotEnabled()

        type("   ")
        composeRule.onNodeWithText(AppStringsSupport.reportIssueSendBtn).assertIsNotEnabled()

        type("it crashed")
        composeRule.onNodeWithText(AppStringsSupport.reportIssueSendBtn).assertIsEnabled()
    }

    @Test
    fun theFieldKeepsAtMostAThousandCharacters() {
        show()

        type("a".repeat(1200))
        composeRule.onNodeWithText(AppStringsSupport.reportIssueSendBtn).performClick()

        assertEquals(listOf("a".repeat(1000)), sent)
    }

    @Test
    fun sendPassesTheTextOnceAndThenDismisses() {
        show()

        type("it crashed")
        composeRule.onNodeWithText(AppStringsSupport.reportIssueSendBtn).performClick()

        assertEquals(listOf("it crashed"), sent)
        assertEquals(1, dismissed)
    }

    @Test
    fun cancelDismissesAndSendsNothing() {
        show()

        type("half typed")
        composeRule.onNodeWithText(AppStrings.btnCancel).performClick()

        assertEquals(emptyList(), sent)
        assertEquals(1, dismissed)
    }
}
