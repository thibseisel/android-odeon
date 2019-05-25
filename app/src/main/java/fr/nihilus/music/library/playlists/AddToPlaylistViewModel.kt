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

package fr.nihilus.music.library.playlists

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import fr.nihilus.music.base.BaseViewModel
import fr.nihilus.music.client.MediaBrowserConnection
import fr.nihilus.music.media.MediaId
import fr.nihilus.music.media.actions.CustomActions
import fr.nihilus.music.ui.Event
import fr.nihilus.music.ui.LoadRequest
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PlaylistEditionResult {
    data class Success(val trackCount: Int, val playlistTitle: CharSequence?) : PlaylistEditionResult()
    data class AlreadyTaken(val requestedPlaylistName: String) : PlaylistEditionResult()
}

class AddToPlaylistViewModel
@Inject constructor(
    private val connection: MediaBrowserConnection
) : BaseViewModel() {

    private val _targetPlaylists = MutableLiveData<LoadRequest<List<MediaBrowserCompat.MediaItem>>>()
    val targetPlaylists: LiveData<LoadRequest<List<MediaBrowserCompat.MediaItem>>>
        get() = _targetPlaylists

    private val _playlistUpdateResult = MutableLiveData<Event<PlaylistEditionResult>>()
    val playlistUpdateResult: LiveData<Event<PlaylistEditionResult>>
        get() = _playlistUpdateResult

    init {
        launch {
            _targetPlaylists.postValue(LoadRequest.Pending)
            connection.subscribe(MediaId.ALL_PLAYLISTS).consumeEach { playlistUpdates ->
                _targetPlaylists.postValue(LoadRequest.Success(playlistUpdates))
            }
        }
    }

    fun addTracksToPlaylist(
        targetPlaylist: MediaBrowserCompat.MediaItem,
        addedTracks: Array<MediaBrowserCompat.MediaItem>
    ) {
        launch {
            val playlistId = targetPlaylist.mediaId
            val newTrackMediaIds = Array(addedTracks.size) { addedTracks[it].mediaId }

            val params = Bundle(2).apply {
                putString(CustomActions.EXTRA_PLAYLIST_ID, playlistId)
                putStringArray(CustomActions.EXTRA_MEDIA_IDS, newTrackMediaIds)
            }

            connection.executeAction(CustomActions.ACTION_MANAGE_PLAYLIST, params)
            _playlistUpdateResult.value = Event(
                PlaylistEditionResult.Success(
                    addedTracks.size,
                    targetPlaylist.description.title
                )
            )
        }
    }
}
