package com.payslipmax.pdfparser.ui.platform

import androidx.compose.runtime.Composable

// iOS needs no override: on the owner's iPhone (2026-10-10) the status bar contrasted with the page on every
// tab and on the Guide in light and dark, including the forced Light/Dark choices (checklist row 14 in
// docs/Plan/rule_cards/16_guide_phase_plan.md). Android has to set the flag itself, so the shared
// statusBarIconStyleFor() decision is applied there only. If iOS ever regresses, implement it here.
@Composable
actual fun ApplyStatusBarIconStyle(style: StatusBarIconStyle) = Unit
