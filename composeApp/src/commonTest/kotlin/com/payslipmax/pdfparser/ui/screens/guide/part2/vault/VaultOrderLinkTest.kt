package com.payslipmax.pdfparser.ui.screens.guide.part2.vault

import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: an order is tied to the draft it came from, and the officer can change that (Round 21). A link is saved at once, only to a draft the
 * device holds, and an order whose draft is gone stays linked so it can be unlinked.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class VaultOrderLinkTest : VaultOrderTestBase() {
    @Test
    fun anOrderIsLinkedToADraftAndTheLinkIsSavedAndTheDraftCanBeOpened() =
        check { d ->
            stored(draftId = null)
            repo.saveDraft(SyntheticPart2Drafts.emptyDraft("draft-9"))
            val vault = model(d)
            assertFalse(vault.state.value.linked)

            vault.setDraft("draft-9")
            advanceUntilIdle()

            assertTrue(vault.state.value.linked)
            assertEquals("draft-9", vault.state.value.draftId)
            assertEquals("draft-9", repo.order("order-1")?.draftId)
            assertEquals(9_000L, repo.order("order-1")?.updatedAt)
            vault.close()
        }

    @Test
    fun unlinkingClearsTheLinkAndTheDraftIsNoLongerOffered() =
        check { d ->
            stored(draftId = "draft-1")
            repo.saveDraft(SyntheticPart2Drafts.emptyDraft("draft-1"))
            val vault = model(d)
            assertTrue(vault.state.value.linked)

            vault.setDraft(null)
            advanceUntilIdle()

            assertFalse(vault.state.value.linked)
            assertNull(vault.state.value.draftId)
            assertNull(repo.order("order-1")?.draftId)
            vault.close()
        }

    @Test
    fun anOrderLinkedToADraftThatIsGoneIsStillLinkedSoItCanBeUnlinked() =
        check { d ->
            stored(draftId = "draft-gone")
            val vault = model(d)
            assertTrue(vault.state.value.linked)
            assertNull(vault.state.value.draftId, "there is nothing to open")
        }

    @Test
    fun aDraftTheDeviceDoesNotHoldIsNeverLinked() =
        check { d ->
            stored(draftId = null)
            val vault = model(d)
            vault.setDraft("no-such-draft")
            advanceUntilIdle()
            assertFalse(vault.state.value.linked)
            assertNull(repo.order("order-1")?.draftId)
        }

    @Test
    fun aLinkChangedAfterTheScreenWasLeftIsIgnored() =
        check { d ->
            stored(draftId = null)
            repo.saveDraft(SyntheticPart2Drafts.emptyDraft("draft-9"))
            val vault = model(d)
            vault.close()
            vault.setDraft("draft-9")
            advanceUntilIdle()
            assertNull(repo.order("order-1")?.draftId)
        }
}
