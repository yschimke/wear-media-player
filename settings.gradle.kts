pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // See https://jmfayard.github.io/refreshVersions
    id("de.fayard.refreshVersions") version "0.40.2"
}

run {
    val rootProjectPropertiesFile = rootDir.resolve("gradle.properties")
    val buildSrcPropertiesFile = rootDir.resolve("buildSrc").resolve("gradle.properties")
    if (buildSrcPropertiesFile.exists().not() ||
        rootProjectPropertiesFile.readText() != buildSrcPropertiesFile.readText()
    ) {
        rootProjectPropertiesFile.copyTo(target = buildSrcPropertiesFile, overwrite = true)
    }
}

rootProject.name = "WearMediaApp"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include("wear-app")
