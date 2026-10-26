package com.payslipmax.pdfparser.nav

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * iOS only: a pushed detail is a native screen that covers the tab tree, so a reminder tap that opens Guide, Part II, Due must first close
 * every detail ([popToRoot] is `NavBridge.popToRoot`). It does so when a tap is waiting and the app is unlocked, once per waiting tap, and
 * leaves the opening itself to the Compose side (`Part2LinkEffect`), which takes the link. A locked app pushes and pops nothing, so a tap that
 * waited behind the lock screen closes the details when the app unlocks.
 */
class PendingLinkStackReset(
    private val pending: PendingPart2Link,
    private val locked: Flow<Boolean>,
    private val popToRoot: () -> Unit,
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(pending.link, locked) { link, isLocked -> link != null && !isLocked }
                .distinctUntilChanged()
                .collect { if (it) popToRoot() }
        }
    }
}
