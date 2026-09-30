package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.ui.pcdao.AppStringsPcdao
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReplicaAuditActionCardTest {
    @AfterTest
    fun tearDown() {
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun replicaAuditActionCard_displaysCtaTextAndDescription() =
        runComposeUiTest {
            setContent {
                ReplicaAuditActionCard(onAuditClick = {})
            }

            onNodeWithTag("replica_audit_action_card").assertIsDisplayed()
            onNodeWithText(AppStringsPcdao.replicaAuditCta).assertIsDisplayed()
            onNodeWithText(AppStringsPcdao.screenDescription).assertIsDisplayed()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun clickingCard_invokesCallback() =
        runComposeUiTest {
            var clicked = false
            setContent {
                ReplicaAuditActionCard(onAuditClick = { clicked = true })
            }

            onNodeWithTag("replica_audit_action_card").performClick()
            assertTrue(clicked, "Expected onAuditClick to be invoked when replica audit card is clicked")
        }
}
