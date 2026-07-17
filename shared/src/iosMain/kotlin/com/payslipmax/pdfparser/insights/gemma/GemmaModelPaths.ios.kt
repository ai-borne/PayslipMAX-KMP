@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.payslipmax.pdfparser.insights.gemma

import com.payslipmax.pdfparser.subscription.isDebugBuild
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual fun gemmaModelStorageDir(): String = documentDirectory()

actual fun fileExistsAt(path: String): Boolean = path.isNotEmpty() && NSFileManager.defaultManager.fileExistsAtPath(path)

/**
 * Must match exactly the App Group entitlement shared between `iosApp` and the (not-yet-created)
 * Background Assets extension target — see Phase 4's Xcode-side handoff in
 * docs/AI_INSIGHTS_PIPELINE.md. Getting this string wrong is silent: a mismatched identifier just
 * makes [NSFileManager.containerURLForSecurityApplicationGroupIdentifier] return null, which
 * [resolveInstalledGemmaModelPath] already treats identically to "not installed yet".
 */
private const val GEMMA_APP_GROUP_IDENTIFIER = "group.com.payslipmax.pdfparser.gemma"

/**
 * Resolves the Gemma base model's on-disk path inside the Background Assets App Group container.
 * Returns null both when the App Group entitlement isn't configured yet (Phase 4's Xcode-side work,
 * blocked on Apple Developer Program enrollment) and when the entitlement exists but the extension
 * hasn't finished downloading the model into it — both cases mean "not ready" to the caller.
 *
 * Debug builds additionally fall back to a manually-sideloaded file in the app's Documents
 * directory (e.g. dropped in via Xcode's device file browser) when the App Group container isn't
 * reachable — the only way to test Tier 6 on a real device before the Xcode-side extension target
 * exists. Release builds never take this branch.
 */
actual fun resolveInstalledGemmaModelPath(): String? {
    val fileName = GemmaModelStorageManager().getRecommendedModelFileName()
    val containerPath =
        NSFileManager.defaultManager
            .containerURLForSecurityApplicationGroupIdentifier(GEMMA_APP_GROUP_IDENTIFIER)
            ?.path
    if (containerPath != null) {
        val modelPath = "$containerPath/$fileName"
        if (fileExistsAt(modelPath)) return modelPath
    }
    if (isDebugBuild()) {
        val sideloadPath = "${gemmaModelStorageDir()}/$fileName"
        if (fileExistsAt(sideloadPath)) return sideloadPath
    }
    return null
}

private fun documentDirectory(): String {
    val documentDirectory =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
    return documentDirectory?.path ?: ""
}
