/*
 * Copyright 2022 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ee.schimke.wmp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.horologist.media.model.MediaItem
import com.google.android.horologist.media.repository.PlayerRepository
import com.google.android.horologist.networks.data.DataRequestRepository
import com.google.android.horologist.networks.data.DataUsageReport
import com.google.android.horologist.networks.data.Networks
import com.google.android.horologist.networks.status.NetworkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import ee.schimke.wmp.config.AppConfig
import ee.schimke.wmp.settings.Settings
import ee.schimke.wmp.settings.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.io.IOException
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class MediaPlayerAppViewModel @Inject constructor(
    networkRepository: NetworkRepository,
    dataRequestRepository: DataRequestRepository,
    private val settingsRepository: SettingsRepository,
    private val playerRepository: PlayerRepository,
    private val appConfig: AppConfig,
) : ViewModel() {
    val networkStatus: StateFlow<Networks> = networkRepository.networkStatus

    val networkUsage: StateFlow<DataUsageReport?> = dataRequestRepository.currentPeriodUsage()
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds.inWholeMilliseconds),
            initialValue = null
        )

    val settingsState: StateFlow<Settings?> = settingsRepository.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    val deepLinkPrefix: String = appConfig.deeplinkUriPrefix

    val ticker = flow {
        while (true) {
            delay(1.seconds)
            emit(Unit)
        }
    }

    suspend fun loadItems() {
        if (playerRepository.currentMediaItem.value == null) {
            try {
                val mediaItems = listOf(
                    MediaItem(
                        id = "1",
                        uri = "https://npr-ice.streamguys1.com/live.mp3",
                        title = "NPR 24 Hour Program Stream",
                        artist = "NPR",
                        artworkUri = "http://images.radio.orange.com/radios/large_npr_national_public_radio.png"
                    )
                )

                playerRepository.setMediaItems(mediaItems)
                playerRepository.prepare()
            } catch (ioe: IOException) {
                // Nothing
            }
        }
    }

    suspend fun startupSetup() {
        waitForConnection()

        val currentMediaItem = playerRepository.currentMediaItem.value

        if (currentMediaItem == null) {
            loadItems()
        }
    }

    suspend fun playItems(mediaId: String?, collectionId: String) {
        // TODO
        val mediaItems = listOf<MediaItem>().filter {
            it.artist == collectionId
        }

        val index = mediaItems.indexOfFirst { it.id == mediaId }.coerceAtLeast(0)

        waitForConnection()

        playerRepository.setMediaItems(mediaItems)
        playerRepository.prepare()
        playerRepository.play(mediaItemIndex = index)
    }

    private suspend fun waitForConnection() {
        // setMediaItems is a noop before this point
        playerRepository.connected.filter { it }.first()
    }
}
