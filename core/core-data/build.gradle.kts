plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

android {
    namespace = "com.example.heavyduty.core.data"
    compileSdk {
        version = release(37)
    }

    testOptions {
        // android.util.Log in code under test (the Health Connect sync logs its failures).
        unitTests.isReturnDefaultValues = true
    }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // java.time (Health Connect's Instant and ZoneOffset) on API 24-25.
        isCoreLibraryDesugaringEnabled = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Project modules
    implementation(project(":core:core-common"))
    implementation(project(":core:core-domain"))

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // JSON
    implementation(libs.gson)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Health Connect
    implementation(libs.health.connect.client)

    // Testing
    testImplementation(libs.junit)
    testImplementation(project(":core:core-testing"))

    // Instrumented tests (real SQLite on a device/emulator): migrations and backups
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.coroutines.test)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}
