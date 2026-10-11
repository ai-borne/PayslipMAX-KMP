package com.payslipmax.pdfparser.di

import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimViewModel
import org.koin.dsl.module

/**
 * TA/DA pre-claim bindings. Included only where `isPreClaimEnabled()` (the dark-launch flag and the Guide kill switch), so with the
 * flag off R8 drops the ViewModel with the rest of the worksheet. It needs the Guide's own bindings, which it reuses.
 */
val preClaimModule =
    module {
        // A new worksheet per screen, not app-scoped like the Guide models: the form must die with the screen, a lock and process death.
        factory { PreClaimViewModel(guide = get(), crashReporter = get()) }
    }
