package com.ssbmax.pdfparser.subscription

import com.ssbmax.pdfparser.shared.BuildConfig

actual fun isDebugBuild(): Boolean {
    return BuildConfig.DEBUG
}
