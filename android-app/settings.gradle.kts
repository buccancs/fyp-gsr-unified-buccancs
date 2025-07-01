pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Shimmer repository for GSR sensor SDK - No longer needed, using local files
        // maven { url = uri("https://shimmersensing.jfrog.io/artifactory/ShimmerAPI") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/releases/") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }

        // GitHub Packages repositories for Shimmer SDK - No longer needed, using local files
        // Set these in your local gradle.properties or as environment variables:
        // GITHUB_USERNAME=your_username
        // GITHUB_TOKEN=your_personal_access_token
        // val githubUsername = System.getenv("GITHUB_USERNAME") ?: settings.extra.properties["github.username"] as String? ?: ""
        // val githubToken = System.getenv("GITHUB_TOKEN") ?: settings.extra.properties["github.token"] as String? ?: ""

        // if (githubUsername.isNotEmpty() && githubToken.isNotEmpty()) {
        //     maven {
        //         name = "GitHubPackages"
        //         url = uri("https://maven.pkg.github.com/ShimmerEngineering/ShimmerAndroidAPI")
        //         credentials {
        //             username = githubUsername
        //             password = githubToken
        //         }
        //     }
        //     maven {
        //         name = "GitHubPackages2"
        //         url =
        //             uri("https://maven.pkg.github.com/ShimmerEngineering/Shimmer-Java-Android-API")
        //         credentials {
        //             username = githubUsername
        //             password = githubToken
        //         }
        //     }
        // }

        // Local AAR/JAR files
        flatDir {
            dirs("app/libs")
        }

        // JitPack repository
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "FYP GSR Unified"
include(":app")
