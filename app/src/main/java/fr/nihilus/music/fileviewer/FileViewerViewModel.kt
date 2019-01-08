/*
 * Copyright 2019 Thibault Seisel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.nihilus.music.fileviewer

import android.arch.lifecycle.LiveData
import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import fr.nihilus.music.base.BaseViewModel
import fr.nihilus.music.client.MediaBrowserConnection
import kotlinx.coroutines.launch
import javax.inject.Inject

class FileViewerViewModel
@Inject constructor(
    private val connection: MediaBrowserConnection
) : BaseViewModel() {
    private val token = MediaBrowserConnection.ClientToken()

    val nowPlaying: LiveData<MediaMetadataCompat?>
        get() = connection.nowPlaying

    val playbackState: LiveData<PlaybackStateCompat>
        get() = connection.playbackState


    init {
        connection.connect(token)
    }

    fun playMediaFromUri(uri: Uri?) {
        launch {
            connection.playFromUri(uri)
        }
    }

    fun seekToPosition(positionMs: Long) {
        launch {
            connection.seekTo(positionMs)
        }
    }

    override fun onCleared() {
        connection.disconnect(token)
        super.onCleared()
    }
}