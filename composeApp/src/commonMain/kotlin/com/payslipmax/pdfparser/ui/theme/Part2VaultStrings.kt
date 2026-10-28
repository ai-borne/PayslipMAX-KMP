package com.payslipmax.pdfparser.ui.theme

import com.payslipmax.pdfparser.part2.domain.Part2Limits

/**
 * Copy for the Part II vault: the officer's scanned, published orders (docs/Plan/12_Part2Orders_PhasePlan.md, Phase 6b). Kept apart from
 * [Part2Strings] so neither file nears the 300-line limit; the camera usage text stays there, where `Part2CameraUsageTest` pins it.
 * Nothing here is built from an order's number or date except through the functions that take them.
 */
object Part2VaultStrings {
    // The Part II home section and the slip preview.
    const val vaultHeading = "Your published orders"
    const val scanOrder = "Scan a published order"
    const val addPublishedOrder = "Add the published order"
    const val linkedOrdersHeading = "Published order"
    const val vaultUnreadable = "Some orders could not be read on this device."
    const val numberNotTyped = "Number not typed yet"

    fun orderTitle(
        no: String,
        date: String,
    ): String = "No. $no dated $date"

    fun orderNumberOnly(no: String): String = "No. $no"

    fun pageCount(count: Int): String = if (count == 1) "1 page" else "$count pages"

    fun orderRowLine(
        pages: Int,
        added: String,
    ): String = "${pageCount(pages)} · added $added"

    // One order.
    const val orderScreenTitle = "Published order"
    const val orderScreenSubtitle = "Kept encrypted on this device"
    const val pagesHeading = "Pages"
    const val addPages = "Add pages"
    const val hidePage = "Hide page"
    const val openDraft = "Open the draft"
    const val deleteOrder = "Delete order"
    const val linkToDraft = "Link to a draft"
    const val unlinkDraft = "Unlink from the draft"

    fun linkChoice(
        title: String,
        line: String,
    ): String = "$title · $line"

    const val deletePage = "Delete page"
    const val deletePageTitle = "Delete this page?"
    const val deletePageBody = "The page is removed from this device. The order keeps its other pages."
    const val deleteLastPageBody = "This is the order's only page, so the order is deleted with it."
    const val deleteOrderTitle = "Delete this order?"
    const val deleteOrderBody = "The order and all its pages are removed from this device. A draft it came from is not touched."
    const val orderViewOnlyNote = "Your Premium has ended. You can still read and delete this order, but not change it or add pages."
    const val pageUnreadable = "This page could not be read."
    const val saveFailed = "This order could not be saved on this device."

    fun pageLabel(number: Int): String = "Page $number"

    fun viewPage(number: Int): String = "View page $number"

    // What a scan said (plan Phase 11 item 23): each outcome of a capture, in words the officer can act on.
    const val scanNeedsConnection = "The scanner needs a connection once. Connect to the internet, then scan again. It works offline after that."
    const val scanFailed = "The scan did not work. Try again."
    const val vaultFull = "The vault is full. Delete an order you no longer need, then scan again."
    const val orderGone = "This order is no longer on this device."
    const val noticeDismiss = "OK"

    fun orderFull(): String = "This order already holds ${Part2Limits.PAGES_PER_ORDER_MAX} pages. Add the next pages as a new order."

    fun pagesSaved(count: Int): String = "${pageCount(count)} saved."

    fun orderHoldsMax(): String = "This order holds ${Part2Limits.PAGES_PER_ORDER_MAX} pages, so the rest were not saved."

    fun pagesNotSaved(count: Int): String = if (count == 1) "1 page could not be saved." else "$count pages could not be saved."

    // The grant picker on a follow-up form (EXTNAL, REJOIN, VOLREJ, RECALL).
    const val useVaultOrder = "Use an order from my vault"
    const val closeChoices = "Close"

    fun grantChoice(
        no: String,
        date: String,
    ): String = "$no dated $date"

    // Settings and restore.
    fun sizeWarning(
        usedMb: Long,
        maxMb: Long,
    ): String = "Your vault of published orders is using $usedMb MB of the $maxMb MB it can hold. Delete orders you no longer need in Guide, Part II Orders."

    // Backup and restore when the device holds Part II items (Phase 11 item 56).
    const val restoreExistingTitle = "Part II items already on this device"

    fun backedUpItems(count: Int): String = if (count == 1) "1 Part II item backed up" else "$count Part II items backed up"

    fun restoreSkipped(count: Int): String = if (count == 1) "1 item could not be restored." else "$count items could not be restored."
}
