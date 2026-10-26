package com.payslipmax.pdfparser

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.payslipmax.pdfparser.nav.PendingPart2Link
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.testing.FakePart2Repository
import com.payslipmax.pdfparser.ui.screens.guide.GuideDestination
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * WHY: the opener is composed only inside the unlocked app, so a tap that arrives while locked waits in [PendingPart2Link] and opens the
 * moment the app is unlocked; a tap that arrives while the app is open opens at once. Screens are the only place this timing can break.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Part2LinkOpenerUiTest {
    @get:Rule
    val composeRule: AndroidComposeTestRule<*, ComponentActivity> = createAndroidComposeRule<ComponentActivity>()

    private val repository = FakePart2Repository()
    private val pending = PendingPart2Link(repository)
    private val guide = GuideNavState(listOf(GuideDestination.Search))
    private val shown = mutableListOf<Screen>()

    private fun saveDraft(id: String) = runBlocking { repository.saveDraft(Part2Draft(id, LeaveEventInput(emptyList()), emptyMap(), 1, 1, null)) }

    @Test
    fun aTapThatWaitedWhileLockedOpensWhenTheUnlockedAppComposesTheOpener() {
        saveDraft("d1")
        pending.offer("RETURN", "d1")
        composeRule.waitForIdle()
        assertNotNull(pending.link.value, "nothing composed yet: the tap waits")

        composeRule.setContent { Part2LinkOpener(pending, guide) { shown += it } }
        composeRule.waitForIdle()

        assertEquals(Part2Destination.Due, guide.current)
        assertEquals(listOf(Screen.Guide), shown)
        assertNull(pending.link.value)
    }

    @Test
    fun aTapThatArrivesWhileTheAppIsOpenOpensAtOnce() {
        saveDraft("d1")
        composeRule.setContent { Part2LinkOpener(pending, guide) { shown += it } }
        composeRule.waitForIdle()
        assertEquals(emptyList(), shown)

        pending.offer("YEAR_END", "d1")
        composeRule.waitForIdle()

        assertEquals(Part2Destination.Due, guide.current)
        assertEquals(listOf(Screen.Guide), shown)
    }

    @Test
    fun aTapForADraftThatIsGoneChangesNothingAndDoesNotWait() {
        composeRule.setContent { Part2LinkOpener(pending, guide) { shown += it } }

        pending.offer("RETURN", "gone")
        composeRule.waitForIdle()

        assertEquals(emptyList(), shown)
        assertEquals(GuideDestination.Search, guide.current)
        assertNull(pending.link.value)
    }

    @Test
    fun aTapStillOpensWhenTheDatabaseLookupReallySuspends() {
        // Found on the Pixel 9 (2026-10-26): take() clears the link before its lookup suspends, which changed the effect's key and cancelled
        // it mid-lookup, so the tap never opened. The fake repository answers at once, so only a repository that yields shows it.
        val slow =
            object : com.payslipmax.pdfparser.part2.data.Part2Repository by repository {
                override suspend fun draft(id: String): Part2Draft? {
                    kotlinx.coroutines.delay(50)
                    return repository.draft(id)
                }
            }
        val slowPending = PendingPart2Link(slow)
        saveDraft("d1")
        composeRule.setContent { Part2LinkOpener(slowPending, guide) { shown += it } }
        composeRule.waitForIdle()

        slowPending.offer("RETURN", "d1")
        composeRule.mainClock.autoAdvance = true
        composeRule.waitUntil(timeoutMillis = 5_000) { guide.current == Part2Destination.Due }

        assertEquals(listOf(Screen.Guide), shown)
    }
}
