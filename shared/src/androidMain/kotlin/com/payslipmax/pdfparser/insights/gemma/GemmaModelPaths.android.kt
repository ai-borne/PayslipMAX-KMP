package com.payslipmax.pdfparser.insights.gemma

import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.payslipmax.pdfparser.crypto.ContextHolder
import com.payslipmax.pdfparser.subscription.isDebugBuild
import java.io.File

actual fun gemmaModelStorageDir(): String = ContextHolder.context?.filesDir?.absolutePath ?: ""

actual fun fileExistsAt(path: String): Boolean = path.isNotEmpty() && File(path).exists()

/**
 * Resolves the Gemma base model's on-disk path via Play Asset Delivery. Per Google's guidance, an
 * asset pack's location is never cached across launches — this re-queries it fresh on every call,
 * since an app update or the user clearing app data can invalidate a previously-valid location.
 *
 * On-demand delivery only ever actually fetches through real Play distribution (Internal App
 * Sharing/testing track or a signed release) — a plain debug `adb install` has no pack to resolve.
 * Debug builds only fall back to a manually `adb push`-ed file in the app's files dir so Tier 6 is
 * testable on a bare debug install; release builds never take this branch.
 */
actual fun resolveInstalledGemmaModelPath(): String? {
    val fileName = GemmaModelStorageManager().getRecommendedModelFileName()
    // Mirrors AndroidGemmaBaseModelInstaller's own try/catch around Play Core resolution — an
    // unusable/fake Context (or any other device-specific Play Core failure) must degrade to the
    // debug fallback below rather than crash the parse pipeline.
    val location =
        ContextHolder.context?.let { context ->
            runCatching {
                AssetPackManagerFactory.getInstance(context).getPackLocation(AndroidGemmaBaseModelInstaller.PACK_NAME)
            }.getOrNull()
        }
    if (location != null) {
        return "${location.assetsPath()}/$fileName"
    }
    if (isDebugBuild()) {
        val sideloadPath = "${gemmaModelStorageDir()}/$fileName"
        if (fileExistsAt(sideloadPath)) return sideloadPath
    }
    return null
}
