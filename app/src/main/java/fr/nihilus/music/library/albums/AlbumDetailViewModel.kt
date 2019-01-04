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

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import fr.nihilus.music.base.BaseViewModel
import fr.nihilus.music.client.MediaBrowserConnection
import fr.nihilus.music.ui.LoadRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import javax.inject.Inject

class AlbumDetailViewModel
@Inject constructor(
    private val connection: MediaBrowserConnection
) : BaseViewModel() {
    private val token = MediaBrowserConnection.ClientToken()
    private var loadTracksJob: Job? = null

    private val _albumTracks = MutableLiveData<LoadRequest<List<MediaBrowserCompat.MediaItem>>>()
    val albumTracks: LiveData<LoadRequest<List<MediaBrowserCompat.MediaItem>>>
        get() = _albumTracks

    init {
        connection.connect(token)
    }

    fun loadTracksOfAlbum(album: MediaBrowserCompat.MediaItem) {
        loadTracksJob?.cancel()
        _albumTracks.postValue(LoadRequest.Pending())

        loadTracksJob = launch {
            connection.subscribe(album.mediaId!!).consumeEach { tracksUpdate ->
                _albumTracks.postValue(LoadRequest.Success(tracksUpdate))
            }
        }
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