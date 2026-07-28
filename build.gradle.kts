import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction

plugins {
    // Gradle plugins setup
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.firebaseCrashlytics) apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    afterEvaluate {
        tasks.findByName("check")?.dependsOn(":checkFileSizes")
    }

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        filter {
            exclude { element ->
                element.file.absolutePath.contains("/build/") ||
                    element.file.absolutePath.contains("/generated/")
            }
        }
    }
}

abstract class CheckFileSizesTask : DefaultTask() {
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @TaskAction
    fun check() {
        val maxLines = 300
        val violations = mutableListOf<String>()
        val projectDir = rootDirectory.get().asFile

        sourceFiles.forEach { file ->
            val lines = file.readLines()
            if (lines.size > maxLines) {
                violations.add("${file.relativeTo(projectDir)}: ${lines.size} lines (max: $maxLines)")
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Quality Framework Violation: Kotlin files exceed the $maxLines line limit:\n" + violations.joinToString("\n"),
            )
        }
    }
}

tasks.register<CheckFileSizesTask>("checkFileSizes") {
    group = "verification"
    description = "Enforces that all Kotlin source files are under 300 lines of code."
    rootDirectory.set(project.layout.projectDirectory)
    sourceFiles.from(
        project.fileTree(project.layout.projectDirectory) {
            include("**/src/**/*.kt")
            exclude("**/build/**")
        },
    )
}
