package com.payslipmax.pdfparser.guide

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Security evidence for the TA/DA pre-claim flow (plan 22, P6, checklist 1b), read from the sources because the property is "this
 * code does not exist": nothing the officer types reaches a log, telemetry, an exception message, saved state, the clipboard or the
 * network, and the only telemetry is one breadcrumb with the move type. The file lists are explicit so a moved source fails loudly
 * instead of making a scan vacuous; the P7 screens are listed separately and carry their own, stricter checks (no literal copy or colour, per-field copy only, nothing saved).
 */
class PreClaimSecurityContractTest {
    private val app = File("src/commonMain/kotlin/com/payslipmax/pdfparser/ui/screens/guide/preclaim")
    private val calculators = File("../shared/src/commonMain/kotlin/com/payslipmax/pdfparser/guide/preclaim")

    private val flowFiles =
        listOf(
            "PreClaimAvailability.kt", "PreClaimFieldValues.kt", "PreClaimFormState.kt", "PreClaimInput.kt", "PreClaimMoveType.kt",
            "PreClaimRequests.kt", "PreClaimViewModel.kt", "PreClaimWorksheet.kt", "PreClaimWorksheetBuilder.kt",
        )

    private val screenFiles =
        listOf(
            "PreClaimControls.kt", "PreClaimDateBox.kt", "PreClaimDates.kt", "PreClaimDocumentSection.kt", "PreClaimEstimateSection.kt", "PreClaimFieldSection.kt",
            "PreClaimJourneySection.kt", "PreClaimProfileSection.kt", "PreClaimQuestionSection.kt", "PreClaimRelatedCards.kt", "PreClaimRoute.kt",
            "PreClaimScreen.kt", "PreClaimSectionFrame.kt", "PreClaimTemporaryDutySection.kt", "PreClaimWording.kt",
        )

