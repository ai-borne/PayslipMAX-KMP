package com.payslipmax.pdfparser.guide

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Security evidence for the TA/DA pre-claim flow (plan 22, P6, checklist 1b), read from the sources because the property is "this
 * code does not exist": nothing the officer types reaches a log, telemetry, an exception message, saved state, the clipboard or the
 * network, and the only telemetry is one breadcrumb with the move type. The file lists are explicit so a moved source fails loudly
 * instead of making a scan vacuous; the screens of P7 will join the list with their own, stricter checks (per-field copy only).
 */
class PreClaimSecurityContractTest {
    private val app = File("src/commonMain/kotlin/com/payslipmax/pdfparser/ui/screens/guide/preclaim")
    private val calculators = File("../shared/src/commonMain/kotlin/com/payslipmax/pdfparser/guide/preclaim")

    private val flowFiles =
        listOf(
            "PreClaimAvailability.kt", "PreClaimFieldValues.kt", "PreClaimFormState.kt", "PreClaimInput.kt", "PreClaimMoveType.kt",
            "PreClaimRequests.kt", "PreClaimViewModel.kt", "PreClaimWorksheet.kt", "PreClaimWorksheetBuilder.kt",
        )

    private fun withoutComments(file: File): String = file.readText().replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "").lines().joinToString("\n") { it.substringBefore("//") }

    private val flow: Map<String, String> by lazy { flowFiles.associateWith { withoutComments(File(app, it)) } }
    private val calculatorSources: Map<String, String> by lazy { calculators.listFiles { f -> f.extension == "kt" }.orEmpty().associate { it.name to withoutComments(it) } }

    @Test
    fun theScansBelowReadTheRealFilesSoTheyAreNotVacuous() {
        assertEquals(flowFiles.size, flow.size)
        assertTrue(flow.values.all { it.length > 100 }, "a flow file is empty or missing")
        assertTrue(calculatorSources.size >= 12, "found only ${calculatorSources.size} calculator files; the scan root is wrong")
        val onDisk = app.listFiles { f -> f.extension == "kt" }.orEmpty().map { it.name }.toSet()
        assertEquals(flowFiles.toSet(), onDisk, "a pre-claim source was added or moved; list it here so the scans cover it")
    }

    @Test
    fun onlyTheViewModelTouchesTelemetryAndItWritesOneBreadcrumbWithTheMoveTypeOnly() {
        val users = flow.filterValues { Regex("""CrashReporter|crashReporter|TelemetrySanitizer""").containsMatchIn(it) }.keys
        assertEquals(setOf("PreClaimViewModel.kt"), users)
        val model = flow.getValue("PreClaimViewModel.kt")
        assertEquals(listOf("crashReporter.log(\"\$BREADCRUMB\${move.key}\")"), Regex("""crashReporter\.[^\n]*""").findAll(model).map { it.value }.toList(), "exactly one telemetry call: the breadcrumb and the move key")
        assertTrue(model.contains("const val BREADCRUMB = \"preclaim_opened_\""))
        assertTrue(flow.values.none { Regex("""setCustomKey|recordException|setUserId""").containsMatchIn(it) }, "no custom key and no exception report from the worksheet")
    }

    @Test
    fun noPreClaimCodeLogsPrintsOrBuildsAnExceptionMessageFromAValue() {
        val leaky = Regex("""Logger|println|\bprint\(|\blog\(|Log\.[dievw]\(|Napier|Timber|Firebase|Analytics""")
        val everything = flow + calculatorSources
        assertEquals(emptyList(), everything.filterValues { leaky.containsMatchIn(it.replace("crashReporter.log(", "")) }.keys.toList(), "a pre-claim file logs or prints")
        val templated = Regex("""(Exception|error|require|check|checkNotNull)\([^)]*\$""")
        assertEquals(emptyList(), everything.filterValues { templated.containsMatchIn(it) }.keys.toList(), "an exception message interpolates a value")
    }

    @Test
    fun theFormIsNeverSavedSoProcessDeathALockAndLeavingTheScreenDiscardIt() {
        val saved = Regex("""rememberSaveable|SavedStateHandle|savedStateHandle|\bSaver\b|listSaver|mapSaver|autoSaver|@Serializable|Parcelize|SharedPreferences|NSUserDefaults|Room|Dao\b""")
        assertEquals(emptyList(), flow.filterValues { saved.containsMatchIn(it) }.keys.toList(), "typed values were made persistable")
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
        val patterns = (flow + calculatorSources).filterValues { source -> Regex("""Regex\(""").containsMatchIn(source) && risky.containsMatchIn(source) }
        assertEquals(emptyList(), patterns.keys.toList())
    }

    @Test
    fun theFlowHoldsNoColourOrUserFacingCopyBecauseItDrawsNothing() {
        assertEquals(emptyList(), flow.filterValues { Regex("""Color\(|0x[0-9A-Fa-f]{6,8}|@Composable|\bText\(""").containsMatchIn(it) }.keys.toList(), "the P6 flow is headless: copy comes from the bundle, screens come in P7")
    }
}
