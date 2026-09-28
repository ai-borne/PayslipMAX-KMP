package com.payslipmax.pcdao.repository

/**
 * Multiplatform asset loading abstraction for PCDA canonical datasets.
 * Implementations load canonical JSON payloads from local platform resources
 * without network calls.
 */
interface PcdaoAssetProvider {
    suspend fun loadAsset(fileName: String): String
}
