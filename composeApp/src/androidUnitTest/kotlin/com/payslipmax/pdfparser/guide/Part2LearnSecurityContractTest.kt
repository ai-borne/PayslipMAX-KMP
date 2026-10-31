package com.payslipmax.pdfparser.guide

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Security and resource evidence for Learn about Part II (Orders Bank plan, Phase 2), read from the sources because the property is "this
 * code does not exist": nothing the officer types in the search box reaches a log, telemetry, an exception message, saved state, the
 * clipboard or the network; the saved stack holds ids only; no screen holds a literal of copy or a colour. The file lists are explicit so a
 * moved source fails loudly instead of making a scan vacuous.
 */
class Part2LearnSecurityContractTest {
    private val ui = File("src/commonMain/kotlin/com/payslipmax/pdfparser/ui/screens/guide")
    private val part2 = File(ui, "part2")
    private val domain = File("../shared/src/commonMain/kotlin/com/payslipmax/pdfparser/part2/domain")

    private val screenFiles =
        listOf(
            "Part2CodeScreen.kt",
            "Part2CodeSections.kt",
            "Part2HomeScreen.kt",
            "Part2RibbonScreen.kt",
            "Part2Rows.kt",
            "Part2SearchField.kt",
        )
    private val logicFiles = listOf("Part2Availability.kt", "Part2LookupViewModel.kt")
    private val domainFiles =
        listOf("BankEntry.kt", "BankIndex.kt", "BankStatus.kt", "EntryForm.kt", "Part2Limits.kt", "Part2LookupContent.kt", "Part2LookupIndex.kt", "VaultOrder.kt")

