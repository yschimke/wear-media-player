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

import android.content.Context
import android.os.Build
import android.os.StrictMode
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.android.horologist.audio.SystemAudioRepository
import com.google.android.horologist.media3.audio.AudioOutputSelector
import com.google.android.horologist.media3.audio.BluetoothSettingsOutputSelector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ee.schimke.wmp.config.AppConfig
import ee.schimke.wmp.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConfigContainer {
    @Singleton
    @Provides
    @IsEmulator
    fun isEmulator() = Build.PRODUCT.startsWith("sdk_gwear")

    @Singleton
    @Provides
    fun appConfig(): AppConfig = AppConfig()

    @Singleton
    @Provides
    @CacheDir
    fun cacheDir(
        @ApplicationContext application: Context,
    ): File =
        StrictMode.allowThreadDiskWrites().resetAfter {
            application.cacheDir
        }



    @Singleton
    @Provides
    fun prefsDataStore(
        @ApplicationContext application: Context,
        @ForApplicationScope coroutineScope: CoroutineScope
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = null,
            migrations = listOf(),
            scope = coroutineScope
        ) {
            application.preferencesDataStoreFile("prefs")
        }

    @Singleton
    @Provides
    fun settingsRepository(
        prefsDataStore: DataStore<Preferences>
    ) =
        SettingsRepository(prefsDataStore)
}
