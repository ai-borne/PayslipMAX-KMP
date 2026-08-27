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

    val isMac = System.getProperty("os.name").orEmpty().contains("Mac", ignoreCase = true)
    val xcodeDeveloperDir =
        providers.environmentVariable("DEVELOPER_DIR")
            .orElse(
                if (isMac && file("/usr/bin/xcode-select").exists()) {
                    providers.exec {
                        commandLine("xcode-select", "-p")
                    }.standardOutput.asText.map { it.trim() }
                } else {
                    providers.provider { "/Applications/Xcode.app/Contents/Developer" }
                },
            )
            .orElse("/Applications/Xcode.app/Contents/Developer")
            .get()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "composeApp"
            isStatic = true
            binaryOption("bundleId", "in.aiborne.payslipmax")
            export(project(":shared"))
        }
        iosTarget.binaries.all {
            val isSimulator = iosTarget.name.contains("Simulator") || iosTarget.name.endsWith("X64")
            val platform = if (isSimulator) "iphonesimulator" else "iphoneos"
            val sdk = if (isSimulator) "iPhoneSimulator" else "iPhoneOS"
            linkerOpts(
                "-L$xcodeDeveloperDir/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/$platform",
                "-L$xcodeDeveloperDir/Platforms/$sdk.platform/Developer/SDKs/$sdk.sdk/usr/lib/swift",
                "-lswift_Concurrency",
                "-lswiftCore",
                "-lswiftFoundation",
            )
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
            implementation(libs.play.asset.delivery)
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

// Resolve release signing credentials from keystore.properties, Gradle properties, or environment variables
val keystorePropertiesFile =
    rootProject.file("keystore.properties").takeIf { it.exists() }
        ?: project.file("keystore.properties").takeIf { it.exists() }
val keystoreProperties =
    Properties().apply {
        if (keystorePropertiesFile != null) {
            keystorePropertiesFile.inputStream().use { load(it) }
        }
    }

fun getSigningProperty(
    key: String,
    envKey: String,
): String? =
    keystoreProperties.getProperty(key)
        ?: providers.gradleProperty(key).orNull
        ?: providers.gradleProperty(envKey).orNull
        ?: providers.environmentVariable(envKey).orNull
        ?: providers.environmentVariable(key).orNull

val releaseKeystorePath = getSigningProperty("KEYSTORE_PATH", "RELEASE_KEYSTORE_PATH")
val releaseKeystorePassword = getSigningProperty("KEYSTORE_PASSWORD", "RELEASE_KEYSTORE_PASSWORD")
val releaseKeyAlias = getSigningProperty("KEY_ALIAS", "RELEASE_KEY_ALIAS")
val releaseKeyPassword = getSigningProperty("KEY_PASSWORD", "RELEASE_KEY_PASSWORD")

val isReleaseSigningConfigured =
    !releaseKeystorePath.isNullOrBlank() &&
        !releaseKeystorePassword.isNullOrBlank() &&
        !releaseKeyAlias.isNullOrBlank() &&
        !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "com.payslipmax.pdfparser"
    compileSdk = 35
    defaultConfig {
        applicationId = "in.aiborne.payslipmax"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = appVersionName
    }
    // On-demand asset pack carrying the Tier 6 Gemma base model (Play Asset Delivery).
    assetPacks += listOf(":gemmaModelPack")

    signingConfigs {
        if (isReleaseSigningConfigured) {
            create("release") {
                val storePath = releaseKeystorePath!!
                val resolvedStoreFile =
                    if (File(storePath).isAbsolute) {
                        File(storePath)
                    } else {
                        rootProject.file(storePath)
                    }
                storeFile = resolvedStoreFile
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (isReleaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
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
