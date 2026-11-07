// R8 Check 2 for the Claim Guide (docs/Plan/rule_cards/16_guide_phase_plan.md): a black-box UiAutomator smoke that
// drives the R8-minified :composeApp `minifiedTest` app. Self-instrumenting, so it runs in its own process: an
// in-app androidTest would share the app's R8-renamed Kotlin stdlib and crash before any test ran.
// Run it with scripts/run_guide_minified_smoke.sh (a device is needed; there is no CI emulator by owner decision).
plugins {
    alias(libs.plugins.androidTest)
}

android {
    namespace = "com.payslipmax.pdfparser.smoke"
    compileSdk = 36
    targetProjectPath = ":composeApp"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    defaultConfig {
        minSdk = 26
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        // Only the app's minifiedTest variant is tested; debug signing like it.
        create("minifiedTest") {
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// The app's UI copy, read from its single source (GuideStrings.kt, AppStringsOnboarding.kt, PreClaimStrings.kt) into a generated Java
// class: self-instrumenting means the app's classes are not on this module's classpath, and a copied string would
// drift. A constant that disappears fails the build here.
abstract class GenerateSmokeStrings : DefaultTask() {
    @get:InputFiles
    abstract val sources: ConfigurableFileCollection

    @get:Input
    abstract val names: ListProperty<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val text = sources.files.joinToString("\n") { it.readText() }
        val fields =
            names.get().joinToString("\n") { name ->
                val value =
                    Regex("""const val $name = (".*")""").find(text)?.groupValues?.get(1)
                        ?: throw GradleException("guideSmokeTest: no `const val $name` in ${sources.files.map { it.name }}")
                "    static final String $name = $value;"
            }
        // The short Guide Home tile names (GuideStrings.areaTileLabel), read from the same file so a rename cannot leave this test stale.
        val tileBlock =
            Regex("""fun areaTileLabel\(.*?\n        }""", RegexOption.DOT_MATCHES_ALL).find(text)?.value
                ?: throw GradleException("guideSmokeTest: no `fun areaTileLabel` in the string files")
        val tiles =
            Regex(""""(\w+)" -> (".*")""").findAll(tileBlock).joinToString("\n") { "            case \"${it.groupValues[1]}\": return ${it.groupValues[2]};" }
        val dir = outputDir.get().dir("com/payslipmax/pdfparser/smoke").asFile.apply { mkdirs() }
        dir.resolve("SmokeStrings.java").writeText(
            "package com.payslipmax.pdfparser.smoke;\n\n/** Generated from the app's string files. */\nfinal class SmokeStrings {\n$fields\n\n    /** The Guide Home tile name for a bundle area id (the bundle's own title when the app names none). */\n    static String areaTile(String id, String bundleTitle) {\n        switch (id) {\n$tiles\n            default: return bundleTitle;\n        }\n    }\n}\n",
        )
    }
}

val generateSmokeStrings by tasks.registering(GenerateSmokeStrings::class) {
    val theme = rootProject.file("composeApp/src/commonMain/kotlin/com/payslipmax/pdfparser/ui/theme")
    sources.from(theme.resolve("GuideStrings.kt"), theme.resolve("AppStringsOnboarding.kt"), theme.resolve("PreClaimStrings.kt"), theme.resolve("AppStrings.kt"), theme.resolve("BackupRestoreStrings.kt"))
    names.set(listOf("tabLabel", "homeSection", "loadFailedTitle", "sectionKeyPoints", "searchOpen", "onboardingSkip", "onboardingCoachmarkDismiss", "entryTitle", "tileTitle", "chooseMove", "trRule", "trRuleChoose", "nightlyRate", "navigationSettings", "settingsRowBackupLabel", "settingsBackupSaveBtn", "labelPayslipsBackedUp", "wrongPasswordOrNotABackup", "restoreBtn", "cancelBtn"))
    outputDir.set(layout.buildDirectory.dir("generated/smokeStrings"))
}

androidComponents {
    beforeVariants(selector().withBuildType("debug")) { it.enable = false }
    onVariants { variant ->
        variant.sources.java?.addGeneratedSourceDirectory(generateSmokeStrings, GenerateSmokeStrings::outputDir)
    }
}

dependencies {
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
}
