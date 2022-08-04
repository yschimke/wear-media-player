plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(Google.dagger.hilt.android.gradlePlugin)
    implementation(Android.tools.build.gradlePlugin)
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:_")
}
