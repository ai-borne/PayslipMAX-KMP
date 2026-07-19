package com.payslipmax.pdfparser.platform

import platform.Foundation.NSBundle

actual fun platformAppVersion(): String {
    return (NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String) ?: "unknown"
}
