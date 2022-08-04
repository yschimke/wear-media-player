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

package ee.schimke.wmp.components

import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.android.horologist.media3.service.LifecycleMediaLibraryService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : LifecycleMediaLibraryService() {
    @Inject
    public override lateinit var mediaLibrarySession: MediaLibrarySession

    @Inject
    public lateinit var _mediaNotificationProvider: MediaNotification.Provider

    override fun onCreate() {
        super.onCreate()

        setMediaNotificationProvider(_mediaNotificationProvider)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }
}
