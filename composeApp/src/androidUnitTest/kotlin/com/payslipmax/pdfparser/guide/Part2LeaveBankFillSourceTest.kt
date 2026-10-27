package com.payslipmax.pdfparser.guide

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Structural guards on the Leave Bank fill's screens and wiring (plan, phase 8b). `Part2DraftSourceTest` already keeps the draft folder blind to
 * codes, logs and payslips; these add what is specific to the fill: the summary is read from the view model in ONE new place, the
 * break-up box has several lines, and the Guide tab attaches the feed once.
 */
class Part2LeaveBankFillSourceTest {
    private val main = File("src/commonMain/kotlin/com/payslipmax/pdfparser")

    private fun text(path: String) = File(main, path).readText()

    private fun withoutComments(source: String): String =
        source.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ").lines().joinToString("\n") { it.substringBefore("//") }

    @Test
    fun theSummaryIsReadFromTheViewModelInOneNewPlaceOnly() {
        val readers = main.walkTopDown().filter { it.extension == "kt" && "leaveBankSummary" in withoutComments(it.readText()) }.map { it.name }.sorted().toList()
        assertEquals(listOf("InsightsScreen.kt", "LeaveAccumulationSection.kt", "Part2LeaveBank.kt", "PayslipViewModel.kt", "RetirementCalculatorsScreen.kt"), readers, "a new reader of the summary is a second derivation in the making: add it here on purpose")
    }

    @Test
    fun noFillFileDerivesTheSummaryOrReachesForAPayslip() {
        for (name in listOf("LeaveBankFiller.kt", "LeaveBankView.kt", "LeaveBankFillView.kt", "FillMessage.kt", "YearsBox.kt")) {
            val code = withoutComments(text("ui/screens/guide/part2/draft/$name"))
            val found = listOf("LeaveBankCalculator", "summarize(", "ParsedPayslip", "PayslipViewModel", "leaveBankSummary").filter { it in code }
            assertEquals(emptyList(), found, "$name must take the rows through LeaveBankSource only")
        }
    }

    @Test
    fun theBreakUpBoxHasSeveralLinesAndIsNotTheSingleLineBox() {
        val box = withoutComments(text("ui/screens/guide/part2/draft/YearsBox.kt"))
        assertTrue("singleLine = false" in box && "minLines" in box, "a career's break-up is read and edited in a box of several lines")
        assertTrue("PreClaimTextBox" !in box)
    }

    @Test
    fun theGuideTabAttachesTheFeedOnceFromTheViewModelItAlreadyHolds() {
        val route = withoutComments(text("ui/screens/guide/GuideTabRoute.kt"))
        assertEquals(1, Regex("attachPart2LeaveBank\\(koin, viewModel\\)").findAll(route).count())
        assertTrue("LaunchedEffect(viewModel)" in route)
    }

    @Test
    fun noScreenFileDecidesWhichCodesCanBeFilled() {
        // why: the rule families register the fill per code; a screen that tested the code itself would need an edit for every new code.
        for (name in listOf("LeaveDraftSections.kt", "LeaveBankFillView.kt", "LeaveDraftFields.kt", "SlipPreviewScreen.kt", "LeaveDraftScreen.kt")) {
            val code = withoutComments(text("ui/screens/guide/part2/draft/$name"))
            assertTrue("code ==" !in code && ".code ==" !in code && "when (code" !in code, "$name tests a code")
        }
    }
}
