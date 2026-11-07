@file:OptIn(ExperimentalFoundationApi::class)

package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import com.payslipmax.pdfparser.crypto.Secret

/**
 * What the officer has typed into one password entry, kept in a [TextFieldState] and nowhere else (docs/Plan/14a P10). The state is made with
 * `remember`, never `rememberTextFieldState` (which saves its text with the screen), and its text is never read into a `String` here:
 * [take] copies it into a [Secret], one character at a time, only at the moment a call needs it.
 */
internal class TypedPassword(
    private val state: TextFieldState = TextFieldState(),
) {
    /** The state the field edits. */
    val field: TextFieldState get() = state

    val isEmpty: Boolean get() = state.text.isEmpty()

    /** A password of spaces is no password, like `String.isBlank`. */
    val isBlank: Boolean get() = state.text.all { it.isWhitespace() }

    /** A fresh copy for one call, which the callee owns and wipes; what is typed stays in the field so a retry needs no retyping. */
    fun take(): Secret = Secret.of(state.text)

    fun sameAs(other: TypedPassword): Boolean = passwordsMatch(state.text, other.state.text)

    /**
     * Empties the entry so nothing reachable from the state still holds the password. Deleting alone leaves the text in the undo history,
     * and `undo` brings it back; deleting and then dropping the history left no reference to it (the P10 spike, recorded in 14a; an
     * overwrite before the delete changed nothing it could see, so there is none). Copies Compose made of each keystroke (a `String` per
     * edit) are no longer referenced but cannot be zeroed.
     */
    fun wipe() {
        state.edit { delete(0, length) }
        state.undoState.clearHistory()
    }
}

/** Both entries of the Backup & Restore form, wiped together when the form leaves the screen. */
internal class BackupPasswordFields {
    val password = TypedPassword()
    val confirm = TypedPassword()

    fun wipe() {
        password.wipe()
        confirm.wipe()
    }
}
