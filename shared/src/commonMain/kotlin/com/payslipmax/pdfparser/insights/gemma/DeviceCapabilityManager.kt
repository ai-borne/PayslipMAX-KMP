package com.payslipmax.pdfparser.insights.gemma

expect class DeviceCapabilityManager() {
    fun checkGemmaSupport(
        requiredRamMb: Long = 4096L,
        requiredStorageMb: Long = 1500L,
    ): GemmaSupportStatus

    companion object {
        fun isRamSufficientForGemma(
            totalRamMb: Long,
            requiredRamMb: Long = 4096L,
        ): Boolean
    }
}
