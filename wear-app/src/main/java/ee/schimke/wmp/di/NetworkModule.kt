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
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.google.android.horologist.networks.data.DataRequestRepository
import com.google.android.horologist.networks.data.RequestType
import com.google.android.horologist.networks.logging.NetworkStatusLogger
import com.google.android.horologist.networks.okhttp.NetworkAwareCallFactory
import com.google.android.horologist.networks.okhttp.NetworkSelectingCallFactory
import com.google.android.horologist.networks.rules.NetworkingRules
import com.google.android.horologist.networks.rules.NetworkingRulesEngine
import com.google.android.horologist.networks.status.HighBandwidthRequester
import com.google.android.horologist.networks.status.NetworkRepository
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ee.schimke.wmp.BuildConfig
import kotlinx.coroutines.CoroutineScope
import okhttp3.Cache
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.LoggingEventListener
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
    @Provides
    fun networkRepository(
        @ApplicationContext application: Context,
        @ForApplicationScope coroutineScope: CoroutineScope,
        networkLogger: NetworkStatusLogger,
    ): NetworkRepository = NetworkRepository.fromContext(application, coroutineScope, networkLogger)

    @Singleton
    @Provides
    fun cache(
        @ApplicationContext application: Context,
    ): Cache = Cache(application.cacheDir.resolve("HttpCache"), 10_000_000)

    @Singleton
    @Provides
    fun okhttpClient(
        cache: Cache,
    ): OkHttpClient {
        val alwaysHttpsInterceptor = Interceptor {
            var request = it.request()

            println("url1: " + request.url)

            if (request.url.scheme == "http") {
                request = request.newBuilder().url(request.url.newBuilder().scheme("https").build())
                    .build()
            }

            request = request.newBuilder()
                .header("User-Agent",
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:94.0) Gecko/20100101 Firefox/94.0")
                .build()

            println("url2: " + request.url)

            it.proceed(request)
        }
        return OkHttpClient.Builder().followSslRedirects(false)
            .addInterceptor(alwaysHttpsInterceptor)
            .eventListenerFactory(LoggingEventListener.Factory())
            .cache(cache)
            .build()
    }

    @Provides
    fun networkingRules(
        appConfig: AppConfig,
    ): NetworkingRules = appConfig.strictNetworking!!

    @Singleton
    @Provides
    fun dataRequestRepository(): DataRequestRepository =
        DataRequestRepository.InMemoryDataRequestRepository

    @Provides
    fun networkingRulesEngine(
        networkRepository: NetworkRepository,
        networkLogger: NetworkStatusLogger,
        networkingRules: NetworkingRules,
    ): NetworkingRulesEngine = NetworkingRulesEngine(networkRepository = networkRepository,
        logger = networkLogger,
        networkingRules = networkingRules)

    @Provides
    fun connectivityManager(
        @ApplicationContext application: Context,
    ): ConnectivityManager =
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Provides
    fun wifiManager(
        @ApplicationContext application: Context,
    ): WifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager

    @Singleton
    @Provides
    fun highBandwidthRequester(
        connectivityManager: ConnectivityManager,
        @ForApplicationScope coroutineScope: CoroutineScope,
        networkLogger: NetworkStatusLogger,
    ) = HighBandwidthRequester(connectivityManager, coroutineScope, networkLogger)

    @Singleton
    @Provides
    fun networkAwareCallFactory(
        appConfig: AppConfig,
        okhttpClient: OkHttpClient,
        networkingRulesEngine: NetworkingRulesEngine,
        highBandwidthRequester: HighBandwidthRequester,
        dataRequestRepository: DataRequestRepository,
    ): Call.Factory = if (appConfig.strictNetworking != null) {
        NetworkSelectingCallFactory(networkingRulesEngine,
            highBandwidthRequester,
            dataRequestRepository,
            okhttpClient)
    } else {
        okhttpClient
    }

    @Singleton
    @Provides
    fun moshi() = Moshi.Builder().build()

    @Singleton
    @Provides
    fun retrofit(
        callFactory: Call.Factory,
        moshi: Moshi,
    ) = Retrofit.Builder().addConverterFactory(MoshiConverterFactory.create(moshi))
        .callFactory(NetworkAwareCallFactory(callFactory, RequestType.ApiRequest)).build()

    @Singleton
    @Provides
    fun imageLoader(
        @ApplicationContext application: Context,
        @CacheDir cacheDir: File,
        callFactory: Call.Factory,
    ): ImageLoader = ImageLoader.Builder(application).crossfade(false).components {
        add(SvgDecoder.Factory())
    }.respectCacheHeaders(false).diskCache {
        DiskCache.Builder().directory(cacheDir.resolve("image_cache")).build()
    }.memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED)
        .networkCachePolicy(CachePolicy.ENABLED).callFactory {
            NetworkAwareCallFactory(callFactory, defaultRequestType = RequestType.ImageRequest)
        }.apply {
            if (BuildConfig.DEBUG) {
                logger(DebugLogger())
            }
        }.build()
}
