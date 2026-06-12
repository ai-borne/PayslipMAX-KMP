package com.ssbmax.pdfparser

import androidx.compose.ui.window.ComposeUIViewController
import com.ssbmax.pdfparser.di.appModule
import com.ssbmax.pdfparser.di.sharedModule
import com.ssbmax.pdfparser.ui.PayslipViewModel
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatformTools
import platform.UIKit.UIViewController

fun MainViewController(
    onPickPdf: (onResult: (ByteArray, String) -> Unit) -> Unit,
    onOpenPdf: (ByteArray, String) -> Unit,
): UIViewController {
    // Auto-initialize Koin context if not already set up
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        startKoin {
            modules(sharedModule, appModule)
        }
    }

    val viewModel = KoinPlatformTools.defaultContext().get().get<PayslipViewModel>()

    return ComposeUIViewController {
        App(
            viewModel = viewModel,
            onPickPdf = onPickPdf,
            onOpenPdf = onOpenPdf,
        )
    }
}
