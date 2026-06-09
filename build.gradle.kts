plugins {
    // Gradle plugins setup
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.ksp) apply false
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

tasks.register("checkFileSizes") {
    group = "verification"
    description = "Enforces that all Kotlin source files are under 300 lines of code."

    doLast {
        val maxLines = 300
        val violations = mutableListOf<String>()

        fileTree(projectDir) {
            include("**/src/**/*.kt")
            exclude("**/build/**")
        }.forEach { file ->
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
