/*
 * Copyright 2022 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ee.schimke.wmp.di

import android.content.ComponentName
import android.content.Context
import android.os.Vibrator
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.audio.AudioSink
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.horologist.audio.SystemAudioRepository
import com.google.android.horologist.media.ui.snackbar.SnackbarManager
import com.google.android.horologist.media3.audio.AudioOutputSelector
import com.google.android.horologist.media3.audio.BluetoothSettingsOutputSelector
import com.google.android.horologist.media3.config.WearMedia3Factory
import com.google.android.horologist.media3.logging.ErrorReporter
import com.google.android.horologist.media3.navigation.IntentBuilder
import com.google.android.horologist.media3.navigation.NavDeepLinkIntentBuilder
import com.google.android.horologist.media3.offload.AudioOffloadManager
import com.google.android.horologist.media3.rules.PlaybackRules
import com.google.android.horologist.networks.logging.NetworkStatusLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ee.schimke.wmp.complication.MediaStatusComplicationService
import ee.schimke.wmp.config.AppConfig
import ee.schimke.wmp.service.DataUpdates
import ee.schimke.wmp.system.Logging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaApplicationContainer {

    @Singleton
    @Provides
    fun intentBuilder(
        @ApplicationContext application: Context,
        appConfig: AppConfig,
    ): IntentBuilder =
        NavDeepLinkIntentBuilder(
            application,
            "${appConfig.deeplinkUriPrefix}/player?page=1",
            "${appConfig.deeplinkUriPrefix}/player?page=0"
        )

    @Singleton
    @Provides
    fun playbackRules(
        appConfig: AppConfig,
        @IsEmulator isEmulator: Boolean,
    ): PlaybackRules =
        if (appConfig.playbackRules != null) {
            appConfig.playbackRules
        } else if (isEmulator) {
            PlaybackRules.SpeakerAllowed
        } else {
            PlaybackRules.Normal
        }

    @Singleton
    @Provides
    @ForApplicationScope
    fun coroutineScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Singleton
    @Provides
    fun audioSink(
        appConfig: AppConfig,
        wearMedia3Factory: WearMedia3Factory,
    ): AudioSink =
        wearMedia3Factory.audioSink(
            attemptOffload = appConfig.offloadEnabled,
            offloadMode = appConfig.offloadMode
        )

    @Singleton
    @Provides
    fun wearMedia3Factory(
        @ApplicationContext application: Context,
    ): WearMedia3Factory =
        WearMedia3Factory(application)

    @Singleton
    @Provides
    fun audioOffloadManager(
        logger: ErrorReporter,
    ) = AudioOffloadManager(logger)

    @Singleton
    @Provides
    fun logger(): NetworkStatusLogger = NetworkStatusLogger.Logging

    @Singleton
    @Provides
    fun errorReporter(
        @ApplicationContext application: Context,
    ): ErrorReporter = Logging(application.resources)

    @Singleton
    @Provides
    fun vibrator(
        @ApplicationContext application: Context,
    ): Vibrator =
        application.getSystemService(Vibrator::class.java)

    @Singleton
    @Provides
    fun cacheDatabaseProvider(
        @ApplicationContext application: Context,
    ): DatabaseProvider = StandaloneDatabaseProvider(application)

    @Singleton
    @Provides
    fun downloadCache(
        @CacheDir cacheDir: File,
        cacheDatabaseProvider: DatabaseProvider,
    ): Cache =
        SimpleCache(
            cacheDir.resolve("media3cache"),
            NoOpCacheEvictor(),
            cacheDatabaseProvider
        )

    @Singleton
    @Provides
    fun snackbarManager() =
        SnackbarManager()

    @Singleton
    @Provides
    fun dataUpdates(
        @ApplicationContext application: Context,
    ): DataUpdates {
        val updater = ComplicationDataSourceUpdateRequester.create(
            application,
            ComponentName(
                application, MediaStatusComplicationService::class.java
            )
        )
        return DataUpdates(updater)
    }

    @Singleton
    @Provides
    fun audioOutputSelector(
        systemAudioRepository: SystemAudioRepository
    ): AudioOutputSelector =
        BluetoothSettingsOutputSelector(systemAudioRepository)

    @Singleton
    @Provides
    fun systemAudioRepository(
        @ApplicationContext application: Context
    ): SystemAudioRepository =
        SystemAudioRepository.fromContext(application)
}
