import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// SSOT for the app's marketing version: shared by this Gradle build and iosApp/Configuration/Config.xcconfig.
val appVersionName =
    Properties().apply {
        load(rootProject.file("version.properties").inputStream())
    }.getProperty("MARKETING_VERSION")

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "composeApp"
            isStatic = true
            binaryOption("bundleId", "com.payslipmax.pdfparser")
            export(project(":shared"))
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
            api(project(":shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(libs.compose.ui.backhandler)
            implementation(libs.compose.material.icons.core)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.koin.compose)
            implementation(libs.ktor.client.core)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.kotlinx.coroutines.android)
            // Firebase Crashlytics — auto-initializes via ContentProvider; captures native-bridge
            // crashes during dev/beta. BOM (pinned in the legacy dependencies block below) aligns
            // the version with the existing firebase-auth-ktx already used in :shared.
            implementation(libs.firebase.crashlytics)
            implementation(libs.firebase.analytics)
            // Play Asset Delivery — MainActivity wires the AssetPackManager confirmation-dialog hook
            implementation(libs.play.asset.delivery.ktx)
            // asset-delivery-ktx transitively pulls androidx.fragment:fragment:1.1.0, too old for
            // registerForActivityResult (lint: InvalidFragmentVersionForActivityResult) — force it
            // up to a version compatible with androidx.activity's activity-result APIs.
            implementation(libs.androidx.fragment)
        }

        iosMain.dependencies {
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }

        val androidUnitTest by getting {
            dependencies {
                implementation("org.robolectric:robolectric:4.12.2")
                implementation("androidx.compose.ui:ui-test-junit4:1.9.4")
                implementation("androidx.compose.ui:ui-test-manifest:1.9.4")
            }
        }
    }
}

android {
    namespace = "com.payslipmax.pdfparser"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.payslipmax.pdfparser"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = appVersionName
    }
    // On-demand asset pack carrying the Tier 6 Gemma base model (Play Asset Delivery).
    assetPacks += listOf(":gemmaModelPack")
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Firebase BOM: pins firebase-crashlytics version (must be in legacy block, not KMP sourceSet)
dependencies {
    add("androidMainImplementation", platform(libs.firebase.bom))
    testImplementation(libs.mockk)
}

// A release bundle must never ship the placeholder that stands in for the real Gemma model in
// debug builds — verify and copy the real binary into gemmaModelPack's assets first.
tasks.matching { it.name == "assetPackReleasePreBundleTask" }.configureEach {
    dependsOn(":gemmaModelPack:fetchGemmaModelForRelease")
}
