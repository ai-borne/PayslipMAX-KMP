package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.payslipmax.pdfparser.guide.model.GuideBundle

/** A level of the Claim Guide's own stack. Ids stay strings (never enums), so new areas are data-only changes. */
sealed interface GuideDestination {
    data object Home : GuideDestination

    data class Area(val areaId: String) : GuideDestination

    /** [facet] is the chosen facet chip, kept here so it survives restore (used from phase E3). */
    data class Case(val caseId: String, val facet: String? = null) : GuideDestination

    data class Card(val cardId: String) : GuideDestination

    data object Search : GuideDestination

    /** "What's new in the Guide": the change-log entries (M4). */
    data object Changes : GuideDestination
}

/** A list's place: its first visible item and how far that item is scrolled. Plain ints, so the saver can write it. */
data class GuideScroll(val index: Int, val offset: Int) {
    companion object {
        val Top = GuideScroll(0, 0)
    }
}

/**
 * The Guide's stack inside its tab. The app's [com.payslipmax.pdfparser.Screen] enum carries no arguments, so
 * the Guide keeps its own stack of ids rather than pushing app details. [GuideDestination.Home] is always at
 * the bottom; the stack changes only through [push], [pop], [popToHome], [upTo] (the breadcrumb) and
 * [selectFacet]. Backed by [mutableStateOf] so Compose recomposes on every change.
 *
 * It also keeps each level's scroll position. The tab's content leaves composition when another tab is shown,
 * so a `rememberSaveable` list state would be lost there; this hoisted state survives tab switches, locking and
 * (through [GuideNavStateSaver]) process death.
 */
@Stable
class GuideNavState(
    initialAboveHome: List<GuideDestination> = emptyList(),
    initialScrolls: Map<Int, GuideScroll> = emptyMap(),
) {
    var stack: List<GuideDestination> by mutableStateOf(listOf(GuideDestination.Home) + initialAboveHome.filterNot { it == GuideDestination.Home })
        private set

    // Keyed by stack level. Not snapshot state: a level reads it only when it is composed again.
    private val scrolls: MutableMap<Int, GuideScroll> = initialScrolls.filterKeys { it in stack.indices }.toMutableMap()

    val current: GuideDestination get() = stack.last()

    /** True when back has a Guide level to return to; false at Home, so Android back leaves the tab. */
    val canPop: Boolean get() = stack.size > 1

    /** Where the current level was left; [GuideScroll.Top] for a level opened fresh. */
    val currentScroll: GuideScroll get() = scrolls[stack.lastIndex] ?: GuideScroll.Top

    /** Every level's saved scroll, for [GuideNavStateSaver]. */
    internal val savedScrolls: Map<Int, GuideScroll> get() = scrolls.toMap()

    fun push(destination: GuideDestination) {
        if (destination == GuideDestination.Home) popToHome() else replaceStack(stack + destination)
    }

    /** @return true if a level was popped, false at Home (a no-op). */
    fun pop(): Boolean {
        if (!canPop) return false
        replaceStack(stack.dropLast(1))
        return true
    }

    fun popToHome() {
        replaceStack(listOf(GuideDestination.Home))
    }

    /**
     * The breadcrumb: goes up to the last level of [path] (the levels above Home that lead to it). If that level
     * is already on the stack, everything above it is popped; otherwise, for a card opened from search or Pay Audit,
     * the stack becomes Home plus [path], so back from there walks up the same way.
     */
    fun upTo(path: List<GuideDestination>) {
        val target = path.lastOrNull() ?: return popToHome()
        val at = stack.lastIndexOf(target)
        replaceStack(if (at >= 0) stack.take(at + 1) else listOf(GuideDestination.Home) + path)
    }

    /** Filters the feed on top by [facet] (null = every card), starting it at the top. Ignored on other levels. */
    fun selectFacet(facet: String?) {
        val case = current as? GuideDestination.Case ?: return
        scrolls.remove(stack.lastIndex)
        stack = stack.dropLast(1) + case.copy(facet = facet)
    }

    /** Records where [level] (the current one by default) is scrolled; a level no longer on the stack is ignored. */
    fun saveScroll(
        scroll: GuideScroll,
        level: Int = stack.lastIndex,
    ) {
        if (level !in stack.indices) return
        if (scroll == GuideScroll.Top) scrolls.remove(level) else scrolls[level] = scroll
    }

    /**
     * Cuts the stack at the first destination [bundle] does not know, and everything above it (the
     * `AppNavStateSaver` rule: never rebuild an order the user did not create). Run once the bundle has loaded,
     * because a restore can happen before it does.
     */
    fun retainKnown(bundle: GuideBundle) {
        val kept = stack.takeWhile(bundle::knows)
        if (kept.size != stack.size) replaceStack(kept.ifEmpty { listOf(GuideDestination.Home) })
    }

    /** Every stack change goes through here, so a level that is left never hands its scroll to the next one there. */
    private fun replaceStack(next: List<GuideDestination>) {
        val keep = minOf(next.size, stack.zip(next).takeWhile { (a, b) -> a == b }.size)
        scrolls.keys.retainAll { it < keep }
        stack = next
    }
}

/** Whether this bundle still has the area, case, facet or card a destination names. */
fun GuideBundle.knows(destination: GuideDestination): Boolean =
    when (destination) {
        GuideDestination.Home, GuideDestination.Search -> true
        GuideDestination.Changes -> changes.isNotEmpty()
        is GuideDestination.Area -> nav.any { it.id == destination.areaId }
        is GuideDestination.Case ->
            nav.any { area -> area.cases.any { it.id == destination.caseId } } &&
                (destination.facet == null || destination.facet in facets)
        is GuideDestination.Card -> cards.any { it.id == destination.cardId }
    }
