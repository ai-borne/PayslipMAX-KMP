package com.payslipmax.pdfparser.guide

import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * E9 security evidence that can be checked mechanically. The Guide is offline: it reads one bundled file, keeps its own
 * state on the device, and talks to the outside only when the user taps Copy cite or Share (the existing clipboard and
 * share-sheet helpers). These checks fail if someone adds a network path, writes Guide data to telemetry, or ships the
 * bundle's internal `from`/`open` fields. They read the sources as text because the property is "this code does not exist".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GuideSecurityContractTest {
    private val sourceRoots = listOf(File("src"), File("../shared/src"))

    private fun isGuideProductionSource(file: File): Boolean {
        val path = file.invariantSeparatorsPath
        val production = path.contains("/commonMain/") || path.contains("/androidMain/") || path.contains("/iosMain/")
        return file.extension == "kt" && production && (path.contains("/guide/") || file.name.startsWith("Guide"))
    }

    private val guideSources: List<File> by lazy {
        sourceRoots.flatMap { root -> root.walkTopDown().filter { it.isFile && isGuideProductionSource(it) }.toList() }
    }

    @Test
    fun theGuideSourcesWereFoundSoTheScansBelowAreNotVacuous() {
        assertTrue(guideSources.size >= 40, "found only ${guideSources.size} Guide source files; the scan roots are wrong")
    }

    @Test
    fun noGuideCodeCanReachTheNetworkOrOpenALink() {
        val banned = Regex("""https?://|ktor|HttpClient|okhttp|java\.net|URL\(|Uri\.parse|openURL|UriHandler|WebView|Firebase|Analytics""", RegexOption.IGNORE_CASE)
        val hits = guideSources.flatMap { f -> f.readLines().mapIndexedNotNull { i, line -> if (banned.containsMatchIn(line)) "${f.name}:${i + 1}" else null } }
        assertEquals(emptyList(), hits, "a Guide file gained a network, link or analytics reference")
    }

    @Test
    fun onlyTheViewModelTouchesTelemetryAndItWritesOneCodeNeverCardData() {
        val users = guideSources.filter { Regex("""CrashReporter|crashReporter|TelemetrySanitizer""").containsMatchIn(it.readText().replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")) }
        // GuideModule hands the reporter to the view model; the search view model only names it in a comment.
        assertEquals(setOf("GuideViewModel.kt", "GuideModule.kt"), users.map { it.name }.toSet())
        val model = guideSources.first { it.name == "GuideViewModel.kt" }.readText()
        assertEquals(1, Regex("""crashReporter\.""").findAll(model).count(), "the view model may report exactly one thing")
        assertTrue(model.contains("mapOf(GUIDE_LOAD_ERROR_KEY to error.name)"), "the only value sent is the load error code")
    }

    @Test
    fun theOnlyThingsTheGuideCanHandOutAreTheCiteAndTheClaimNote() {
        val platform = guideSources.first { it.name == "GuidePlatform.kt" }.readText()
        assertFalse(platform.contains("Intent"), "the Guide hands text to the existing share helper, it builds no intents")
        val outward = guideSources.filter { Regex("""ACTION_SEND|setPrimaryClip|UIPasteboard|createChooser|ClipData""").containsMatchIn(it.readText()) }
        assertEquals(emptyList(), outward.map { it.name })
    }

    @Test
    fun theShippedBundleCarriesNoProvenanceOrOpenPointsAndNoLinks() =
        runTest {
            val text = GuideBundleContract.readShippedBundleText()

            assertFalse(text.contains("\"from\""), "provenance ids must never ship")
            assertFalse(text.contains("\"open\""), "open-point text must never ship")
            assertFalse(text.contains("http", ignoreCase = true), "the bundle holds no links")
        }

    @Test
    fun ruleHistoryAndChangeTextNeverReachTheShareNoteTheClipboardOrTheSavedStack() {
        // The claim note is built from GuideShareParts (title, answer, key points, attach, watch-out, cite, unverified), so a
        // change line, a replacement date or an Updated flag cannot be in it; and the saver writes level names and ids only.
        val parts = guideSources.first { it.name == "GuideShareText.kt" }.readText()
        assertFalse(Regex("""changes|replaced|until|updated|history""", RegexOption.IGNORE_CASE).containsMatchIn(parts.substringAfter("data class GuideShareParts").substringBefore(")")))
        val saver = guideSources.first { it.name == "GuideNavStateSaver.kt" }.readText()
        assertFalse(Regex("""\.text|\.title|changes\.|GuideChange""").containsMatchIn(saver), "the saver must hold ids and level names, never change text")
        val maintenance = guideSources.filter { it.name in setOf("GuideMaintenanceModels.kt", "GuideWhatsNew.kt", "GuideCardHistoryLinks.kt", "GuideChangeLog.kt", "GuideRuleHistory.kt", "GuideMaintenanceStrings.kt") }
        assertEquals(6, maintenance.size, "a maintenance source moved; update this list so the scan stays honest")
        assertTrue(maintenance.none { Regex("""CrashReporter|crashReporter|log\(|println|setPrimaryClip|ACTION_SEND""").containsMatchIn(it.readText()) })
    }
}
