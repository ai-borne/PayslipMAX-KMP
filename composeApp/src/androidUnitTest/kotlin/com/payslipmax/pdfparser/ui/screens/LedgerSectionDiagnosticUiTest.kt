package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.*
import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.DiagnosticSuggestion
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.PDFParserTheme
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * Phase 3 — Tier 6 diagnostic hint surfacing. The warning icon is a new, independent trigger
 * alongside [com.payslipmax.pdfparser.domain.isFieldLowConfidence] (a diagnosed field must be
 * visible even when confidently wrong), and the dialog hint must only render for the exact
 * [DiagnosticSuggestion.fieldKey] the parser named — this is the case the whole feature exists
 * to catch, so both are asserted directly against rendered UI, not just the pure lookup function.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LedgerSectionDiagnosticUiTest {
    private val diagnosticReason = "Off by ~50000, likely swapped with an adjacent row"

    @AfterTest
    fun tearDown() {
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    private fun payslip(
        basicPayConfidence: Float,
        diagnosticSuggestion: DiagnosticSuggestion?,
    ) = ParsedPayslip(
        file = "test.pdf",
        year = 2024,
        monthNum = 1,
        monthName = "January",
        dateStr = "01/2024",
        officer = Officer("Officer Officer", "16/000/000000X", "AR*****90G"),
        earnings = Earnings(basicPay = 140000.0),
        deductions = Deductions(),
        ledgerBalances = LedgerBalances(),
        summary = PayslipSummary(grossPay = 140000.0, totalDeductions = 0.0, netRemittance = 140000.0),
        taxAndSavings = null,
        fieldConfidence = mapOf("basicPay" to basicPayConfidence),
        diagnosticSuggestion = diagnosticSuggestion,
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun diagnosedFieldShowsWarningIconEvenAtHighConfidence() =
        runComposeUiTest {
            setContent {
                PDFParserTheme {
                    LedgerSection(
                        payslip = payslip(basicPayConfidence = 0.9f, diagnosticSuggestion = DiagnosticSuggestion("basicPay", diagnosticReason)),
                        onItemClick = { _, _ -> },
                    )
                }
            }
            onNodeWithContentDescription(AppStrings.correctionIndicatorDesc).assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun dialogShowsDiagnosticHintForMatchingField() =
        runComposeUiTest {
            setContent {
                PDFParserTheme {
                    LedgerSection(
                        payslip = payslip(basicPayConfidence = 0.9f, diagnosticSuggestion = DiagnosticSuggestion("basicPay", diagnosticReason)),
                        onItemClick = { _, _ -> },
                    )
                }
            }
            onNodeWithContentDescription(AppStrings.correctionIndicatorDesc).performClick()
            onNodeWithText(AppStrings.correctionDiagnosticHintHeading).assertExists()
            onNodeWithText(diagnosticReason).assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun dialogHasNoDiagnosticHintForNonMatchingField() =
        runComposeUiTest {
            setContent {
                PDFParserTheme {
                    LedgerSection(
                        payslip =
                            payslip(
                                basicPayConfidence = 0.3f,
                                diagnosticSuggestion = DiagnosticSuggestion("dsopSubscription", diagnosticReason),
                            ),
                        onItemClick = { _, _ -> },
                    )
                }
            }
            onNodeWithContentDescription(AppStrings.correctionIndicatorDesc).performClick()
            onNodeWithText(AppStrings.correctionDiagnosticHintHeading).assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun dialogHasNoDiagnosticHintWhenNoSuggestion() =
        runComposeUiTest {
            setContent {
                PDFParserTheme {
                    LedgerSection(
                        payslip = payslip(basicPayConfidence = 0.3f, diagnosticSuggestion = null),
                        onItemClick = { _, _ -> },
                    )
                }
            }
            onNodeWithContentDescription(AppStrings.correctionIndicatorDesc).performClick()
            onNodeWithText(AppStrings.correctionDiagnosticHintHeading).assertDoesNotExist()
        }
}
