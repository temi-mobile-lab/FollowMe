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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // temi SDK(com.robotemi:sdk)는 Maven Central 에 있다 — 별도 저장소 설정이 필요 없다.
        mavenCentral()
    }
}

rootProject.name = "FollowMe"
include(":app")
