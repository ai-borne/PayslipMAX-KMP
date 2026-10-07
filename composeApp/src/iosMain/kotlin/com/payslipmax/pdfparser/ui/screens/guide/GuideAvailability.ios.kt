package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.subscription.isDebugBuild

// Xcode Debug builds only; TestFlight and App Store binaries are release binaries.
internal actual fun isGuidePreviewBuild(): Boolean = isDebugBuild()