    private fun withoutComments(file: File): String = file.readText().replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "").lines().joinToString("\n") { it.substringBefore("//") }

    private val flow: Map<String, String> by lazy { flowFiles.associateWith { withoutComments(File(app, it)) } }
    private val screens: Map<String, String> by lazy { screenFiles.associateWith { withoutComments(File(app, it)) } }

    /** The Guide-side glue (entry row, card button, destination) is part of the screens even though it lives in the guide package. */
    private val glue: Map<String, String> by lazy { mapOf("GuideTabPreClaim.kt" to withoutComments(File(app, "../GuideTabPreClaim.kt"))) }
    private val drawn: Map<String, String> get() = screens + glue
    private val calculatorSources: Map<String, String> by lazy { calculators.listFiles { f -> f.extension == "kt" }.orEmpty().associate { it.name to withoutComments(it) } }

    @Test
    fun theScansBelowReadTheRealFilesSoTheyAreNotVacuous() {
        assertEquals(flowFiles.size, flow.size)
        assertTrue(drawn.values.all { it.length > 100 }, "a screen file is empty or missing")
        assertTrue(flow.values.all { it.length > 100 }, "a flow file is empty or missing")
        assertTrue(calculatorSources.size >= 12, "found only ${calculatorSources.size} calculator files; the scan root is wrong")
        val onDisk = app.listFiles { f -> f.extension == "kt" }.orEmpty().map { it.name }.toSet()
        assertEquals(flowFiles.toSet() + screenFiles.toSet(), onDisk, "a pre-claim source was added or moved; list it here so the scans cover it")
    }

    @Test
    fun onlyTheViewModelTouchesTelemetryAndItWritesOneBreadcrumbWithTheMoveTypeOnly() {
        val users = (flow + drawn).filterValues { Regex("""CrashReporter|crashReporter|TelemetrySanitizer""").containsMatchIn(it) }.keys
        assertEquals(setOf("PreClaimViewModel.kt"), users)
        val model = flow.getValue("PreClaimViewModel.kt")
        assertEquals(listOf("crashReporter.log(\"\$BREADCRUMB\${move.key}\")"), Regex("""crashReporter\.[^\n]*""").findAll(model).map { it.value }.toList(), "exactly one telemetry call: the breadcrumb and the move key")
        assertTrue(model.contains("const val BREADCRUMB = \"preclaim_opened_\""))
        assertTrue((flow + drawn).values.none { Regex("""setCustomKey|recordException|setUserId""").containsMatchIn(it) }, "no custom key and no exception report from the worksheet")
    }

    @Test
    fun noPreClaimCodeLogsPrintsOrBuildsAnExceptionMessageFromAValue() {
        val leaky = Regex("""Logger|println|\bprint\(|\blog\(|Log\.[dievw]\(|Napier|Timber|Firebase|Analytics""")
        val everything = flow + drawn + calculatorSources
        assertEquals(emptyList(), everything.filterValues { leaky.containsMatchIn(it.replace("crashReporter.log(", "")) }.keys.toList(), "a pre-claim file logs or prints")
        val templated = Regex("""(Exception|error|require|check|checkNotNull)\([^)]*\$""")
        assertEquals(emptyList(), everything.filterValues { templated.containsMatchIn(it) }.keys.toList(), "an exception message interpolates a value")
    }

    @Test
    fun theFormIsNeverSavedSoProcessDeathALockAndLeavingTheScreenDiscardIt() {
        val saved = Regex("""rememberSaveable|SavedStateHandle|savedStateHandle|\bSaver\b|listSaver|mapSaver|autoSaver|@Serializable|Parcelize|SharedPreferences|NSUserDefaults|Room|Dao\b""")
        assertEquals(emptyList(), (flow + drawn).filterValues { saved.containsMatchIn(it) }.keys.toList(), "typed values were made persistable")
        assertEquals(emptyList(), calculatorSources.filterValues { Regex("""@Serializable|SharedPreferences|NSUserDefaults|Room""").containsMatchIn(it) }.keys.toList())
    }

    @Test
    fun noPreClaimCodeReachesTheNetworkTheClipboardOrAShareSheet() {
        val outward = Regex("""https?://|ktor|HttpClient|okhttp|java\.net|URL\(|openURL|UriHandler|WebView|setPrimaryClip|ClipData|ClipboardManager|UIPasteboard|ACTION_SEND|shareText|GuidePlatform""", RegexOption.IGNORE_CASE)
        assertEquals(emptyList(), (flow + calculatorSources).filterValues { outward.containsMatchIn(it) }.keys.toList())
    }

    @Test
    fun noPreClaimCodeUsesLookaroundOrBackreferenceRegexThatIsSlowOnKotlinNative() {
        val risky = Regex("""\(\?[=!<]|\\[1-9]""")
        val patterns = (flow + drawn + calculatorSources).filterValues { source -> Regex("""Regex\(""").containsMatchIn(source) && risky.containsMatchIn(source) }
        assertEquals(emptyList(), patterns.keys.toList())
    }

    @Test
    fun theFlowHoldsNoColourOrUserFacingCopyBecauseItDrawsNothing() {
        assertEquals(emptyList(), flow.filterValues { Regex("""Color\(|0x[0-9A-Fa-f]{6,8}|@Composable|\bText\(""").containsMatchIn(it) }.keys.toList(), "the P6 flow is headless: copy comes from the bundle, screens come in P7")
    }

    @Test
    fun theScreensHoldNoUserFacingCopyBecauseEveryLabelComesFromPreClaimStringsOrTheBundle() {
        // A string literal counts as copy when, after its `${...}` and `$name` parts are removed, a letter is left. List keys are identifiers.
        val literal = Regex(""""((?:[^"\\]|\\.)*)"""")
        val listKey = Regex("""key = "[^"]*"""") // a lazy-list key is an identifier, not copy; only that part of its line is skipped
        val interpolation = Regex("""\$\{[^}]*}|\$[A-Za-z_][A-Za-z0-9_]*""")
        val copy =
            drawn.mapValues { (_, source) ->
                source.lines().filterNot { it.trimStart().startsWith("import ") || it.trimStart().startsWith("package ") }
                    .map { listKey.replace(it, "") }
                    .flatMap { line -> literal.findAll(line).map { it.groupValues[1] }.toList() }
                    .filter { interpolation.replace(it, "").any(Char::isLetter) }
            }.filterValues { it.isNotEmpty() }
        assertEquals(emptyMap(), copy, "user-facing copy belongs in PreClaimStrings (rule 4)")
    }

    @Test
    fun theScreensHoldNoColourAndAskTheThemeForEverything() {
        val colours = Regex("""\bColor\(|\bColor\.|0x[0-9A-Fa-f]{6,8}|\bRgb\(|\.copy\(alpha""")
        assertEquals(emptyList(), drawn.filterValues { colours.containsMatchIn(it) }.keys.toList(), "colours come from the theme tokens only (rule 4)")
    }

    @Test
    fun copyIsPerFieldOnlyWithNoCopyAllNoShareSheetNoEmailAndNoExport() {
        val outward = Regex("""shareText|shareTextViaEmail|\.share\(|\.email\(|ACTION_SEND|Intent|UriHandler|openUri|FileProvider|writeText|ClipData""")
        assertEquals(emptyList(), drawn.filterValues { outward.containsMatchIn(it) }.keys.toList(), "the worksheet shares or exports")
        val copyCalls = drawn.filterValues { Regex("""\bcopy\(""").containsMatchIn(it) }.keys
        assertEquals(setOf("PreClaimFieldSection.kt"), copyCalls, "only the field list may put anything on the clipboard")
        val fieldList = screens.getValue("PreClaimFieldSection.kt")
        assertTrue(fieldList.contains("copy(value)"), "the button copies the one value of its own box")
        assertTrue(!fieldList.contains("joinToString") && !fieldList.contains("copyValues.values") && !fieldList.contains("copyValues.entries"), "a copy-all would join the values")
        assertEquals(1, Regex("""platform\.copy""").findAll(fieldList).count(), "one clipboard hand-off, passed to each row")
    }

    @Test
    fun theScreensUseNoPlatformViewControllerSoNothingEscapesTheThemedIosFactory() {
        val native = Regex("""UIViewController|UIKit|platform\.UIKit|NSObject|android\.app|android\.content|AlertDialog""")
        assertEquals(emptyList(), drawn.filterValues { native.containsMatchIn(it) }.keys.toList())
    }

    @Test
    fun noScreenReadsPayslipsOrStorageItAsksTheViewModelForEverything() {
        val data = Regex("""Repository|Dao\b|PayslipViewModel|GuideProfileProvider|koinInject|getKoin|inject\(""")
        assertEquals(emptyList(), drawn.filterValues { data.containsMatchIn(it) }.keys.toList(), "a screen reaches for data itself")
    }

    @Test
    fun theLockDiscardsTheHeldWorksheetAndNothingSavesIt() {
        val appSource = withoutComments(File("src/commonMain/kotlin/com/payslipmax/pdfparser/App.kt"))
        assertTrue(
            Regex("""isAppLocked\)\s*\{[^}]*LaunchedEffect\(Unit\)\s*\{\s*guideNavState\?\.discardTransientForms\(\)\s*\}""", RegexOption.DOT_MATCHES_ALL).containsMatchIn(appSource),
            "App must discard the worksheet when the app locks",
        )
        val nav = withoutComments(File("src/commonMain/kotlin/com/payslipmax/pdfparser/ui/screens/guide/GuideNavState.kt"))
        assertTrue(nav.contains("if (next.none { it is GuideDestination.PreClaim }) discardPreClaim()"), "leaving the worksheet must drop it")
        val saver = withoutComments(File("src/commonMain/kotlin/com/payslipmax/pdfparser/ui/screens/guide/GuideNavStateSaver.kt"))
        assertTrue(!saver.contains("preClaimModel"), "the held worksheet must never be saved")
    }
}
