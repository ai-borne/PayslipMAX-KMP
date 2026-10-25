package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Structural guards on the Due screens (Part II plan, Phase 7a): "the planner owns all timing; neither the UI nor the scheduler computes a
 * date". The Due files must stay blind to what a code means, never log or report anything about a draft, count no days, keep no regex on
 * a hot path, and leave the repository to the one model that only reads it. App.kt, which sat three lines under its limit, must not grow.
 */
class Part2DueSourceTest {
    private val ui = File("src/commonMain/kotlin/com/payslipmax/pdfparser/ui/screens")
    private val folder = File(ui, "guide/part2/due")

    private fun files() = folder.listFiles { f -> f.extension == "kt" }.orEmpty().sortedBy { it.name } + File(ui, "guide/GuideTabPart2Due.kt") + File(ui, "DashboardDueCard.kt")

    private fun withoutComments(source: String): String =
        source.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ").lines().joinToString("\n") { it.substringBefore("//") }

    private fun code(file: File) = withoutComments(file.readText())

    @Test
    fun theFolderIsWhereTheTestExpectsItAndHoldsTheDueFiles() {
        assertTrue(folder.isDirectory, "run from the composeApp module: ${folder.absolutePath}")
        for (expected in listOf("DueViewModel.kt", "DueCardModel.kt", "DueScreen.kt", "DueHomeRow.kt", "DueImportCard.kt", "GuideTabPart2Due.kt", "DashboardDueCard.kt")) {
            assertTrue(expected in files().map { it.name }, "$expected is missing")
        }
    }

    @Test
    fun noDueFileNamesAnyOfTheCodesInTheManual() {
        val text = File("src/commonMain/composeResources/files/guide/guide_bundle.json").readText()
        val codes = assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(text)).bundle.part2!!.serials.flatMap { it.codes }.toSet()
        for (file in files()) {
            val named = codes.filter { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(code(file)) }
            assertEquals(emptyList(), named, "${file.name} names a code; the Due screens are blind to what a code means")
        }
    }

    @Test
    fun noDueFileLogsOrReportsAnything() {
        for (file in files()) {
            val found = listOf("Logger", "CrashReporter", "println(", "TelemetrySanitizer", "printStackTrace", "Log.").filter { it in code(file) }
            assertEquals(emptyList(), found, "${file.name} must not log or report anything about a draft or a reminder")
        }
    }

    @Test
    fun noDueFileCountsDaysOrBuildsAnInstant() {
        // Only formatting a date the planner handed over is allowed; every other calendar call, the clock and the picker's conversion are not.
        for (file in files()) {
            val source = code(file)
            val calendar = Regex("CivilCalendar\\.(\\w+)").findAll(source).map { it.groupValues[1] }.toSet()
            assertEquals(emptySet(), calendar - "format", "${file.name} does calendar arithmetic; the planner owns all timing")
            val found = listOf("PreClaimDates", "86_400_000", "3_600_000", "daysBetween", "dayNumber", "localUtcOffsetMillis", "TimeZone").filter { it in source }
            assertEquals(emptyList(), if (file.name == "DueViewModel.kt") found - "localUtcOffsetMillis" else found, "${file.name} reaches for time arithmetic")
        }
    }

    @Test
    fun onlyTheDueModelReadsTheClockAndNoFileUsesRegex() {
        for (file in files()) {
            val source = code(file)
            assertTrue("Regex(" !in source && "toRegex" !in source, "${file.name}: no regex in commonMain")
            if (file.name != "DueViewModel.kt") assertTrue("currentTimeMillis" !in source, "${file.name}: only DueViewModel is given the clock")
        }
    }

    @Test
    fun onlyTheDueModelHoldsTheRepositoryAndItsOneWriteIsADismissal() {
        for (file in files().filter { it.name != "DueViewModel.kt" }) {
            val source = code(file)
            assertTrue("repository." !in source && "Part2Repository" !in source, "${file.name}: a screen goes through the Due model")
        }
        val model = code(File(folder, "DueViewModel.kt"))
        assertEquals(emptyList(), listOf("saveOrder", "deleteDraft", "deleteOrder", "addPage", "Room", "Dao", "CryptoHelper").filter { it in model }, "the Due list never touches an order or deletes anything")
        // The one write is a dismissal: the draft as it was, with one more name in its dismissed set and nothing else changed.
        assertEquals(1, Regex("saveDraft\\(").findAll(model).count(), "one write, in dismiss")
        assertTrue("saveDraft(draft.copy(dismissed = draft.dismissed + task))" in model, "the write changes the dismissed field alone")
    }

    @Test
    fun theCardKeepsNothingOnDiskAndAppKtDoesNotGrow() {
        for (name in listOf("DueCardModel.kt", "DueImportCard.kt")) {
            val source = code(File(folder, name))
            assertEquals(emptyList(), listOf("Settings", "Preferences", "SharedPreferences", "Keychain", "Serializable", "File(").filter { it in source }, "$name: 'Not now' lives in memory only")
        }
        val app = File("src/commonMain/kotlin/com/payslipmax/pdfparser/App.kt")
        assertTrue(app.readLines().size <= MAX_APP_LINES, "App.kt is ${app.readLines().size} lines, the limit is $MAX_APP_LINES: move code out, never grow it")
    }

    @Test
    fun theAppAndTheDashboardWireTheCardToTheDueListAndTheCardWaitsForTheFeature() {
        // why: the Dashboard is not driven by a test with the real subscription, so the three links are pinned here: App.kt hands the Dashboard
        // the opener, the Dashboard shows the card, and the card does nothing (asks Koin for nothing) while Part II is off.
        val app = File("src/commonMain/kotlin/com/payslipmax/pdfparser/App.kt").readText()
        assertTrue("onOpenDue = { openDue(guideNavState, onNavigate) }" in app, "App.kt must hand the Dashboard the Due opener")
        assertTrue("DashboardDueCard(viewModel, onOpenDue)" in File(ui, "DashboardScreen.kt").readText(), "the Dashboard must show the card")
        val card = code(File(ui, "DashboardDueCard.kt"))
        assertTrue(card.indexOf("isPart2Enabled()") in 0 until card.indexOf("koinInject<"), "the flag is checked before Koin is asked for the model")
        assertTrue("FeatureGate.PART2_ORDERS" in card && "ImportUiState.Success" in card, "the card follows the Part II entitlement and the import succeeding")
    }

    private companion object {
        const val MAX_APP_LINES = 297
    }
}
