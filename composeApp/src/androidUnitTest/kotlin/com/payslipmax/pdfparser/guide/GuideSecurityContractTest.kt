package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.guide.domain.GuideSuggestion
import com.payslipmax.pdfparser.guide.domain.GuideSuggestionLabels
import com.payslipmax.pdfparser.guide.domain.GuideSuggestionParts
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
 * state on the device, and talks to the outside only when the user taps Copy cite, Share or, since M5, Suggest a correction
 * (the existing clipboard, share-sheet and email helpers; the email one only opens the user's mail app, the app sends nothing). These checks fail if someone adds a network path, writes Guide data to telemetry, or ships the
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
        // The claim note is built from GuideShareParts (title, answer, key points, attach, watch-out, cite, unverified and, since the
        // owner's 2026-10-09 decision, the replacement DATE), so a change line, an Updated flag or a history link cannot be in it;
        // and the saver writes level names and ids only.
        val parts = guideSources.first { it.name == "GuideShareText.kt" }.readText()
        assertFalse(Regex("""changes|updated|history|until""", RegexOption.IGNORE_CASE).containsMatchIn(parts.substringAfter("data class GuideShareParts").substringBefore(")")))
        val saver = guideSources.first { it.name == "GuideNavStateSaver.kt" }.readText()
        assertFalse(Regex("""\.text|\.title|changes\.|GuideChange""").containsMatchIn(saver), "the saver must hold ids and level names, never change text")
        val maintenance = guideSources.filter { it.name in setOf("GuideMaintenanceModels.kt", "GuideWhatsNew.kt", "GuideCardHistoryLinks.kt", "GuideChangeLog.kt", "GuideRuleHistory.kt", "GuideMaintenanceStrings.kt") }
        assertEquals(6, maintenance.size, "a maintenance source moved; update this list so the scan stays honest")
        assertTrue(maintenance.none { Regex("""CrashReporter|crashReporter|log\(|println|setPrimaryClip|ACTION_SEND""").containsMatchIn(it.readText()) })
    }

    private fun withoutComments(file: File): String = file.readText().replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "").lines().joinToString("\n") { it.substringBefore("//") }

    @Test
    fun personalNotesStayOnTheDeviceAndInTheBackupOnlyAndNothingAboutThemCanBeLoggedSharedOrReported() {
        // M6: notes are private text. The data layer must not log, report, share or print them, must not put the text in an
        // exception message, and the share and suggestion models must have no field that could carry one.
        val noteFiles = setOf("GuideNote.kt", "GuideNoteEdit.kt", "GuideNotePlacements.kt", "GuideNotesRepository.kt", "RoomGuideNotesRepository.kt", "GuideNoteEntity.kt", "GuideNoteBackup.kt")
        val found = guideSources.filter { it.name in noteFiles }
        assertEquals(noteFiles, found.map { it.name }.toSet(), "a notes source moved; update this list so the scan stays honest")
        val leaky = Regex("""CrashReporter|crashReporter|TelemetrySanitizer|Logger|println|\bprint\(|\blog\(|Log\.[dievw]\(|shareText|setPrimaryClip|ClipData|ACTION_SEND|GuidePlatform|Firebase""")
        assertEquals(emptyList(), found.filter { leaky.containsMatchIn(withoutComments(it)) }.map { it.name }, "a notes file logs, reports, shares or prints")
        // An exception message built from a template could quote the note (serialization errors quote their input already).
        val templated = Regex("""(Exception|error|require|check)\([^)]*\$""")
        assertEquals(emptyList(), found.filter { templated.containsMatchIn(withoutComments(it)) }.map { it.name }, "an exception message interpolates a value")
        val shareParts = withoutComments(guideSources.first { it.name == "GuideShareText.kt" }).substringAfter("data class GuideShareParts").substringBefore("\n)")
        assertFalse(Regex("""note""", RegexOption.IGNORE_CASE).containsMatchIn(shareParts), "the share note must have no personal-note field")
        val entity = withoutComments(guideSources.first { it.name == "GuideNoteEntity.kt" })
        val columns = entity.substringAfter("data class GuideNoteEntity(").substringBefore("\n)").lines().map { it.trim().removeSuffix(",") }.filter { it.isNotEmpty() }
        assertEquals(listOf("@PrimaryKey val cardId: String", "val ciphertext: String"), columns, "the stored row is the card id (key) and one ciphertext, nothing readable")
        assertTrue(entity.contains("CryptoHelper.encrypt"))
    }

    @Test
    fun onlyGuidePlatformNamesTheEmailHelperAndNoGuideFileBuildsALinkOrHoldsAnAddress() {
        // M5 deliberately widens the earlier "share sheet and clipboard only" rule by exactly one seam: the user's mail app, opened
        // from a button tap through the existing shareTextViaEmail. The Guide still builds no intent and no mailto link itself.
        val naming = guideSources.filter { withoutComments(it).contains("shareTextViaEmail") }
        assertEquals(listOf("GuidePlatform.kt"), naming.map { it.name }, "only the platform seam may name the email helper")
        val mailto = guideSources.filter { Regex("mailto|ACTION_SENDTO|EXTRA_EMAIL", RegexOption.IGNORE_CASE).containsMatchIn(withoutComments(it)) }
        assertEquals(emptyList(), mailto.map { it.name }, "a Guide file builds its own mail link or intent")
        val addresses = guideSources.filter { Regex("""[\w.+-]+@[\w-]+\.[a-z]{2,}""", RegexOption.IGNORE_CASE).containsMatchIn(withoutComments(it)) }
        assertEquals(emptyList(), addresses.map { it.name }, "the recipient is AppStringsSupport.supportEmail, never a literal in the Guide")
        val recipientUsers = guideSources.filter { withoutComments(it).contains("supportEmail") }
        assertEquals(listOf("GuideSuggestionBuilder.kt"), recipientUsers.map { it.name })
    }

    @Test
    fun theSuggestionEmailIsBuiltFromSixFixedFieldsAndNothingFromTheCardsPaidHalfOrThePersonalState() {
        val domain = withoutComments(guideSources.first { it.name == "GuideSuggestion.kt" })
        val fields = domain.substringAfter("data class GuideSuggestionParts(").substringBefore("\n)").lines().map { it.trim().removeSuffix(",") }.filter { it.isNotEmpty() }
        assertEquals(
            listOf("val cardId: String", "val cardTitle: String", "val bundleGenerated: String", "val cardRev: String", "val appVersion: String", "val text: String"),
            fields,
            "a new field in the suggestion needs a deliberate decision, not a quiet addition",
        )
        val sources = listOf("GuideSuggestion.kt", "GuideSuggestionBuilder.kt").map { name -> withoutComments(guideSources.first { it.name == name }) }
        val banned = Regex("""figure|profile|pins|changeLog|history|\.cite|\.key\b|\.details|\.attach|\.watch|\.answer|\.full|crashReporter|CrashReporter""", RegexOption.IGNORE_CASE)
        assertTrue(sources.none { banned.containsMatchIn(it) }, "the suggestion must not reach a figure, profile, pin, change line or the card's body")
    }

    @Test
    fun noRealCardsSuggestionEmailEverHoldsItsBodyItsCiteItsDetailsOrItsFigureText() =
        runTest {
            val bundle = (GuideBundleParser.parse(GuideBundleContract.readShippedBundleText()) as com.payslipmax.pdfparser.guide.GuideLoadResult.Loaded).bundle
            val labels = GuideSuggestionLabels("[Guide]", "Guide correction suggestion", "Card:", "Title:", "Guide data:", "Card revision:", "App version:", "Suggestion:")

            val leaks =
                bundle.cards.filter { card ->
                    val body = GuideSuggestion.body(GuideSuggestionParts(card.id, card.title, bundle.generated, card.rev, "1.3.0", "x"), labels)
                    val paid = card.key + card.attach + card.watch + listOf(card.cite, card.details).filter { it.isNotBlank() } + listOf(card.answer.takeIf { it != card.title }).filterNotNull()
                    paid.any { body.contains(it) }
                }
            assertEquals(emptyList(), leaks.map { it.id })
            assertTrue(bundle.cards.size > 300, "the scan covered the real bundle")
        }

    @Test
    fun theNotesScreensAndModelsNeverLogReportShareSaveOrPrintNoteTextAndTheEntryPointsPassTheNotesModel() {
        // M7: the UI and view-model files for personal notes. Note text must not reach a log, telemetry, the clipboard or share
        // sheet, a saved-state key (the draft is plain `remember`, so process death discards it) or an exception message.
        val noteFiles =
            setOf("GuideNotesViewModel.kt", "GuideNotesState.kt", "GuideNoteSection.kt", "GuideNoteEditor.kt", "GuideRemovedNotes.kt", "GuideSearchNotes.kt")
        val found = guideSources.filter { it.name in noteFiles }
        assertEquals(noteFiles, found.map { it.name }.toSet(), "a notes source moved; update this list so the scan stays honest")
        val leaky = Regex("""CrashReporter|crashReporter|TelemetrySanitizer|Logger|println|\bprint\(|\blog\(|Log\.[dievw]\(|shareText|setPrimaryClip|ClipData|ACTION_SEND|GuidePlatform|Firebase""")
        assertEquals(emptyList(), found.filter { leaky.containsMatchIn(withoutComments(it)) }.map { it.name }, "a notes file logs, reports, shares or prints")
        val saved = Regex("""rememberSaveable|SavedStateHandle|savedStateHandle|\bSaver\b|listSaver|mapSaver|autoSaver""")
        assertEquals(emptyList(), found.filter { saved.containsMatchIn(withoutComments(it)) }.map { it.name }, "a notes file saves state across process death")
        val templated = Regex("""(Exception|error|require|check)\([^)]*\$""")
        assertEquals(emptyList(), found.filter { templated.containsMatchIn(withoutComments(it)) }.map { it.name }, "an exception message interpolates a value")

        // Nothing that is shared, copied, mailed or saved with the stack may know a note type.
        val noteTypes = Regex("""GuideNotesViewModel|GuideNotesState|GuideCardNotes|GuideNoteItem|GuideRemovedNote|GuideSearchNotes|GuideNotesRepository|GuideNote\b|notesViewModel""")
        val outward = setOf("GuideCardShare.kt", "GuideShareText.kt", "GuideSuggestion.kt", "GuideSuggestionBuilder.kt", "GuidePlatform.kt", "GuideCardActions.kt", "GuideNavStateSaver.kt")
        val outwardFiles = guideSources.filter { it.name in outward }
        assertEquals(outward, outwardFiles.map { it.name }.toSet(), "a share, suggestion or saver source moved; update this list")
        assertEquals(emptyList(), outwardFiles.filter { noteTypes.containsMatchIn(withoutComments(it)) }.map { it.name }, "note types reached a share, suggestion or saved-state file")

        // The search model is built with the notes model (optional in its constructor so older screen tests compile); a binding
        // that dropped the argument would make search silently ignore notes.
        assertTrue(
            withoutComments(guideSources.first { it.name == "GuideModule.kt" }).contains("GuideSearchViewModel(guide = get(), notes = get())"),
            "GuideModule must build the search model with the notes model",
        )
        // The two production entry points hand the Koin notes model down; a third that forgot would silently have no notes.
        for (entry in listOf("GuideTabRoute.kt", "GuideCardRoute.kt")) {
            assertTrue(withoutComments(guideSources.first { it.name == entry }).contains("notesViewModel = koinInject()"), "$entry must pass the notes model")
        }
    }
}
