import de.fayard.refreshVersions.core.versionFor
import org.gradle.api.JavaVersion
import org.gradle.kotlin.dsl.implementation
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("android")
    id("com.android.application")
}

android {
    compileSdk = 32
    defaultConfig {
        minSdk = 23
        targetSdk = 32
    }
    buildTypes {
        getByName("release").isMinifyEnabled = true
        getByName("release").isShrinkResources = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures.compose = true
    composeOptions {
        kotlinCompilerExtensionVersion = versionFor(AndroidX.compose.compiler)
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    kotlinOptions.jvmTarget = "1.8"
}

dependencies {
    implementation(AndroidX.activity.compose)

    implementation(AndroidX.compose.runtime)
    implementation(AndroidX.compose.foundation)
}