    private fun withoutComments(file: File): String = file.readText().replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "").lines().joinToString("\n") { it.substringBefore("//") }

    private val screens: Map<String, String> by lazy { screenFiles.associateWith { withoutComments(File(part2, it)) } + ("GuideTabPart2.kt" to withoutComments(File(ui, "GuideTabPart2.kt"))) }
    private val logic: Map<String, String> by lazy {
        logicFiles.associateWith { withoutComments(File(part2, it)) } + domainFiles.associateWith { withoutComments(File(domain, it)) } +
            ("Part2Destination.kt" to withoutComments(File(ui, "Part2Destination.kt")))
    }

    @Test
    fun theScansBelowReadTheRealFilesSoTheyAreNotVacuous() {
        assertTrue((screens + logic).values.all { it.length > 100 }, "a Learn source is empty or missing")
        val onDisk = part2.listFiles { f -> f.extension == "kt" }.orEmpty().map { it.name }.toSet()
        assertEquals(screenFiles.toSet() + logicFiles.toSet(), onDisk, "a Learn source was added or moved; list it here so the scans cover it")
        val domainOnDisk = domain.listFiles { f -> f.extension == "kt" }.orEmpty().map { it.name }.toSet()
        assertEquals(domainFiles.toSet(), domainOnDisk, "a Part II domain source was added or moved; list it here so the scans cover it")
    }

    @Test
    fun noLearnCodeLogsPrintsReportsOrBuildsAnExceptionMessageFromAValue() {
        val leaky = Regex("""Logger|println|\bprint\(|\blog\(|Log\.[dievw]\(|Napier|Timber|Firebase|Analytics|CrashReporter|crashReporter|TelemetrySanitizer|setCustomKey|recordException""")
        assertEquals(emptyList(), (screens + logic).filterValues { leaky.containsMatchIn(it) }.keys.toList(), "a Learn file logs, prints or reports")
        val templated = Regex("""(Exception|error|require|check|checkNotNull)\([^)]*\$""")
        assertEquals(emptyList(), (screens + logic).filterValues { templated.containsMatchIn(it) }.keys.toList(), "an exception message interpolates a value")
    }

    @Test
    fun theQueryIsNeverSavedSoOnlyIdsSurviveProcessDeath() {
        val saved = Regex("""rememberSaveable|SavedStateHandle|savedStateHandle|\bSaver\b|listSaver|mapSaver|autoSaver|@Serializable|Parcelize|SharedPreferences|NSUserDefaults|Room|Dao\b""")
        assertEquals(emptyList(), (screens + logic).filterValues { saved.containsMatchIn(it) }.keys.toList(), "typed text was made persistable")
        val saver = withoutComments(File(ui, "GuideNavStateSaver.kt"))
        assertTrue(!Regex("""query|Lookup""", RegexOption.IGNORE_CASE).containsMatchIn(saver), "the Guide saver must know no query")
        val destination = logic.getValue("Part2Destination.kt")
        assertTrue(Regex("""fun encodePart2[^{]*=\s*when""").containsMatchIn(destination) && !destination.contains("query", ignoreCase = true), "a destination is an id and nothing else")
    }

    @Test
    fun aLockDropsTheSearchTextThroughTheGuideNavState() {
        val nav = withoutComments(File(ui, "GuideNavState.kt"))
        assertTrue(Regex("""fun discardTransientForms\(\)\s*\{[^}]*part2Lookup\?\.clear\(\)""", RegexOption.DOT_MATCHES_ALL).containsMatchIn(nav), "a lock must clear the Learn search")
        val app = withoutComments(File("src/commonMain/kotlin/com/payslipmax/pdfparser/App.kt"))
        assertTrue(Regex("""isAppLocked\)\s*\{[^}]*discardTransientForms\(\)""", RegexOption.DOT_MATCHES_ALL).containsMatchIn(app), "App must call it when the app locks")
    }

    @Test
    fun noLearnCodeReachesTheNetworkTheClipboardOrAShareSheet() {
        val outward = Regex("""https?://|ktor|HttpClient|okhttp|java\.net|URL\(|openURL|UriHandler|WebView|setPrimaryClip|ClipData|ClipboardManager|UIPasteboard|ACTION_SEND|shareText|GuidePlatform""", RegexOption.IGNORE_CASE)
        assertEquals(emptyList(), (screens + logic).filterValues { outward.containsMatchIn(it) }.keys.toList())
    }

    @Test
    fun noLearnCodeUsesLookaroundOrBackreferenceRegexThatIsSlowOnKotlinNative() {
        val risky = Regex("""\(\?[=!<]|\\[1-9]""")
        val patterns = (screens + logic).filterValues { source -> Regex("""Regex\(""").containsMatchIn(source) && risky.containsMatchIn(source) }
        assertEquals(emptyList(), patterns.keys.toList())
        assertEquals(emptyList(), logic.filterKeys { it in domainFiles }.filterValues { Regex("""Regex\(|\.toRegex\(""").containsMatchIn(it) }.keys.toList(), "search is plain scans: no regex in the hot path at all")
    }

    @Test
    fun theScreensHoldNoUserFacingCopyAndNoColourBecauseEveryLabelIsLearnStringsOrTheBundleAndEveryColourIsATheme() {
        // A string literal counts as copy when, after its `${...}` and `$name` parts are removed, a letter is left. List keys are identifiers.
        val literal = Regex(""""((?:[^"\\]|\\.)*)"""")
        val listKey = Regex("""key = "[^"]*"""")
        val interpolation = Regex("""\$\{[^}]*}|\$[A-Za-z_][A-Za-z0-9_]*""")
        val copy =
            screens.mapValues { (_, source) ->
                source.lines().filterNot { it.trimStart().startsWith("import ") || it.trimStart().startsWith("package ") }
                    .map { listKey.replace(it, "") }
                    .flatMap { line -> literal.findAll(line).map { it.groupValues[1] }.toList() }
                    .filter { interpolation.replace(it, "").any(Char::isLetter) }
            }.filterValues { it.isNotEmpty() }
        assertEquals(emptyMap(), copy, "user-facing copy belongs in LearnStrings (rule 4)")
        assertEquals(emptyList(), screens.filterValues { Regex("""Color\(|0x[0-9A-Fa-f]{6,8}|Color\.[A-Z]""").containsMatchIn(it) }.keys.toList(), "colours come from the theme (rule 4)")
    }
}
