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
    }
}

rootProject.name = "homeflix-android-tv"

include(
    ":app",
    ":core:catalog",
    ":core:designsystem",
    ":core:network",
    ":core:session",
    ":feature:auth",
    ":feature:detail",
    ":feature:home",
    ":feature:library",
    ":feature:player",
    ":feature:profile",
)
