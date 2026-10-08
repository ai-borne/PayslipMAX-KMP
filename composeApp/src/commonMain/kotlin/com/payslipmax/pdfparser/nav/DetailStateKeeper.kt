package com.payslipmax.pdfparser.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.payslipmax.pdfparser.Screen

/**
 * Keeps a detail's `rememberSaveable` state (Pay Audit's month and tab) while another detail is pushed on top of it, so Back
 * from a Guide card lands on the same finding (E7). Android draws only the top detail, which would otherwise drop the one
 * beneath. State is cleared as soon as its detail leaves the stack, so a fresh entry always starts empty.
 */
@Stable
class DetailStateKeeper internal constructor(
    private val holder: SaveableStateHolder,
    private val stack: List<Screen>,
) {
    @Composable
    fun Provide(content: @Composable () -> Unit) {
        holder.SaveableStateProvider(detailStateKeys(stack).last(), content)
    }
}

/** One key per stack position, so the same screen pushed twice never shares state. */
internal fun detailStateKeys(stack: List<Screen>): List<String> = stack.mapIndexed { index, screen -> "$index:${screen.name}" }

@Composable
fun rememberDetailStateKeeper(stack: List<Screen>): DetailStateKeeper {
    val holder = rememberSaveableStateHolder()
    val kept = remember { mutableSetOf<String>() }
    val keys = detailStateKeys(stack)
    LaunchedEffect(keys) {
        (kept - keys.toSet()).forEach(holder::removeState)
        kept.clear()
        kept.addAll(keys)
    }
    return remember(holder, stack) { DetailStateKeeper(holder, stack) }
}
