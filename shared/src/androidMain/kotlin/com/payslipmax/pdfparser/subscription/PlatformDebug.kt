package com.payslipmax.pdfparser.subscription

import com.payslipmax.pdfparser.shared.BuildConfig

actual fun isDebugBuild(): Boolean {
    return BuildConfig.DEBUG
}
