import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
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

private val gemmaModelAssetsDir = layout.projectDirectory.dir("src/main/assets")
private val gemmaModelPlaceholderFile = gemmaModelAssetsDir.file("PLACEHOLDER_MODEL_NOT_YET_ACQUIRED.txt").asFile

abstract class FetchGemmaModelTask : DefaultTask() {
    @get:Input
    @get:Optional
    abstract val modelSourcePath: Property<String>

    @get:OutputFile
    abstract val targetModelFile: RegularFileProperty

    @get:OutputFile
    @get:Optional
    abstract val placeholderFile: RegularFileProperty

    @TaskAction
    fun fetch() {
        val sourcePath =
            modelSourcePath.orNull
                ?: error(
                    "No Gemma model source configured. Pass -PgemmaModelSourcePath=/path/to/gemma-model.litertlm " +
                        "or set the GEMMA_MODEL_SOURCE_PATH environment variable before running a release bundle.",
                )
        val sourceFile = File(sourcePath)
        check(sourceFile.exists()) { "Gemma model source file not found at $sourcePath" }

        val expectedSha256 = "1325ae366d31950f137c9c357b9fa89448b176d76998180c08ceaca78bba98be"
        val actualSha256 = sha256Of(sourceFile)
        check(actualSha256 == expectedSha256) {
            "Gemma model checksum mismatch: expected $expectedSha256 but " +
                "$sourcePath hashed to $actualSha256 — refusing to package a corrupted/wrong-version model."
        }

        val targetFile = targetModelFile.get().asFile
        sourceFile.copyTo(targetFile, overwrite = true)
        val placeholder = placeholderFile.orNull?.asFile
        if (placeholder != null && placeholder.exists()) {
            placeholder.delete()
        }
        logger.lifecycle("Gemma model verified (sha256=$actualSha256) and copied to $targetFile")
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
}

val gemmaModelSourceProvider =
    providers.gradleProperty("gemmaModelSourcePath")
        .orElse(providers.environmentVariable("GEMMA_MODEL_SOURCE_PATH"))

tasks.register<FetchGemmaModelTask>("fetchGemmaModelForRelease") {
    group = "build"
    description = "Verifies and copies the real Gemma base model into src/main/assets before bundleRelease."
    modelSourcePath.set(gemmaModelSourceProvider)
    targetModelFile.set(gemmaModelAssetsDir.file(gemmaModelFileName))
    placeholderFile.set(gemmaModelPlaceholderFile)
}
