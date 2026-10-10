plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.heavyduty.core.domain"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Coroutines for Flow in repository interfaces
    implementation(libs.kotlinx.coroutines.core)

    // Testing
    testImplementation(libs.junit)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}
