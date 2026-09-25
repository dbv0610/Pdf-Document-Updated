pluginManagement {
 val mavenUser: String by settings
    val mavenPassword: String by settings
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
dependencyResolutionManagement {
    val mavenUser: String by settings
    val mavenPassword: String by settings
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
        maven(url = "https://artifact.bytedance.com/repository/pangle/")
        maven(url = "https://repository.aspose.com/repo/")
        maven(url = "https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea")

    }
}

rootProject.name = "AS005 PDF 8"
include(":app")
include(":lib")
include(":android_office")

include(":ucrop")
include(":android_office")
