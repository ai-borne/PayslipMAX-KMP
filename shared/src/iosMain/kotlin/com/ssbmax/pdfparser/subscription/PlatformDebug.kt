package com.ssbmax.pdfparser.subscription

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

@OptIn(ExperimentalNativeApi::class)
actual fun isDebugBuild(): Boolean {
    return Platform.isDebugBinary
}
