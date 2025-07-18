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
        maven {
            setUrl("https://github.com/jitsi/jitsi-maven-repository/raw/master/releases")
        }
        maven {
            setUrl("https://jitpack.io")
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            setUrl("https://github.com/jitsi/jitsi-maven-repository/raw/master/releases")
        }
        maven {
            setUrl("https://jitpack.io")
        }
    }
}

rootProject.name = "Stranger Voice Call App with Google Authentication"
include(":app")
 