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
 * delivery mechanism (Play Asset Delivery on Android, Background Assets on iOS), or null if it has
 * not been installed yet. Both actuals additionally fall back to a manually-sideloaded file in
 * [gemmaModelStorageDir] in debug builds only, since store-delivered on-demand assets can't be
 * fetched outside real store distribution (Android) or before the Background Assets Xcode-side
 * work lands (iOS) — see each actual's doc for detail.
 */
expect fun resolveInstalledGemmaModelPath(): String?
