package com.payslipmax.pdfparser.ui.screens.guide

/** Access for Guide UI tests that are not about the paywall: the whole Guide is open, and unlocking is never needed. */
internal val UnlockedGuideAccess = GuideAccess(isUnlocked = true, onUnlock = {})
