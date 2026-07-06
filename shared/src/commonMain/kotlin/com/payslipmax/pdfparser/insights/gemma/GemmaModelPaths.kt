package com.payslipmax.pdfparser.insights.gemma

/**
 * Platform-writable directory the Gemma model file is read from. Single source of truth for
 * "where the model lives" — [GemmaModelStorageManager] and the production parse path
 * (`PdfParser.kt`, both platforms) resolve through this.
 */
expect fun gemmaModelStorageDir(): String

expect fun fileExistsAt(path: String): Boolean

/**
 * Resolves the on-disk path of the Gemma base model as installed by the platform store's asset
 * delivery mechanism (Play Asset Delivery on Android, Background Assets on iOS), or null if it
 * has not been installed yet. Phase 1 placeholder returning null on both platforms — Android's
 * `AssetPackManager`-backed resolution lands in Phase 3, iOS's App Group container resolution in
 * Phase 4.
 */
expect fun resolveInstalledGemmaModelPath(): String?
