package com.payslipmax.pdfparser

import androidx.compose.ui.window.ComposeUIViewController
import com.payslipmax.pdfparser.auth.AuthTokenProvider
import com.payslipmax.pdfparser.di.appModule
import com.payslipmax.pdfparser.di.sharedModule
import com.payslipmax.pdfparser.ui.PayslipViewModel
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatformTools
import platform.UIKit.UIViewController

fun getAuthTokenProvider(): AuthTokenProvider = AuthTokenProvider()

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
