package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.BankActions
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.BankScreen
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.BankViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.EntryActions
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.EntryEditViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.EntryScreen
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.SaveOutcome
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings

/** Who may add to the Orders Bank (Premium, no free allowance) and how to offer the upgrade. A lapsed officer can still view, delete and back up. */
class BankAccess(
    val canAdd: Boolean,
    val onUnlock: () -> Unit,
) {
    companion object {
        val Locked = BankAccess(canAdd = false, onUnlock = {})
    }
}

/** The Bank list. Add opens Learn's search above it, so the casualty is chosen the way the officer learns them; Back returns here. */
@Composable
internal fun GuideBankLevel(
    navState: GuideNavState,
    host: Part2Host,
    bank: BankViewModel,
    listState: LazyListState,
) {
    val state by bank.state.collectAsState()
    val actions =
        BankActions(
            canAdd = host.access.canAdd,
            onAdd = { host.open(navState) },
            onUnlock = host.access.onUnlock,
            onOpenEntry = { navState.push(Part2Destination.Entry(it)) },
            onFilter = bank::setFilter,
            onBack = { navState.pop() },
        )
    BankScreen(state, bank::nameOf, actions, listState)
}

/** What opening an entry's form found. */
private sealed interface EditorLoad {
    data object Loading : EditorLoad

    data object Missing : EditorLoad

    class Ready(val editor: EntryEditViewModel) : EditorLoad
}

/** One entry's form. An entry the store no longer has closes its level instead of showing a blank form. */
@Composable
internal fun GuideEntryLevel(
    navState: GuideNavState,
    bank: BankViewModel,
    entryId: String,
    access: BankAccess,
) {
    val load by produceState<EditorLoad>(EditorLoad.Loading, entryId) { value = bank.openEditor(entryId)?.let { EditorLoad.Ready(it) } ?: EditorLoad.Missing }
    when (val current = load) {
        EditorLoad.Loading -> GuideLoading()
        EditorLoad.Missing -> {
            LaunchedEffect(entryId) { navState.pop() }
            GuideLoading()
        }
        is EditorLoad.Ready -> EntryLevelContent(navState, bank, current.editor, access)
    }
}

@Composable
private fun EntryLevelContent(
    navState: GuideNavState,
    bank: BankViewModel,
    editor: EntryEditViewModel,
    access: BankAccess,
) {
    val state by editor.state.collectAsState()
    var message by remember { mutableStateOf<String?>(null) }
    val actions =
        EntryActions(
            canEdit = access.canAdd,
            isNew = editor.isNew,
            name = bank.nameOf(editor.form.code),
            message = message,
            onSave = {
                editor.save { outcome ->
                    message = saveMessage(outcome)
                    if (outcome == SaveOutcome.SAVED) {
                        bank.markSaved(editor.id)
                        navState.pop()
                    }
                }
            },
            onDelete = { editor.delete { bank.markSaved(editor.id).also { navState.pop() } } },
            onBack = { navState.pop() },
        )
    EntryScreen(editor, state, actions)
}

private fun saveMessage(outcome: SaveOutcome): String? =
    when (outcome) {
        SaveOutcome.FULL -> OrdersBankStrings.bankFull
        SaveOutcome.FAILED -> OrdersBankStrings.saveFailed
        else -> null
    }
