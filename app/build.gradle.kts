import java.io.StringReader
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.baselineprofile)
}

// Release signing key, read from keystore.properties at the project root (git-ignored; see
// keystore.properties.example). Read through a Gradle provider so the configuration cache
// notices when the file is added or changed.
val keystoreProperties = Properties().apply {
    providers.fileContents(rootProject.layout.projectDirectory.file("keystore.properties"))
        .asText.orNull
        ?.let { load(StringReader(it)) }
}

/*
 * versionCode counts the commits, so a build from any later commit installs over the one
 * before it without anyone remembering to bump a number — Android refuses an update whose code
 * isn't higher. Builds handed out should come from a commit. Without git it falls back to 1.
 */
val commitCount: Int = runCatching {
    providers.exec { commandLine("git", "rev-list", "--count", "HEAD") }.standardOutput.asText.get().trim().toInt()
}.getOrElse {
    logger.warn("versionCode: git rev-list failed (${it.message}); using 1")
    1
}

android {
    testOptions {
        // Robolectric needs the merged resources (strings, theme) to render composables.
        unitTests.isIncludeAndroidResources = true
    }

    namespace = "com.example.heavyduty"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        // The app's identity on devices and on Google Play: changing it later makes a different app,
        // and installs of the old one keep their data to themselves. The Kotlin packages and
        // namespace stay com.example.* — they're source layout, not identity.
        applicationId = "com.waldoz.heavyduty"
        minSdk = 24
        targetSdk = 37
        versionCode = commitCount
        // Set by hand when a release is worth naming (0.1.0 → 0.2.0…); shown in Perfil.
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (!keystoreProperties.isEmpty) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        // Studio's debug builds are signed with this PC's debug key and release with the real
        // one: as one app they couldn't replace each other. Side by side, the debug one apart.
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appLabel"] = "HeavyDuty Debug"
        }
        release {
            manifestPlaceholders["appLabel"] = "@string/app_name"
            // R8: shrinks, optimizes and obfuscates code and drops unused resources. Its keep
            // rules live in src/main/keepRules/, which AGP passes to R8 by itself.
            optimization {
                enable = true
            }
            // The real key when keystore.properties exists. Without it, the debug key, so release
            // (and the benchmark builds derived from it) can still be installed and measured
            // locally — but such an APK/AAB can't be uploaded to Google Play.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // java.time (LocalDate, DayOfWeek…) needs API 26 natively; minSdk is 24.
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
    }

    bundle {
        // The app switches language itself (Spanish by default, English from the system's
        // per-app setting), so every install needs both: with Play's language splits a phone
        // only gets its own language and English could be missing when chosen. Strings are tiny.
        language {
            enableSplit = false
        }
    }
}

androidComponents {
    finalizeDsl { android ->
        // The baselineprofile plugin copies release into nonMinifiedRelease but, with AGP 9's
        // `optimization` DSL, doesn't turn R8 off there. The profile must be recorded on
        // unobfuscated code or its class/method names won't match the release build.
        android.buildTypes.findByName("nonMinifiedRelease")?.optimization?.enable = false
    }
}

baselineProfile {
    // Regenerate on demand (./gradlew :app:generateBaselineProfile), not on every release build.
    automaticGenerationDuringBuild = false
    // Also reorder the dex so startup code sits together on disk.
    dexLayoutOptimization = true
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Installs the Baseline Profile on devices where the app isn't installed from Play.
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))

    // Core modules
    implementation(project(":core:core-common"))
    implementation(project(":core:core-domain"))
    implementation(project(":core:core-ui"))
    implementation(project(":core:core-data"))

    // Feature modules
    implementation(project(":feature:feature-exercises"))
    implementation(project(":feature:feature-routines"))
    implementation(project(":feature:feature-workout"))
    implementation(project(":feature:feature-history"))
    implementation(project(":feature:feature-timer"))
    implementation(project(":feature:feature-calendar"))
    implementation(project(":feature:feature-dashboard"))
    implementation(project(":feature:feature-profile"))
    implementation(project(":feature:feature-stats"))

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.material)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Debug builds only: reports leaked screens/ViewModels with a notification and heap analysis.
    debugImplementation(libs.leakcanary.android)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Testing
    testImplementation(libs.junit)
    testImplementation(project(":core:core-testing"))

    // Compose UI tests on the JVM (Robolectric): no device needed, run with the unit tests.
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}
