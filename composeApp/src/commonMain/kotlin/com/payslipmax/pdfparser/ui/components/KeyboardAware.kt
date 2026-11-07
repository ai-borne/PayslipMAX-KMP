package com.payslipmax.pdfparser.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity

/*
 * The one place that keeps a typed field above the keyboard, for every Part II form and search (Phase 8 item 3). On a phone the keyboard
 * overlays the window, so nothing made room for it and it covered the field being typed in. Two parts, both needed (Pixel walk, 2026-11-02):
 * the list gives up the keyboard's height at its foot ([aboveKeyboard]), and a field that takes focus scrolls into the space that is left
 * ([scrollToWhenFocused]). The second is what the keyboard's Next key needs: it moves focus with the keyboard already up, and Compose does
 * not scroll the new field into view by itself.
 */

/**
 * On a scrolling list: leaves room for the keyboard so the last field can be scrolled clear of it. The app's `Scaffold` already consumed
 * the bottom bar's inset, so only what the keyboard adds beyond the bar is taken. Zero while the keyboard is away.
 */
fun Modifier.aboveKeyboard(): Modifier = imePadding()

/**
 * On a text field inside a scrolling list: when it takes focus, and again while the keyboard's height changes (it grows over a few frames),
 * it is scrolled into view. A field that is not focused is left alone.
 */
@Composable
fun Modifier.scrollToWhenFocused(): Modifier {
    val requester = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    val keyboard = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(focused, keyboard) { if (focused) requester.bringIntoView() }
    return this.bringIntoViewRequester(requester).onFocusChanged { focused = it.isFocused }
}
