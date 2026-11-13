// ============================================================================
//  SETTINGS  —  runs BEFORE any build file. Defines the modules and the
//  repositories everything is downloaded from.
// ============================================================================
pluginManagement {
    repositories {
        google {
            // Only look on Google's server for Google's own artifacts: faster,
            // and stops a typo'd group from silently resolving elsewhere.
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

dependencyResolutionManagement {
    // Forbid modules from declaring their own repositories; all come from here.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "NeuroVerse Auth"
include(":app")
