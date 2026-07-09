package com.payslipmax.pdfparser.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Applies the platform safe-area (status bar / notch, home indicator) inset to a detail screen's
 * root so it is self-contained — correct whether hosted inside [App]'s root `Scaffold` (Android,
 * and the iOS tab root) or as a standalone iOS `ComposeUIViewController` (a pushed detail screen).
 *
 * Single source of truth for *which* inset detail screens honor. Because Compose window insets are
 * consumed down the tree, this resolves to ~0 wherever an ancestor already handled them (e.g. under
 * App's `Scaffold`, which `consumeWindowInsets`) and to the real inset on a standalone VC — so the
 * same shared screen renders identically on both platforms without any platform-specific code.
 */
@Composable
fun Modifier.detailScreenSafeArea(): Modifier = windowInsetsPadding(WindowInsets.safeDrawing)
