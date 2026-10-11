package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.subscription.LaunchFlags

/**
 * Whether the Guide tab and every Guide entry point exist: the [LaunchFlags.GUIDE_ENABLED] kill switch and nothing
 * else, in every build type. Switched off, the tab, the routes, the Pay Audit link and the Guide Koin module are all
 * absent, and R8 drops the Guide UI from release.
 */
fun isGuideEnabled(): Boolean = LaunchFlags.GUIDE_ENABLED
