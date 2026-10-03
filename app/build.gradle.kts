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

android {
    namespace = "com.example.heavyduty"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.heavyduty"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

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
        release {
            // R8: shrinks, optimizes and obfuscates code and drops unused resources.
            optimization {
                enable = true
                keepRules {
                    files.add(file("proguard-rules.pro"))
                }
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
    implementation(libs.androidx.compose.ui.tooling.preview)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Testing
    testImplementation(libs.junit)
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
