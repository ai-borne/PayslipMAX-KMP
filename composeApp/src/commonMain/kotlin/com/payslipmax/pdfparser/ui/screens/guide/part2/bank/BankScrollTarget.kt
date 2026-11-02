package com.payslipmax.pdfparser.ui.screens.guide.part2.bank

import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.VaultSize

/**
 * The index of [entryId]'s row in the Bank's list, or null while the list does not hold it yet. It counts the items [BankScreen] adds, in
 * the order it adds them (the buttons, the lens, the optional notes, then the chips, and a heading per year before its rows), so it must
 * change with that function; `Part2AddLandsOnBankUiTest` fails when the two drift apart.
 */
internal fun bankRowIndex(
    state: BankUiState,
    canAdd: Boolean,
    entryId: String,
): Int? {
    if (!state.loaded || state.view.all == 0) return null
    var index = 1 // the Add button
    if (canAdd) index++ // Attach an order
    if (state.view.all > 0) index++ // Print or save as PDF
    index++ // the Casualties / Orders lens
    if (VaultSize.warning(state.vaultBytes) != null) index++
    if (!canAdd) index++ // the lapsed officer's note
    if (state.unreadable > 0) index++
    index++ // the chips
    if (state.view.years.isEmpty()) index++ // "nothing under this chip"
    state.view.years.forEach { year ->
        index++ // the year heading
        year.rows.forEach { row ->
            if (row.entry.id == entryId) return index
            index++
        }
    }
    return null
}
