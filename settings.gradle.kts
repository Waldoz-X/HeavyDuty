pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "HeavyDuty"

// App module
include(":app")

// Core modules
include(":core:core-common")
include(":core:core-domain")
include(":core:core-ui")
include(":core:core-data")

// Feature modules
include(":feature:feature-exercises")
include(":feature:feature-routines")
include(":feature:feature-workout")
include(":feature:feature-history")
include(":feature:feature-timer")
include(":feature:feature-calendar")
include(":feature:feature-dashboard")
