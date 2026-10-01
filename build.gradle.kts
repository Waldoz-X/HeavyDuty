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
