package com.payslipmax.pdfparser.guide

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Security and resource evidence for the Orders Bank (plan 13, Phase 3), read from the sources because the property is "this code does not
 * exist": nothing about an entry, a code, a date, a note or an order number reaches a log, telemetry, an exception message, the clipboard,
 * a share sheet or the network; what is typed is never persisted by the UI layer; the saved stack and the Bank screens hold ids only; and
 * no Bank screen holds a literal of copy or a colour. The file lists are explicit so a moved source fails loudly instead of making a scan vacuous.
 */
class OrdersBankSecurityContractTest {
    private val bank = File("src/commonMain/kotlin/com/payslipmax/pdfparser/ui/screens/guide/part2/bank")
    private val ui = File("src/commonMain/kotlin/com/payslipmax/pdfparser/ui/screens/guide")
    private val shared = File("../shared/src/commonMain/kotlin/com/payslipmax/pdfparser/part2")

    private val screenFiles = listOf("BankRows.kt", "BankScreen.kt", "EntryFields.kt", "EntryScreen.kt")
    private val logicFiles = listOf("BankViewModel.kt", "EntryEditViewModel.kt")

    private fun withoutComments(file: File): String = file.readText().replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "").lines().joinToString("\n") { it.substringBefore("//") }

    private val screens by lazy { screenFiles.associateWith { withoutComments(File(bank, it)) } + ("GuideTabBank.kt" to withoutComments(File(ui, "GuideTabBank.kt"))) }
    private val logic by lazy {
        logicFiles.associateWith { withoutComments(File(bank, it)) } +
            File(shared, "data").listFiles { f -> f.extension == "kt" }.orEmpty().associate { it.name to withoutComments(it) } +
            listOf("BankEntry.kt", "BankIndex.kt", "BankStatus.kt", "EntryForm.kt", "VaultOrder.kt").associateWith { withoutComments(File(shared, "domain/$it")) }
    }

    @Test
    fun theScansReadTheRealFilesSoTheyAreNotVacuous() {
        assertTrue((screens + logic).values.all { it.length > 100 }, "a Bank source is empty or missing")
        assertEquals((screenFiles + logicFiles).toSet(), bank.listFiles { f -> f.extension == "kt" }.orEmpty().map { it.name }.toSet(), "a Bank source was added or moved; list it here so the scans cover it")
        assertTrue(logic.keys.containsAll(listOf("Part2Codec.kt", "Part2Backup.kt", "RoomPart2Repository.kt", "PageArchive.kt")))
    }

    @Test
    fun noBankCodeLogsPrintsReportsOrBuildsAnExceptionMessageFromAValue() {
        val leaky = Regex("""Logger|println|\bprint\(|\blog\(|Log\.[dievw]\(|Napier|Timber|Firebase|Analytics|CrashReporter|crashReporter|TelemetrySanitizer|setCustomKey|recordException|printStackTrace""")
        assertEquals(emptyList(), (screens + logic).filterValues { leaky.containsMatchIn(it) }.keys.toList(), "a Bank file logs, prints or reports")
        val templated = Regex("""(Exception|error|require|check|checkNotNull)\([^)]*\$""")
        assertEquals(emptyList(), (screens + logic).filterValues { templated.containsMatchIn(it) }.keys.toList(), "an exception message interpolates a value")
    }

    @Test
    fun theUiLayerNeverMakesWhatWasTypedPersistable() {
        val saved = Regex("""rememberSaveable|SavedStateHandle|savedStateHandle|\bSaver\b|listSaver|mapSaver|Parcelize|SharedPreferences|NSUserDefaults""")
        assertEquals(emptyList(), (screens + logic.filterKeys { it in logicFiles }).filterValues { saved.containsMatchIn(it) }.keys.toList(), "typed text was made persistable")
        val saver = withoutComments(File(ui, "GuideNavStateSaver.kt"))
        assertTrue(Regex("""isPending""").containsMatchIn(saver), "a new entry that was never saved must stay out of the saved stack")
    }

    @Test
    fun aLockDropsAnOpenEntryFormThroughTheGuideNavState() {
        val nav = withoutComments(File(ui, "GuideNavState.kt"))
        assertTrue(Regex("""fun discardTransientForms\(\)\s*\{[^}]*part2Bank\?\.discardForms\(\)""", RegexOption.DOT_MATCHES_ALL).containsMatchIn(nav), "a lock must drop the open Bank form")
    }

    @Test
    fun noBankCodeReachesTheNetworkTheClipboardOrAShareSheet() {
        val outward = Regex("""https?://|ktor|HttpClient|okhttp|java\.net|URL\(|openURL|UriHandler|WebView|setPrimaryClip|ClipData|ClipboardManager|UIPasteboard|ACTION_SEND|shareText|shareBytes|GuidePlatform""", RegexOption.IGNORE_CASE)
        assertEquals(emptyList(), (screens + logic).filterValues { outward.containsMatchIn(it) }.keys.toList())
    }

    @Test
    fun noBankCodeUsesARegexWhichIsSlowOnKotlinNative() {
        assertEquals(emptyList(), (screens + logic).filterValues { Regex("""Regex\(|\.toRegex\(|java\.util\.regex""").containsMatchIn(it) }.keys.toList())
    }

    @Test
    fun theScreensHoldNoUserFacingCopyAndNoColourBecauseEveryLabelIsOrdersBankStringsOrTheBundleAndEveryColourIsATheme() {
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
        assertEquals(emptyMap(), copy, "user-facing copy belongs in OrdersBankStrings (rule 4)")
        assertEquals(emptyList(), screens.filterValues { Regex("""Color\(|0x[0-9A-Fa-f]{6,8}|Color\.[A-Z]""").containsMatchIn(it) }.keys.toList(), "colours come from the theme (rule 4)")
    }
}
