plugins {
    `android-app`

    id("com.google.dagger.hilt.android")
}

android {
    namespace = "ee.schimke.wmp"

    buildTypes {
        debug {
            manifestPlaceholders["schemeSuffix"] = "-debug"
        }
        release {
            manifestPlaceholders["schemeSuffix"] = ""
        }
    }

    defaultConfig {
        applicationId = "ee.schimke.wmp"
        minSdk = 28
    }

    lintOptions {
        disable("UnsafeOptInUsageError")
    }
}

val experimentalAnnotations = """
    androidx.media3.common.util.UnstableApi
    com.google.android.horologist.audio.ExperimentalHorologistAudioApi
    com.google.android.horologist.audio.ui.ExperimentalHorologistAudioUiApi
    com.google.android.horologist.composables.ExperimentalHorologistComposablesApi
    com.google.android.horologist.compose.tools.ExperimentalHorologistComposeToolsApi
    com.google.android.horologist.media.ExperimentalHorologistMediaApi
    com.google.android.horologist.media.ui.ExperimentalHorologistMediaUiApi
    com.google.android.horologist.media3.ExperimentalHorologistMedia3BackendApi
    com.google.android.horologist.networks.ExperimentalHorologistNetworksApi
    com.google.android.horologist.tiles.ExperimentalHorologistTilesApi
    kotlinx.coroutines.ExperimentalCoroutinesApi
    com.google.accompanist.pager.ExperimentalPagerApi
""".trimIndent().split("\n")

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    experimentalAnnotations.forEach {
        kotlinOptions.freeCompilerArgs += "-opt-in=$it"
    }
}

dependencies {
    implementation(AndroidX.wear.compose.material)
    implementation(AndroidX.wear.compose.navigation)
    implementation(AndroidX.wear.compose.foundation)

    implementation(Square.okHttp3.okHttp)
    implementation(Square.okHttp3.loggingInterceptor)
    implementation(Square.retrofit2)
    implementation(Square.retrofit2.converter.moshi)
    implementation(Square.moshi)
    implementation(AndroidX.dataStore.core)
    implementation(AndroidX.dataStore.preferences)
    implementation(AndroidX.media3.common)
    implementation(AndroidX.media3.exoPlayer)
    implementation(AndroidX.media3.dataSource.okhttp)
    implementation(AndroidX.wear.watchFace.complications.dataSourceKtx)
    implementation(AndroidX.wear.tiles.material)
    implementation(AndroidX.compose.material.icons.core)
    implementation(AndroidX.compose.material.icons.extended)
    implementation(COIL.compose)
    implementation(COIL.svg)
    implementation(AndroidX.hilt.navigationCompose)
    implementation(KotlinX.coroutines.guava)

    implementation("com.google.android.horologist:horologist-media:_")
    implementation("com.google.android.horologist:horologist-media-ui:_")
    implementation("com.google.android.horologist:horologist-media-data:_")
    implementation("com.google.android.horologist:horologist-media3-backend:_")
    implementation("com.google.android.horologist:horologist-composables:_")
    implementation("com.google.android.horologist:horologist-compose-layout:_")
    implementation("com.google.android.horologist:horologist-network-awareness:_")
    implementation("com.google.android.horologist:horologist-audio:_")
    implementation("com.google.android.horologist:horologist-audio-ui:_")
    implementation("com.google.android.horologist:horologist-tiles:_")

    debugImplementation("com.google.android.horologist:horologist-compose-tools:_")

    kapt(Google.dagger.compiler.withVersionPlaceholder())
    kapt(Google.dagger.hilt.compiler.withVersionPlaceholder())
    implementation(Google.dagger.hilt.android)
}
