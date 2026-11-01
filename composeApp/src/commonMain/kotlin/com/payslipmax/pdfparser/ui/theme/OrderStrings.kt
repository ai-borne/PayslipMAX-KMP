package com.payslipmax.pdfparser.ui.theme

import com.payslipmax.pdfparser.part2.domain.OrderCompleteness
import com.payslipmax.pdfparser.part2.domain.Part2Limits

/**
 * App chrome copy for scanned orders: attaching one to casualties, the order screen and the orders shelf (docs/Plan/13_OrdersBank_PhasePlan.md,
 * Phase 4, owner decisions row 15). No sentence here ever carries an order number, a date or a page; the screens add those beside the copy.
 */
object OrderStrings {
    // Attach flow
    const val attachTitle = "Attach an order"
    const val scanButton = "Scan the order"
    const val scanMoreButton = "Scan more pages"
    const val discardScan = "Discard the scan"
    const val orderNoLabel = "Order number"
    const val orderDateLabel = "Order date"
    const val expectedLabel = "No. of Occurrences printed on the order"
    const val lastOrderLabel = "Last Part II order no. it quotes"
    const val waitingHeading = "Tick the casualties this order covers"
    const val noWaiting = "No casualty is waiting for an order. Add one below."
    const val addCasualty = "Add a casualty"
    const val saveOrder = "Save order"
    const val savingOrder = "Saving"
    const val attachThisCasualty = "Attach an order to this casualty"
    const val viewOrder = "View the order"
    const val upgradeToAttach = "Attaching an order is part of Premium."

    fun pagesReady(count: Int): String = if (count == 1) "1 page scanned" else "$count pages scanned"

    // How a scan or a save ended (none names an order, number or date)
    const val scanUnavailable = "The scanner could not start. Check your connection once, then try again."
    const val scanFailed = "The scan did not work. Nothing was saved."

    fun orderFull(): String = "An order holds at most ${Part2Limits.PAGES_PER_ORDER_MAX} pages."

    const val vaultFull = "Your order pages are full. Delete an order you no longer need, then try again."
    const val orderGone = "This order is no longer here."
    const val saveFailed = "The order could not be saved. Nothing was changed."
    const val bankFull = "The Bank is full. Delete something you no longer need, then try again."

    fun pagesSaved(
        saved: Int,
        total: Int,
    ): String = "$saved of $total pages saved. The rest could not be stored."

    // What the form checks
    const val needPages = "Scan at least one page."
    const val needCasualty = "Tick at least one casualty."
    const val orderNoTooLong = "Too long"
    const val badCount = "Enter a whole number from 1 up"

    // The order screen
    const val orderTitle = "Order"
    const val referenceCopy = "Personal reference copy, not for PCDA(O) submission"
    const val pagesHeading = "Pages"
    const val casualtiesHeading = "Casualties on this order"
    const val addPages = "Add pages"
    const val showPage = "Show page"
    const val hidePage = "Hide page"
    const val deletePage = "Delete page"
    const val deleteOrder = "Delete order"
    const val deleteOrderBody = "The scan and its pages are deleted. Your casualties stay in the Bank and go back to Not attached."
    const val deletePageBody = "This page is deleted from the order."
    const val confirm = "Delete"
    const val cancel = "Cancel"
    const val pageUnreadable = "This page could not be read."
    const val noOrderNumber = "No order number"
    const val noOrderDate = "Date not entered"
    const val unlink = "Remove from this order"

    fun page(
        number: Int,
        total: Int,
    ): String = "Page $number of $total"

    fun pageNumber(number: Int): String = "Page $number"

    /** "3 of 5 casualties" when the order printed its count, "3 casualties" when it did not: never "3 of ?". */
    fun counter(completeness: OrderCompleteness): String {
        val noun = if (completeness.entered == 1 && completeness.expected == null) "casualty" else "casualties"
        return if (completeness.expected == null) "${completeness.entered} $noun" else "${completeness.entered} of ${completeness.expected} casualties"
    }

    fun chainMissing(quoted: String): String =
        "This order quotes last order no. $quoted, which is not in your orders yet. A page of the chain may be missing."

    // The shelf
    const val lensEntries = "Casualties"
    const val lensOrders = "Orders"
    const val shelfTitle = "Orders"
    const val shelfEmpty = "No order is attached yet. Scan one from a casualty that is waiting."
    const val undatedHeading = "Date not entered"
    const val checkChain = "Check chain"

    // Vault size (one text function for Settings and the Bank)
    fun sizeUsage(
        usedMb: Long,
        maxMb: Long,
    ): String = "Order pages use $usedMb MB of $maxMb MB"

    fun sizeWarning(
        usedMb: Long,
        maxMb: Long,
    ): String = "Order pages use $usedMb MB of $maxMb MB. Delete an order you no longer need before you scan more."

    // The backup screen (a backup carries the scanned pages)
    const val backupCloudWarning =
        "A backup includes your scanned order pages. It is encrypted with your password, but it is only as private as the place you save it. Keep the file where only you can open it."

    // iOS camera prompt (Info.plist NSCameraUsageDescription); OrdersBankCameraUsageTest keeps the two equal.
    const val cameraUsageDescription =
        "PayslipMax uses the camera only when you scan a Part II order into your Orders Bank. Pages are stored encrypted on this device and are never added to your Photos."
}
