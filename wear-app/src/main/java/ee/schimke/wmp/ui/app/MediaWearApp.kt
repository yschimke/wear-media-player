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

@file:OptIn(ExperimentalPagerApi::class)

package ee.schimke.wmp.ui.app

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.android.horologist.compose.layout.StateUtils.rememberStateWithLifecycle
import com.google.android.horologist.media.ui.navigation.MediaNavController.navigateToCollections
import com.google.android.horologist.media.ui.navigation.MediaNavController.navigateToSettings
import com.google.android.horologist.media.ui.navigation.MediaNavController.navigateToVolume
import com.google.android.horologist.media.ui.navigation.MediaPlayerScaffold
import com.google.android.horologist.media.ui.screens.browse.BrowseScreen
import com.google.android.horologist.media.ui.screens.browse.BrowseScreenState
import ee.schimke.wmp.components.MediaActivity
import ee.schimke.wmp.ui.theme.WearMediaPlayerTheme
import ee.schimke.wmp.ui.player.MediaPlayerScreen
import ee.schimke.wmp.ui.settings.SettingsScreen

@Composable
fun MediaWearApp(
    navController: NavHostController,
    intent: Intent
) {
    val appViewModel: MediaPlayerAppViewModel = hiltViewModel()
    val settingsState by rememberStateWithLifecycle(flow = appViewModel.settingsState)

    val volumeViewModel: VolumeViewModel = hiltViewModel()

    val timeText: @Composable (Modifier) -> Unit = { modifier ->
        TimeText(modifier = modifier)
    }

    WearMediaPlayerTheme {
        MediaPlayerScaffold(
            playerScreen = { focusRequester ->
                MediaPlayerScreen(
                    modifier = Modifier.fillMaxSize(),
                    mediaPlayerScreenViewModel = hiltViewModel(),
                    volumeViewModel = volumeViewModel,
                    onVolumeClick = {
                        navController.navigateToVolume()
                    },
                    playerFocusRequester = focusRequester,
                    settingsState = settingsState
                )
            },
            libraryScreen = { focusRequester, scalingLazyListState ->
                BrowseScreen(
                    browseScreenState = BrowseScreenState.Loaded(emptyList()),
                    onDownloadItemClick = { },
                    onPlaylistsClick = { navController.navigateToCollections() },
                    onSettingsClick = { navController.navigateToSettings() },
                    focusRequester = focusRequester,
                    scalingLazyListState = scalingLazyListState,
                )
            },
            categoryEntityScreen = { _, _, _, _ ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Category XXX")
                }
            },
            mediaEntityScreen = { _, _ ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Media XXX")
                }
            },
            playlistsScreen = { _, _ ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Media XXX")
                }
            },
            settingsScreen = { focusRequester, state ->
                SettingsScreen(
                    focusRequester = focusRequester,
                    state = state,
                    settingsScreenViewModel = hiltViewModel()
                )
            },
            snackbarViewModel = hiltViewModel<SnackbarViewModel>(),
            volumeViewModel = hiltViewModel<VolumeViewModel>(),
            timeText = timeText,
            deepLinkPrefix = appViewModel.deepLinkPrefix,
            navController = navController
        )
    }

    LaunchedEffect(Unit) {
        val collectionId = intent.getAndRemoveKey(MediaActivity.CollectionKey)
        val mediaId = intent.getAndRemoveKey(MediaActivity.MediaIdKey)

        if (collectionId != null) {
            appViewModel.playItems(mediaId, collectionId)
        } else {
            appViewModel.startupSetup()
        }
    }
}

private fun Intent.getAndRemoveKey(key: String): String? =
    getStringExtra(key).also {
        removeExtra(key)
    }
