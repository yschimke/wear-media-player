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

package ee.schimke.wmp.surfaces

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.wear.tiles.ActionBuilders
import androidx.wear.tiles.ActionBuilders.AndroidActivity
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.ResourceBuilders.Resources
import androidx.wear.tiles.TileBuilders.Tile
import coil.ImageLoader
import com.google.android.horologist.compose.tools.TileLayoutPreview
import com.google.android.horologist.compose.tools.WearPreviewDevices
import com.google.android.horologist.compose.tools.WearPreviewFontSizes
import com.google.android.horologist.media.model.Media
import com.google.android.horologist.media.ui.tiles.MediaCollectionsTileRenderer
import com.google.android.horologist.media.ui.tiles.toTileColors
import com.google.android.horologist.tiles.CoroutinesTileService
import com.google.android.horologist.tiles.images.drawableResToImageResource
import com.google.android.horologist.tiles.images.loadImageResource
import com.google.android.horologist.tiles.images.toImageResource
import dagger.hilt.android.AndroidEntryPoint
import ee.schimke.wmp.BuildConfig
import ee.schimke.wmp.R
import ee.schimke.wmp.components.MediaActivity
import ee.schimke.wmp.ui.theme.wearColorPalette
import javax.inject.Inject

/**
 * A Tile with links to open the app, or two specific media collections (playlist, album).
 *
 * Links to the MediaActivity, with extras for collection and optionally mediaId.
 */
@AndroidEntryPoint
class MediaCollectionsTileService : CoroutinesTileService() {
    @Inject
    internal lateinit var imageLoader: ImageLoader

    private val renderer: MediaCollectionsTileRenderer = MediaCollectionsTileRenderer(
        context = this,
        materialTheme = wearColorPalette.toTileColors(),
        debugResourceMode = BuildConfig.DEBUG
    )

    /**
     * Render a Playlist primary button and two chips with direct links to collections.
     */
    override suspend fun tileRequest(requestParams: TileRequest): Tile {
        val (song, album) = loadItems()

        return renderer.renderTimeline(
            state = MediaCollectionsTileRenderer.MediaCollectionsState(
                R.string.tile_playlists_chip,
                appLauncher(),
                MediaCollectionsTileRenderer.MediaCollection(
                    name = song.title,
                    artworkId = song.id,
                    action = appLauncher {
                        addStringExtra(MediaActivity.CollectionKey, song.artist)
                        addStringExtra(MediaActivity.MediaIdKey, song.id)
                    }
                ),
                MediaCollectionsTileRenderer.MediaCollection(
                    name = album.title,
                    artworkId = album.id,
                    action = appLauncher {
                        addStringExtra(MediaActivity.CollectionKey, album.artist)
                    }
                )
            ),
            requestParams = requestParams
        )
    }

    suspend fun loadItems(): Pair<Media, Media> {
        // TODO update

        val first = Media(
            "1",
            "https://example.org/nothing",
            "Title 1",
            "Artist 1",
        )

        val second = Media(
            "1",
            "https://example.org/nothing",
            "Title 1",
            "Artist 1",
        )

        return Pair(first, second)
    }

    private fun AndroidActivity.Builder.addStringExtra(key: String, value: String) {
        addKeyToExtraMapping(
            key,
            ActionBuilders.AndroidStringExtra.Builder()
                .setValue(value)
                .build()
        )
    }

    /**
     * Create a launcher to an activity, with an optional extra "collection" linking to
     * a screen to open.
     */
    private fun appLauncher(
        extrasBuilder: AndroidActivity.Builder.() -> Unit = {}
    ) = ActionBuilders.LaunchAction.Builder()
        .setAndroidActivity(
            AndroidActivity.Builder()
                .setClassName(MediaActivity::class.java.name)
                .setPackageName(this.packageName)
                .apply {
                    extrasBuilder()
                }
                .build()
        )
        .build()

    /**
     * Show UAMP as AppIcon, and favourites and podcasts icons.
     */
    override suspend fun resourcesRequest(requestParams: ResourcesRequest): Resources {
        val (song, album) = loadItems()

        val songResource = imageLoader.loadImageResource(this, song.artworkUri)
        val albumResource = imageLoader.loadImageResource(this, album.artworkUri)

        return renderer.produceRequestedResources(
            MediaCollectionsTileRenderer.ResourceState(
                R.drawable.baseline_radio_24,
                mapOf(
                    song.id to songResource,
                    album.id to albumResource
                )
            ),
            requestParams
        )
    }
}

@WearPreviewDevices
@WearPreviewFontSizes
@Composable
fun SampleTilePreview() {
    val context = LocalContext.current

    val action = ActionBuilders.LaunchAction.Builder()
        .build()

    val tileState = remember {
        MediaCollectionsTileRenderer.MediaCollectionsState(
            chipName = R.string.preview_channels,
            chipAction = action,
            collection1 = MediaCollectionsTileRenderer.MediaCollection(
                name = context.getString(R.string.preview_channel1),
                artworkId = "s1",
                action = action
            ),
            collection2 = MediaCollectionsTileRenderer.MediaCollection(
                name = context.getString(R.string.preview_channel2),
                artworkId = "c2",
                action = action
            ),
        )
    }

    val resourceState = remember {
        val kyoto = BitmapFactory.decodeResource(context.resources, R.drawable.baseline_radio_24)

        MediaCollectionsTileRenderer.ResourceState(
            appIcon = R.drawable.baseline_radio_24,
            images = mapOf(
                "s1" to kyoto.toImageResource(),
                "c2" to drawableResToImageResource(R.drawable.baseline_radio_24)
            )
        )
    }

    val renderer = remember {
        MediaCollectionsTileRenderer(
            context = context,
            materialTheme = wearColorPalette.toTileColors(),
            debugResourceMode = BuildConfig.DEBUG
        )
    }

    TileLayoutPreview(
        tileState,
        resourceState,
        renderer
    )
}
