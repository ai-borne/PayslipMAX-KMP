package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Structural guards on the Part II reminder files (Phase 7b), in the pattern of [Part2DueSourceTest]: "the planner owns all timing; neither
 * the UI nor the scheduler computes a date", and nothing about a draft leaves the app except a reminder kind and a draft id. Each rule
 * names every file it covers, so a new file in a reminder folder is covered at once and a moved one fails the folder check.
 */
class Part2ReminderSourceTest {
    private val pkg = "com/payslipmax/pdfparser"
    private val shared = File("../shared/src/commonMain/kotlin/$pkg/part2/reminders")
    private val ui = File("src/commonMain/kotlin/$pkg/ui/screens/guide/part2/reminders")
    private val android = File("src/androidMain/kotlin/$pkg/reminders")
    private val ios = File("src/iosMain/kotlin/$pkg/reminders")
    private val nav = File("src/commonMain/kotlin/$pkg/nav")

    private fun kt(folder: File) = folder.listFiles { f -> f.extension == "kt" }.orEmpty().sortedBy { it.name }

    private fun all(): List<File> =
        kt(shared) + kt(ui) + kt(android) + kt(ios) + File(nav, "PendingPart2Link.kt") + File(nav, "PendingLinkStackReset.kt") +
            File("src/commonMain/kotlin/$pkg/Part2LinkEffect.kt") + File("src/commonMain/kotlin/$pkg/ui/screens/ReminderDelaySwitch.kt") +
            File("src/commonMain/kotlin/$pkg/di/Part2Reminders.kt")

    private fun code(file: File): String =
        file.readText().replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ").lines().joinToString("\n") { it.substringBefore("//") }

    private fun text(path: String) = File(path).readText()

    @Test
    fun theFoldersAreWhereTheTestExpectsThemAndHoldTheReminderFiles() {
        for (folder in listOf(shared, ui, android, ios)) assertTrue(folder.isDirectory, "run from the composeApp module: ${folder.absolutePath}")
        val names = all().map { it.name }
        for (expected in listOf("ReminderSync.kt", "ReminderFireCheck.kt", "ReminderScheduler.kt", "ShortDelayReminderScheduler.kt", "ReminderPromptModel.kt", "ReminderWorker.kt", "ReminderNotifier.kt", "AndroidReminderScheduler.kt", "IosReminderBridge.kt", "PendingPart2Link.kt", "Part2LinkEffect.kt")) {
            assertTrue(expected in names, "$expected is missing")
        }
        assertTrue(all().all { it.isFile }, "a listed reminder file does not exist")
    }

