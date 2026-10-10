package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.guide.domain.GuideSearchIndex
import com.payslipmax.pdfparser.guide.domain.GuideSearchNotes
import com.payslipmax.pdfparser.guide.domain.GuideSearchScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/**
 * M7: search also reads the user's notes. The JVM proves it correct, not that Kotlin/Native does it fast (see
 * `GuideLoaderIosPerfTest`). This is the worst case a user can build: a full-length note on every card of the real bundle,
 * turned into search words once (as happens when the notes change) and then searched repeatedly (as happens per keystroke).
 */
class GuideNotesSearchIosPerfTest {
    @Test
    fun aFullLengthNoteOnEveryCardIsIndexedAndSearchedWithinBudgetOnNative() =
        runTest {
            val bundle = GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText())
            val index = GuideSearchIndex(bundle)
            val sentence = "Remember to attach the countersigned order and ask the unit pay cell about the zebra form. "
            val texts = bundle.cards.associate { it.id to sentence.repeat(NOTE_CHARS / sentence.length) }

            val build = TimeSource.Monotonic.markNow()
            val notes = GuideSearchNotes.of(texts)
            val buildMs = build.elapsedNow().inWholeMilliseconds

            val searches = TimeSource.Monotonic.markNow()
            var found = 0
            repeat(SEARCHES) { found = index.search("zebra form", GuideSearchScope.FULL, notes).size }
            val searchMs = searches.elapsedNow().inWholeMilliseconds

            println("guide notes on Native: ${bundle.cards.size} notes of $NOTE_CHARS chars: words ${buildMs}ms, $SEARCHES searches ${searchMs}ms")
            assertEquals(bundle.cards.count { !it.isReplaced }, found, "every searchable card matches through its note")
            assertTrue(buildMs < BUILD_BUDGET_MS, "indexing the notes took ${buildMs}ms, budget ${BUILD_BUDGET_MS}ms")
            assertTrue(searchMs < SEARCH_BUDGET_MS, "$SEARCHES searches took ${searchMs}ms, budget ${SEARCH_BUDGET_MS}ms")
        }

    private companion object {
        const val NOTE_CHARS = 2000
        const val SEARCHES = 20
        const val BUILD_BUDGET_MS = 3000L
        const val SEARCH_BUDGET_MS = 2000L
    }
}
