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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.client.MediaBrowserConnection
import fr.nihilus.music.core.ui.client.MediaSubscriptionException
import fr.nihilus.music.core.ui.extensions.consumeAsLiveData
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

class AlbumDetailViewModel
@Inject constructor(
    private val connection: MediaBrowserConnection
) : ViewModel() {
    private val token = MediaBrowserConnection.ClientToken()
    private var observeTracksJob: Job? = null

    private val _album = MutableLiveData<MediaBrowserCompat.MediaItem>()
    val album: LiveData<MediaBrowserCompat.MediaItem> = _album

    private val _tracks = MutableLiveData<LoadRequest<List<MediaBrowserCompat.MediaItem>>>()
    val tracks: LiveData<LoadRequest<List<MediaBrowserCompat.MediaItem>>> = _tracks

    init {
        connection.connect(token)
    }

    fun setAlbumId(albumId: String) {
        viewModelScope.launch {
            _album.value = connection.getItem(albumId)
        }

        observeTracksJob?.cancel()
        observeTracksJob = connection.subscribe(albumId)
            .map { LoadRequest.Success(it) as LoadRequest<List<MediaBrowserCompat.MediaItem>> }
            .onStart { emit(LoadRequest.Pending) }
            .catch { if (it is MediaSubscriptionException) emit(LoadRequest.Error(it)) }
            .onEach { _tracks.postValue(it) }
            .launchIn(viewModelScope)
    }

    /**
     * The track that the player is currently playing, if any.
     * TODO: would be better to share the position of the track that is currently playing
     */
    val nowPlaying: LiveData<MediaMetadataCompat?> =
        connection.nowPlaying.consumeAsLiveData(viewModelScope)

    fun playAlbum() {
        val albumId = album.value?.mediaId
        if (albumId != null) {
            playMedia(albumId)
        }
    }

    fun playTrack(track: MediaBrowserCompat.MediaItem) {
        playMedia(track.mediaId!!)
    }

    private fun playMedia(mediaId: String) = viewModelScope.launch {
        connection.playFromMediaId(mediaId)
    }

    override fun onCleared() {
        connection.disconnect(token)
        super.onCleared()
    }
}