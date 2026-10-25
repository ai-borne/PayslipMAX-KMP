package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertIsDisplayed
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: an order is tied to the draft it came from, and the officer can change that later (Round 21): link an order scanned from the vault to
 * a draft, or unlink one. Opening the draft works only while it exists; an officer whose Premium ended sees the link and cannot change it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Part2VaultLinkUiTest : Part2VaultUiBase() {
    private val draftTitle = Part2Strings.draftRowTitle(listOf(com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT))

    @Test
    fun anOrderWithNoDraftCanBeLinkedToOneOfTheDraftsThenOpenedAndUnlinked() {
        heldOrder("o1", draftId = null)
        savedDraft("draft-1")
        orderLevel()
        assertFalse(has(Part2VaultStrings.openDraft))
        assertFalse(has(Part2VaultStrings.unlinkDraft))

        tap(Part2VaultStrings.linkToDraft)
        assertTrue(has(" · edited "), "each draft is listed with its title and the day it was last edited")
        tapContaining(draftTitle)

        assertEquals("draft-1", runBlocking { repository.order("o1") }?.draftId)
        see(Part2VaultStrings.openDraft).assertIsDisplayed()
        see(Part2VaultStrings.unlinkDraft).assertIsDisplayed()
        assertFalse(has(Part2VaultStrings.linkToDraft), "linked, so the way to link is not offered")

        press(Part2VaultStrings.unlinkDraft)

        assertNull(runBlocking { repository.order("o1") }?.draftId)
        assertFalse(has(Part2VaultStrings.openDraft))
        see(Part2VaultStrings.linkToDraft).assertIsDisplayed()
    }

    @Test
    fun theDraftListCanBeClosedWithoutLinking() {
        heldOrder("o1", draftId = null)
        savedDraft("draft-1")
        orderLevel()

        tap(Part2VaultStrings.linkToDraft)
        assertTrue(has(draftTitle))
        press(Part2VaultStrings.closeChoices)

        assertFalse(has(draftTitle))
        assertNull(runBlocking { repository.order("o1") }?.draftId)
    }

    @Test
    fun withNoDraftOnTheDeviceThereIsNothingToLinkTo() {
        heldOrder("o1", draftId = null)
        orderLevel()
        assertFalse(has(Part2VaultStrings.linkToDraft))
    }

    @Test
    fun anOrderLinkedToADraftThatIsGoneCanBeUnlinkedButNotOpened() {
        heldOrder("o1", draftId = "draft-gone")
        orderLevel()
        assertFalse(has(Part2VaultStrings.openDraft))
        see(Part2VaultStrings.unlinkDraft).assertIsDisplayed()
    }

    @Test
    fun anOfficerWhosePremiumEndedSeesTheLinkButCannotChangeIt() {
        heldOrder("o1", draftId = "draft-1")
        savedDraft("draft-1")
        orderLevel(unlocked = false)
        see(Part2VaultStrings.openDraft).assertIsDisplayed()
        assertFalse(has(Part2VaultStrings.unlinkDraft))
    }

    @Test
    fun anOfficerWhosePremiumEndedCannotLinkAnUnlinkedOrder() {
        heldOrder("o1", draftId = null)
        savedDraft("draft-1")
        orderLevel(unlocked = false)
        assertFalse(has(Part2VaultStrings.linkToDraft))
    }

    @Test
    fun tappingTheLinkButtonAgainClosesTheList() {
        heldOrder("o1", draftId = null)
        savedDraft("draft-1")
        orderLevel()

        tap(Part2VaultStrings.linkToDraft)
        assertTrue(has(draftTitle))
        tap(Part2VaultStrings.linkToDraft)

        assertFalse(has(draftTitle))
    }
}
