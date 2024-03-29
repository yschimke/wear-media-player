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

import android.app.Service
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.Clock
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.analytics.AnalyticsCollector
import androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.session.BitmapLoader
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaNotification
import coil.ImageLoader
import com.google.android.horologist.audio.SystemAudioRepository
import com.google.android.horologist.media3.WearConfiguredPlayer
import com.google.android.horologist.media3.audio.AudioOutputSelector
import com.google.android.horologist.media3.config.WearMedia3Factory
import com.google.android.horologist.media3.logging.AnalyticsEventLogger
import com.google.android.horologist.media3.logging.ErrorReporter
import com.google.android.horologist.media3.logging.TransferListener
import com.google.android.horologist.media3.navigation.IntentBuilder
import com.google.android.horologist.media3.offload.AudioOffloadManager
import com.google.android.horologist.media3.rules.PlaybackRules
import com.google.android.horologist.networks.data.RequestType
import com.google.android.horologist.networks.okhttp.NetworkAwareCallFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import ee.schimke.wmp.media3.BasicMediaLibrarySessionCallback
import ee.schimke.wmp.media3.CoilBitmapLoader
import ee.schimke.wmp.surfaces.DataUpdates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Provider

@Module
@InstallIn(ServiceComponent::class)
object PlaybackServiceContainer {
    @ServiceScoped
    @Provides
    fun loadControl(): LoadControl = DefaultLoadControl.Builder()
        .setBackBuffer(
            /* backBufferDurationMs = */ 30_000,
            /* retainBackBufferFromKeyframe = */ false
        )
        .build()

    @ServiceScoped
    @Provides
    fun mediaCodecSelector(
        wearMedia3Factory: WearMedia3Factory
    ): MediaCodecSelector = wearMedia3Factory.mediaCodecSelector()

    @ServiceScoped
    @Provides
    fun audioOnlyRenderersFactory(
        wearMedia3Factory: WearMedia3Factory,
        audioSink: AudioSink,
        mediaCodecSelector: MediaCodecSelector
    ) =
        wearMedia3Factory.audioOnlyRenderersFactory(
            audioSink,
            mediaCodecSelector
        )

    @ServiceScoped
    @Provides
    fun defaultAnalyticsCollector(
        logger: ErrorReporter
    ): AnalyticsCollector =
        DefaultAnalyticsCollector(Clock.DEFAULT).apply {
            addListener(AnalyticsEventLogger(logger))
        }

    @ServiceScoped
    @Provides
    fun extractorsFactory(): ExtractorsFactory =
        DefaultExtractorsFactory()

    @ServiceScoped
    @Provides
    fun transferListener(
        logger: ErrorReporter
    ) = TransferListener(logger)

    @ServiceScoped
    @Provides
    fun streamDataSourceFactory(
        networkAwareCallFactory: okhttp3.Call.Factory,
        transferListener: TransferListener
    ): OkHttpDataSource.Factory =
        OkHttpDataSource.Factory(
            NetworkAwareCallFactory(
                networkAwareCallFactory,
                defaultRequestType = RequestType.UnknownRequest
            )
        )
            .setTransferListener(transferListener)

    @ServiceScoped
    @Provides
    fun cacheDataSourceFactory(
        downloadCache: Cache,
        streamDataSourceFactory: OkHttpDataSource.Factory,
        transferListener: TransferListener,
        appConfig: AppConfig
    ): CacheDataSource.Factory =
        CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(streamDataSourceFactory)
            .setEventListener(transferListener)
            .apply {
                if (!appConfig.cacheWriteBack) {
                    setCacheWriteDataSinkFactory(null)
                }
            }

    @ServiceScoped
    @Provides
    fun mediaSourceFactory(
        appConfig: AppConfig,
        cacheDataSourceFactory: CacheDataSource.Factory,
        streamDataSourceFactory: OkHttpDataSource.Factory,
        extractorsFactory: ExtractorsFactory
    ): MediaSource.Factory {
        val dataSourceFactory =
            if (appConfig.cacheItems) {
                cacheDataSourceFactory
            } else {
                streamDataSourceFactory
            }
        return DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)
    }

    @ServiceScoped
    @Provides
    fun exoPlayer(
        service: Service,
        loadControl: LoadControl,
        audioOnlyRenderersFactory: RenderersFactory,
        analyticsCollector: AnalyticsCollector,
        mediaSourceFactory: MediaSource.Factory,
        dataUpdates: DataUpdates,
    ) =
        ExoPlayer.Builder(service, audioOnlyRenderersFactory)
            .setAnalyticsCollector(analyticsCollector)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setLoadControl(loadControl)
            .setSeekForwardIncrementMs(10_000)
            .setSeekBackIncrementMs(10_000)
            .build().apply {
                addListener(analyticsCollector)

                addListener(dataUpdates.listener)
            }

    @ServiceScoped
    @Provides
    fun serviceCoroutineScope(
        service: Service
    ): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default).also {
            (service as LifecycleOwner).lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    it.cancel()
                }
            })
        }
    }

    @ServiceScoped
    @Provides
    fun player(
        exoPlayer: ExoPlayer,
        serviceCoroutineScope: CoroutineScope,
        systemAudioRepository: SystemAudioRepository,
        audioOutputSelector: AudioOutputSelector,
        playbackRules: PlaybackRules,
        logger: ErrorReporter,
        appConfig: AppConfig,
        audioOffloadManager: Provider<AudioOffloadManager>
    ): Player =
        WearConfiguredPlayer(
            player = exoPlayer,
            audioOutputRepository = systemAudioRepository,
            audioOutputSelector = audioOutputSelector,
            playbackRules = playbackRules,
            errorReporter = logger,
            coroutineScope = serviceCoroutineScope
        ).also {
            serviceCoroutineScope.launch {
                it.startNoiseDetection()
            }

            if (appConfig.offloadEnabled) {
                serviceCoroutineScope.launch {
                    audioOffloadManager.get().connect(exoPlayer)
                }
            }
        }

    @ServiceScoped
    @Provides
    fun librarySessionCallback(
        logger: ErrorReporter,
        serviceCoroutineScope: CoroutineScope,
    ): MediaLibrarySession.Callback =
        BasicMediaLibrarySessionCallback(serviceCoroutineScope, logger)

    @ServiceScoped
    @Provides
    fun mediaLibrarySession(
        service: Service,
        player: Player,
        librarySessionCallback: MediaLibrarySession.Callback,
        intentBuilder: IntentBuilder
    ): MediaLibrarySession {
        return MediaLibrarySession.Builder(
            service as MediaLibraryService,
            player,
            librarySessionCallback
        )
            .setSessionActivity(intentBuilder.buildPlayerIntent())
            .build().also {
                (service as LifecycleOwner).lifecycle.addObserver(object :
                    DefaultLifecycleObserver {
                    override fun onDestroy(owner: LifecycleOwner) {
                        it.release()
                    }
                }
                )
            }
    }

    @ServiceScoped
    @Provides
    fun bitmapLoader(
        @ApplicationContext application: Context,
        imageLoader: ImageLoader,
        coroutineScope: CoroutineScope
    ) :BitmapLoader =
        CoilBitmapLoader(imageLoader, application, coroutineScope)

    @ServiceScoped
    @Provides
    fun mediaNotificationProvider(
        @ApplicationContext application: Context,
        bitmapLoader: BitmapLoader
    ) :MediaNotification.Provider =
            DefaultMediaNotificationProvider(application, bitmapLoader)
}
