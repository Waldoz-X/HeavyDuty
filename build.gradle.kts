// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.baselineprofile) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
}

// Formatting for the whole repo: ./gradlew spotlessApply fixes it, spotlessCheck fails the build
// when something isn't formatted. Rules come from ktlint (+ Compose rules) and .editorconfig.
spotless {
    kotlin {
        target("app/src/**/*.kt", "baselineprofile/src/**/*.kt", "core/*/src/**/*.kt", "feature/*/src/**/*.kt")
        ktlint(libs.versions.ktlint.get())
            .setEditorConfigPath(rootProject.file(".editorconfig"))
            .editorConfigOverride(mapOf("max_line_length" to 140, "ktlint_standard_no-unused-imports" to "enabled"))
            .customRuleSets(listOf("io.nlopez.compose.rules:ktlint:${libs.versions.composeRules.get()}"))
    }
    kotlinGradle {
        target("*.kts", "app/*.kts", "baselineprofile/*.kts", "core/*/*.kts", "feature/*/*.kts")
        ktlint(libs.versions.ktlint.get())
            .setEditorConfigPath(rootProject.file(".editorconfig"))
            .editorConfigOverride(mapOf("max_line_length" to 140, "ktlint_standard_no-unused-imports" to "enabled"))
    }
}

// Compose compiler setup shared by every module that uses Compose.
subprojects {
    plugins.withId("org.jetbrains.kotlin.plugin.compose") {
        extensions.configure<org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension> {
            // core-domain has no Compose compiler, so its (immutable) models would count as
            // unstable and every composable taking one would recompose on each new emission.
            stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("compose_stability.conf"))

            // ./gradlew assembleRelease -PcomposeReports=true → build/compose_compiler/ per module.
            if (providers.gradleProperty("composeReports").orNull == "true") {
                reportsDestination.set(layout.buildDirectory.dir("compose_compiler"))
                metricsDestination.set(layout.buildDirectory.dir("compose_compiler"))
            }
        }
    }
}

// Static analysis for the whole repo: ./gradlew detekt. Findings that predate it are recorded in
// config/detekt/baseline.xml so the build stays green while new code is held to the rules; the
// baseline shrinks as that code is cleaned up (regenerate with ./gradlew detektBaseline).
detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    baseline = rootProject.file("config/detekt/baseline.xml")
    parallel = true
    source.setFrom(
        fileTree(rootDir) {
            include("app/src/main/**/*.kt", "core/*/src/main/**/*.kt", "feature/*/src/main/**/*.kt")
        }
    )
}

dependencies {
    detektPlugins("io.nlopez.compose.rules:detekt:${libs.versions.composeRules.get()}")
}
