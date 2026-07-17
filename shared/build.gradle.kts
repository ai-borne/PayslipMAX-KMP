import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // iOS targets
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
        iosTarget.compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.pdfbox.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.litertlm.android)
            implementation(libs.firebase.auth.ktx)
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.crashlytics)
            // Play Asset Delivery — Tier 6 base model on-demand install (AndroidGemmaBaseModelInstaller)
            implementation(libs.play.asset.delivery.ktx)
        }

        val androidUnitTest by getting {
            dependencies {
                implementation("org.json:json:20240303")
                implementation(libs.mockk)
                implementation("org.robolectric:robolectric:4.12.2")
            }
        }

        iosMain.dependencies {
            // iOS native frameworks like PDFKit are imported automatically via platform libraries
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "com.payslipmax.pdfparser.shared"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

// Forward opt-in local-corpus system properties to the forked unit-test JVM so the
// developer-only CorpusCaptureTest / PlatformPdfParserTest can locate real PDFs. These are
// absent in CI, so the gated tests skip cleanly and no machine-specific paths are committed.
tasks.withType<Test>().configureEach {
    listOf(
        "payslip.localCorpus",
        "payslip.localCorpus.json",
        "payslip.localCorpus.password",
        "payslip.localCorpus.minYear",
        "payslip.localCorpus.out",
    ).forEach { key ->
        System.getProperty(key)?.let { systemProperty(key, it) }
    }
}

// Forward corpus paths into the iOS Simulator test binary. simctl spawn does not inherit the
// host environment; it only passes through vars prefixed with SIMCTL_CHILD_. Set these in your
// shell before calling ./gradlew iosSimulatorArm64Test:
//   export SIMCTL_CHILD_PAYSLIP_LOCAL_CORPUS="/path/to/Pay Slip Elements"
//   export SIMCTL_CHILD_PAYSLIP_LOCAL_CORPUS_JSON="/path/to/payslips_data_standardized.json"
//   export SIMCTL_CHILD_PAYSLIP_LOCAL_CORPUS_TOKENS_OUT="/tmp/ios_corpus_tokens"   # IosTokenCaptureTest
// The test reads PAYSLIP_LOCAL_CORPUS / PAYSLIP_LOCAL_CORPUS_JSON (without prefix) at runtime
// because simctl strips the SIMCTL_CHILD_ prefix before setting the child's env.

// KSP: generate Room _Impl for every KMP target
// Firebase BOM: pins firebase-auth-ktx version (must be in legacy block, not KMP sourceSet)
dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosX64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("androidMainImplementation", platform(libs.firebase.bom))
}
