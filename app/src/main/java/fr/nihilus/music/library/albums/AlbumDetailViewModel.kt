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

package fr.nihilus.music.library.albums

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.lifecycle.LiveData
import fr.nihilus.music.base.BrowsableContentViewModel
import fr.nihilus.music.client.MediaBrowserConnection
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

class AlbumDetailViewModel
@Inject constructor(
    private val connection: MediaBrowserConnection
) : BrowsableContentViewModel(connection) {
    private val token = MediaBrowserConnection.ClientToken()
    private var observeTracksJob: Job? = null

    init {
        connection.connect(token)
    }

    fun loadTracksOfAlbum(album: MediaBrowserCompat.MediaItem) {
        observeTracksJob?.cancel()
        observeTracksJob = observeChildren(album.mediaId!!)
    }

    /**
     * The track that the player is currently playing, if any.
     * TODO: would be better to share the position of the track that is currently playing
     */
    val nowPlaying: LiveData<MediaMetadataCompat?>
        get() = connection.nowPlaying

    fun playMedia(item: MediaBrowserCompat.MediaItem) {
        launch {
            connection.playFromMediaId(item.mediaId!!)
        }
    }

    override fun onCleared() {
        connection.disconnect(token)
        super.onCleared()
    }
}