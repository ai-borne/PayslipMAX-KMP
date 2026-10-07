package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.subscription.LaunchFlags

/**
 * Whether the Guide tab and every Guide entry point exist. Release builds follow [LaunchFlags.GUIDE_ENABLED]
 * (false until phase E9, so release behaves exactly as before the Guide); debug builds and the Android
 * `minifiedTest` smoke build always show it so each phase can be tried before launch.
 */
fun isGuideEnabled(): Boolean = LaunchFlags.GUIDE_ENABLED || isGuidePreviewBuild()

/** True for debug builds (and Android's `minifiedTest`); a compile-time constant false in release. */
internal expect fun isGuidePreviewBuild(): Boolean
