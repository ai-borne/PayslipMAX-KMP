package com.payslipmax.pdfparser.insights.gemma

expect class DeviceCapabilityManager() {
    fun checkGemmaSupport(
        requiredRamMb: Long = 3500L,
        requiredStorageMb: Long = 1500L,
    ): GemmaSupportStatus
}
