package com.payslipmax.pdfparser.guide

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Security and resource evidence for scanned orders (plan 13, Phase 4), read from the sources because the property is "this code does not
 * exist": nothing about an order number, a date, a quoted order or a page reaches a log, telemetry, an exception message, the clipboard, a
 * share sheet or the network; what is typed is never persisted by the UI layer; the attach level is never saved with the Guide stack; a
 * lock closes the attach flow (which deletes the scanner's pages); and no screen holds a literal of copy or a colour. File lists are
 * explicit so a moved source fails loudly instead of making a scan vacuous.
 */
class OrdersAttachSecurityContractTest {
    private val orders = File("src/commonMain/kotlin/com/payslipmax/pdfparser/ui/screens/guide/part2/orders")
    private val ui = File("src/commonMain/kotlin/com/payslipmax/pdfparser/ui/screens/guide")

    private val screenFiles = listOf("AttachOrderScreen.kt", "OrderScreen.kt", "OrderPages.kt", "OrdersShelfScreen.kt", "OrdersLens.kt", "VaultSizeSettings.kt")
    private val logicFiles = listOf("AttachOrderViewModel.kt", "AttachState.kt", "OrderViewModel.kt", "OrderNotices.kt", "VaultSize.kt", "LocalPageScanner.kt")

    private fun withoutComments(file: File): String = file.readText().replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "").lines().joinToString("\n") { it.substringBefore("//") }

    private val screens by lazy { screenFiles.associateWith { withoutComments(File(orders, it)) } + ("GuideTabOrders.kt" to withoutComments(File(ui, "GuideTabOrders.kt"))) }
    private val logic by lazy { logicFiles.associateWith { withoutComments(File(orders, it)) } + ("PageStore.kt" to withoutComments(File("../shared/src/commonMain/kotlin/com/payslipmax/pdfparser/part2/pages/PageStore.kt"))) }

    @Test
    fun theScansReadTheRealFilesSoTheyAreNotVacuous() {
        assertTrue(screens.values.all { it.length > 200 } && logic.values.all { it.length > 100 })
        assertEquals((screenFiles + logicFiles).toSet(), orders.listFiles { f -> f.extension == "kt" }.orEmpty().map { it.name }.toSet(), "a source was added or moved; list it here so the scans cover it")
    }

    @Test
    fun noOrderCodeLogsPrintsReportsOrBuildsAnExceptionMessageFromAValue() {
        val leaky = Regex("""Logger|println|\bprint\(|\blog\(|Log\.[dievw]\(|Napier|Timber|Firebase|Analytics|CrashReporter|crashReporter|TelemetrySanitizer|setCustomKey|recordException|printStackTrace""")
        val templated = Regex("""(Exception|error|require|check|checkNotNull)\([^)]*\$""")
        val all = screens + logic
        assertEquals(emptyList(), all.filterValues { leaky.containsMatchIn(it) }.keys.toList(), "no log, print or report in order code")
        assertEquals(emptyList(), all.filterValues { templated.containsMatchIn(it) }.keys.toList(), "no exception message built from a value")
    }

    @Test
    fun theUiLayerNeverMakesWhatWasTypedOrScannedPersistable() {
        val saved = Regex("""rememberSaveable|SavedStateHandle|savedStateHandle|\bSaver\b|listSaver|mapSaver|Parcelize|SharedPreferences|NSUserDefaults""")
        assertEquals(emptyList(), (screens + logic).filterValues { saved.containsMatchIn(it) }.keys.toList())
        val saver = withoutComments(File(ui, "GuideNavStateSaver.kt"))
        assertTrue(Regex("""is Part2Destination\.Attach -> null""").containsMatchIn(saver), "the attach level holds a scan that cannot outlive the process, so it must stay out of the saved stack")
    }

    @Test
    fun aLockClosesTheAttachFlowWhichDeletesTheScannersPages() {
        val nav = withoutComments(File(ui, "GuideNavState.kt"))
        assertTrue(Regex("""fun discardTransientForms\(\)\s*\{[^}]*part2Bank\?\.discardForms\(\)""", RegexOption.DOT_MATCHES_ALL).containsMatchIn(nav))
        val bank = withoutComments(File(ui, "part2/bank/BankViewModel.kt"))
        assertTrue(Regex("""fun discardForms\(\)\s*\{[^}]*attach\.close\(\)""", RegexOption.DOT_MATCHES_ALL).containsMatchIn(bank), "discardForms must close the attach flow")
        val close = withoutComments(File(orders, "AttachOrderViewModel.kt"))
        assertTrue(Regex("""fun close\(\)\s*\{[^}]*discardScan\(\)""", RegexOption.DOT_MATCHES_ALL).containsMatchIn(close), "close must delete every held page")
    }

    @Test
    fun theBankDeletesLeftoverScansWheneverItIsTheLevelOnTop() {
        val tab = withoutComments(File(ui, "GuideTabBank.kt"))
        assertTrue(Regex("""attach\.close\(\)\s*scanner\.discardLeftovers\(\)""").containsMatchIn(tab), "opening the Bank must sweep the scanner's cache")
        val order = withoutComments(File(ui, "GuideTabOrders.kt"))
        assertTrue(Regex("""holdsScan\(\)\)\s*scanner\.discardLeftovers\(\)""").containsMatchIn(order), "a restored order level must sweep the scanner's cache unless a scan is in use")
    }

    @Test
    fun noOrderCodeReachesTheNetworkTheClipboardOrAShareSheet() {
        val outward = Regex("""https?://|ktor|HttpClient|okhttp|java\.net|URL\(|openURL|UriHandler|WebView|setPrimaryClip|ClipData|ClipboardManager|UIPasteboard|ACTION_SEND|shareText|shareBytes|GuidePlatform""", RegexOption.IGNORE_CASE)
        assertEquals(emptyList(), (screens + logic).filterValues { outward.containsMatchIn(it) }.keys.toList())
    }

    @Test
    fun noOrderCodeUsesARegexWhichIsSlowOnKotlinNative() {
        assertEquals(emptyList(), (screens + logic).filterValues { Regex("""Regex\(|\.toRegex\(|java\.util\.regex""").containsMatchIn(it) }.keys.toList())
    }

    @Test
    fun theScreensHoldNoUserFacingCopyAndNoColour() {
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
        assertEquals(emptyMap(), copy, "user-facing copy belongs in OrderStrings (rule 4)")
        assertEquals(emptyList(), screens.filterValues { Regex("""Color\(|0x[0-9A-Fa-f]{6,8}|Color\.[A-Z]""").containsMatchIn(it) }.keys.toList(), "colours come from the theme (rule 4)")
    }
}
