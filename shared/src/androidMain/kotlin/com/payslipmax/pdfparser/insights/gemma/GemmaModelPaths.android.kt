package com.payslipmax.pdfparser.insights.gemma

import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.payslipmax.pdfparser.crypto.ContextHolder
import java.io.File

actual fun gemmaModelStorageDir(): String = ContextHolder.context?.filesDir?.absolutePath ?: ""

actual fun fileExistsAt(path: String): Boolean = path.isNotEmpty() && File(path).exists()

/**
 * Resolves the Gemma base model's on-disk path via Play Asset Delivery. Per Google's guidance, an
 * asset pack's location is never cached across launches — this re-queries it fresh on every call,
 * since an app update or the user clearing app data can invalidate a previously-valid location.
 */
actual fun resolveInstalledGemmaModelPath(): String? {
    val context = ContextHolder.context ?: return null
    val location =
        AssetPackManagerFactory.getInstance(context).getPackLocation(AndroidGemmaBaseModelInstaller.PACK_NAME)
            ?: return null
    return "${location.assetsPath()}/${GemmaModelStorageManager().getRecommendedModelFileName()}"
}