    @Test
    fun noReminderFileNamesAnyOfTheCodesInTheManual() {
        val bundle = File("src/commonMain/composeResources/files/guide/guide_bundle.json").readText()
        val codes = assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(bundle)).bundle.part2!!.serials.flatMap { it.codes }.toSet()
        for (file in all()) {
            val named = codes.filter { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(code(file)) }
            assertEquals(emptyList(), named, "${file.name} names a code; reminders are blind to what a code means")
        }
    }

    @Test
    fun noReminderFileLogsReportsOrPrintsAnything() {
        for (file in all()) {
            val found = listOf("Logger", "CrashReporter", "println(", "TelemetrySanitizer", "printStackTrace", "Log.", "NSLog", "Crashlytics").filter { it in code(file) }
            assertEquals(emptyList(), found, "${file.name} must not log or report anything about a draft or a reminder")
        }
    }

    @Test
    fun noReminderFileDoesCalendarArithmeticOrUsesRegex() {
        // The planner owns all timing. A scheduler may subtract "now" from the planner's instant, and the debug decorator may replace an instant
        // with "soon"; nothing here may turn an instant into a day, count days or look at a zone.
        for (file in all()) {
            val source = code(file)
            val found = listOf("CivilCalendar", "CalendarDate", "dayNumber", "daysBetween", "86_400_000", "TimeZone", "localUtcOffsetMillis", "java.util.Calendar", "NSCalendar", "LocalDate").filter { it in source }
            assertEquals(emptyList(), found, "${file.name} reaches for calendar arithmetic; the planner owns all timing")
            if (file.path.contains("commonMain")) assertTrue("Regex(" !in source && "toRegex" !in source, "${file.name}: no regex in commonMain")
        }
    }

    @Test
    fun onlyTheSyncAndTheFireCheckReadTheRepositoryAndNeitherWritesToIt() {
        for (file in all().filter { it.name !in setOf("ReminderSync.kt", "ReminderFireCheck.kt", "PendingPart2Link.kt") }) {
            assertTrue("Part2Repository" !in code(file) && "repository." !in code(file), "${file.name}: a scheduler, notifier or screen gets reminders, not the repository")
        }
        for (name in listOf("ReminderSync.kt", "ReminderFireCheck.kt", "PendingPart2Link.kt")) {
            val source = code(all().first { it.name == name })
            assertEquals(emptyList(), listOf("saveDraft", "saveOrder", "deleteDraft", "deleteOrder", "addPage").filter { it in source }, "$name only reads")
        }
    }

    @Test
    fun nothingStoresANewFieldAndNothingHoldsStateInAnObject() {
        for (file in all()) {
            val source = code(file)
            assertFalse("@Serializable" in source || "Room" in source || "Dao" in source, "${file.name} stores something: a new stored field is a decision")
            assertFalse(Regex("(?m)^\\s*(?:internal |private |public )?object \\w+").containsMatchIn(source) && "var " in source, "${file.name}: no object with a var; hold state in a Koin-scoped class")
        }
    }

    @Test
    fun theNotificationIsExplicitImmutableAndLocal() {
        val notifier = code(File(android, "ReminderNotifier.kt"))
        assertTrue("FLAG_IMMUTABLE" in notifier && "FLAG_MUTABLE" !in notifier, "an immutable PendingIntent")
        val link = code(File(android, "ReminderLinkIntent.kt"))
        assertTrue("Intent(context, MainActivity::class.java)" in link, "an explicit intent for our own activity")
        for (file in kt(android)) {
            val source = code(file)
            assertEquals(emptyList(), listOf("ClipboardManager", "java.net", "HttpURLConnection", "okhttp", "WebView", "ContentResolver", "FileProvider", "sendBroadcast", "startService").filter { it in source }, "${file.name}: no clipboard, network, file sharing or broadcast")
        }
    }

    @Test
    fun theWorkersInputIsTheKindAndTheDraftIdAndNothingElse() {
        val scheduler = code(File(android, "AndroidReminderScheduler.kt"))
        assertEquals(1, Regex("workDataOf\\(").findAll(scheduler).count())
        assertTrue("workDataOf(ReminderWorker.KIND to reminder.kind.name, ReminderWorker.DRAFT to reminder.draftId)" in scheduler)
        assertTrue("setRequiresNetwork" !in scheduler && "Constraints" !in scheduler, "no constraint: the job needs nothing from the network")
    }

    @Test
    fun theEntryPointsStartTheSyncOnceAndHandTheTapToThePendingLinkOnlyOnAFreshLaunchOrAnNewIntent() {
        val activity = text("src/androidMain/kotlin/$pkg/MainActivity.kt")
        assertEquals(1, Regex("startPart2Reminders\\(").findAll(activity).count())
        assertTrue("if (savedInstanceState == null) offerReminderTap(intent)" in activity, "a recreated activity re-reads the same intent")
        assertTrue(Regex("override fun onNewIntent\\(intent: Intent\\) \\{[^}]*offerReminderTap\\(intent\\)").containsMatchIn(activity))
        val application = text("src/androidMain/kotlin/$pkg/PayslipApplication.kt")
        assertFalse("startPart2Reminders" in application, "a worker's cold process must not rebuild the schedule it is running from")
        assertTrue("startPart2Reminders(koin, viewModel)" in text("src/iosMain/kotlin/$pkg/MainViewController.kt"))
    }

    @Test
    fun appKtGainsOneCallAndStaysUnderItsPin() {
        val app = File("src/commonMain/kotlin/$pkg/App.kt")
        assertEquals(1, Regex("Part2LinkEffect\\(navState, guideNavState\\)").findAll(app.readText()).count())
        assertTrue(app.readLines().size <= 297, "App.kt is ${app.readLines().size} lines: move code out, never grow it")
        val guideTab = File("src/commonMain/kotlin/$pkg/ui/screens/guide/GuideTab.kt")
        assertTrue(guideTab.readLines().size <= 263, "GuideTab.kt is pinned at 263")
    }

    @Test
    fun theAppDeclaresTheNotificationPermissionAndNothingElseNew() {
        val manifest = text("src/androidMain/AndroidManifest.xml")
        assertEquals(listOf("android.permission.POST_NOTIFICATIONS"), Regex("<uses-permission android:name=\"([^\"]+)\"").findAll(manifest).map { it.groupValues[1] }.toList())
        assertFalse("exported=\"true\"" in manifest.replace(Regex("<activity\\s+android:name=\".MainActivity\"[^>]*>"), ""), "no new exported component")
    }

    @Test
    fun theLinkEffectChecksTheFlagBeforeAskingKoinAndTheEntitlementIsPartTwoOrders() {
        val effect = code(File("src/commonMain/kotlin/$pkg/Part2LinkEffect.kt"))
        assertTrue(effect.indexOf("isPart2Enabled()") in 0 until effect.indexOf("koinInject()"), "the flag is checked before Koin is asked for the link")
        assertTrue("FeatureGate.PART2_ORDERS" in code(File("src/commonMain/kotlin/$pkg/di/Part2Reminders.kt")), "reminders follow the Part II entitlement, not another gate")
        assertTrue(code(File("src/commonMain/kotlin/$pkg/ui/screens/ReminderDelaySwitch.kt")).let { it.indexOf("if (!enabled || !debug) return") in 0 until it.indexOf("koinInject<") })
    }
}
