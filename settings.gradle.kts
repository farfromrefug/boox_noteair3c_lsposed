pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
        maven("https://api.xposed.info/")
//        mavenLocal {
//            content {
//                includeGroup("io.github.libxposed")
//            }
//        }
    }
    versionCatalogs {
        create("libs")
    }
}

rootProject.name = "ScreenOffBiometrics"
include(":app")