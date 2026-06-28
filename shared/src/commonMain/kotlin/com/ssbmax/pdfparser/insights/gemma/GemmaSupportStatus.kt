package com.ssbmax.pdfparser.insights.gemma

sealed class GemmaSupportStatus {
    data object Supported : GemmaSupportStatus()

    data class InsufficientRam(val requiredMb: Long, val availableMb: Long) : GemmaSupportStatus()

    data class InsufficientStorage(val requiredMb: Long, val availableMb: Long) : GemmaSupportStatus()

    data class UnsupportedArchitecture(val reason: String) : GemmaSupportStatus()

    val isSupported: Boolean get() = this is Supported
}
