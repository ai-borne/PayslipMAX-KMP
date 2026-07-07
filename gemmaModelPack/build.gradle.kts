import java.security.MessageDigest

plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("gemma_model_pack")
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}

// Must match GemmaModelStorageManager.ACTIVE_SLOT_FILE / getRecommendedModelFileName() — that's
// the filename resolveInstalledGemmaModelPath() (GemmaModelPaths.android.kt) looks for inside the
// installed pack's assetsPath() at runtime.
private val gemmaModelFileName = "gemma-active.litertlm"

// Known-good SHA-256 of the Tier 6 Gemma base model (gemma3-1b-it-int4.litertlm). The fetch task
// refuses to package any source file whose hash doesn't match this, so a corrupted or wrong-version
// local copy can never silently ship in a release build.
private val expectedGemmaModelSha256 = "1325ae366d31950f137c9c357b9fa89448b176d76998180c08ceaca78bba98be"

private val gemmaModelAssetsDir = layout.projectDirectory.dir("src/main/assets")
private val gemmaModelTargetFile = gemmaModelAssetsDir.file(gemmaModelFileName).asFile
private val gemmaModelPlaceholderFile = gemmaModelAssetsDir.file("PLACEHOLDER_MODEL_NOT_YET_ACQUIRED.txt").asFile

/**
 * Populates src/main/assets/ with the real Tier 6 Gemma base model before a release bundle is
 * packaged. Never committed to git (see root .gitignore) — this task is the only thing that ever
 * writes the real binary here, and only after verifying its SHA-256. Point it at a local copy via
 * `-PgemmaModelSourcePath=/path/to/gemma-model.litertlm` or the `GEMMA_MODEL_SOURCE_PATH` env var;
 * a proper release pipeline would instead fetch this from wherever the model is archived (e.g. the
 * original Hugging Face source), but no such fetch step exists yet — see the Phase 3 handoff.
 */
tasks.register("fetchGemmaModelForRelease") {
    group = "build"
    description = "Verifies and copies the real Gemma base model into src/main/assets before bundleRelease."

    outputs.file(gemmaModelTargetFile)
    outputs.upToDateWhen {
        gemmaModelTargetFile.exists() && sha256Of(gemmaModelTargetFile) == expectedGemmaModelSha256
    }

    doLast {
        val sourcePath =
            (project.findProperty("gemmaModelSourcePath") as String?)
                ?: System.getenv("GEMMA_MODEL_SOURCE_PATH")
                ?: error(
                    "No Gemma model source configured. Pass -PgemmaModelSourcePath=/path/to/gemma-model.litertlm " +
                        "or set the GEMMA_MODEL_SOURCE_PATH environment variable before running a release bundle.",
                )
        val sourceFile = File(sourcePath)
        check(sourceFile.exists()) { "Gemma model source file not found at $sourcePath" }

        val actualSha256 = sha256Of(sourceFile)
        check(actualSha256 == expectedGemmaModelSha256) {
            "Gemma model checksum mismatch: expected $expectedGemmaModelSha256 but " +
                "$sourcePath hashed to $actualSha256 — refusing to package a corrupted/wrong-version model."
        }

        sourceFile.copyTo(gemmaModelTargetFile, overwrite = true)
        if (gemmaModelPlaceholderFile.exists()) {
            gemmaModelPlaceholderFile.delete()
        }
        logger.lifecycle("Gemma model verified (sha256=$actualSha256) and copied to $gemmaModelTargetFile")
    }
}

private fun sha256Of(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(64 * 1024)
        var read: Int
        while (input.read(buffer).also { read = it } > 0) {
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
