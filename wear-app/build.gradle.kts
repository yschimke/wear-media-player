plugins {
    `android-app`
}

android {
    namespace = "ee.schimke.wearmediaplayer"

    defaultConfig {
        applicationId = "ee.schimke.wearmediaplayer"
        minSdk = 28
    }
}

setVersionsForApp(AppKind.Wear)

dependencies {
    implementation(AndroidX.wear.compose.material)
    implementation(AndroidX.wear.compose.navigation)
    implementation(AndroidX.wear.compose.foundation)
}
