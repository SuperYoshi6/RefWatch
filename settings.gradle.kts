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
    // Pin AGP to exactly 9.0.0 — Android Studio versions in this project support up to
    // AGP 9.0.0. Newer versions (e.g. 9.2.1) on the plugin portal will be rejected.
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.android.application" || requested.id.id == "com.android.library") {
                useVersion("9.0.0")
            }
        }
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

rootProject.name = "RefWatch"
include(":mobile")
include(":wear")
include(":common")
