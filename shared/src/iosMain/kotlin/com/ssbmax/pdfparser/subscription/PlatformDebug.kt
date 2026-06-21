package com.ssbmax.pdfparser.subscription

import kotlin.native.Platform

actual fun isDebugBuild(): Boolean {
    return Platform.isDebugBinary
}
