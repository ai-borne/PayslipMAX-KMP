package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.BuildConfig

// A static final constant per build type (composeApp/build.gradle.kts), so R8 drops the Guide UI from release.
internal actual fun isGuidePreviewBuild(): Boolean = BuildConfig.GUIDE_PREVIEW
