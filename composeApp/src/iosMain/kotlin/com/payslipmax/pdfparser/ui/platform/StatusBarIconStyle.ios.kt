package com.payslipmax.pdfparser.ui.platform

import androidx.compose.runtime.Composable

// iOS draws the status bar from the view controller's interface style, not from Compose, so this is a
// deliberate no-op until the simulator/device check says whether forced Light/Dark needs an override.
@Composable
actual fun ApplyStatusBarIconStyle(style: StatusBarIconStyle) = Unit
