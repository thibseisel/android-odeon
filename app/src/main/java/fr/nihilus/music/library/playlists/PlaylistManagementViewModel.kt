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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.nihilus.music.common.media.CustomActions
import fr.nihilus.music.common.media.MediaId
import fr.nihilus.music.core.ui.Event
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.client.BrowserClient
import fr.nihilus.music.core.ui.client.MediaSubscriptionException
import fr.nihilus.music.core.ui.extensions.consumeAsLiveData
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Describe the result of an action performed on a playlist.
 */
sealed class PlaylistActionResult {

    /**
     * A playlist has been successfully created.
     * @property playlistName The name given to the newly created playlist.
     */
    class Created(val playlistName: String) : PlaylistActionResult()

    /**
     * A playlist has been modified by adding some tracks.
     * @property playlistName The name of the playlist that have been modified.
     * @property addedTracksCount The number of tracks that have been added to the playlist.
     */
    class Edited(val playlistName: String, val addedTracksCount: Int) : PlaylistActionResult()
}

/**
 * A shared ViewModel to handle playlist creation and edition.
 */
class PlaylistManagementViewModel
@Inject constructor(
    private val client: BrowserClient
) : ViewModel() {

    private val _playlistActionResult = MutableLiveData<Event<PlaylistActionResult>>()
    val playlistActionResult: LiveData<Event<PlaylistActionResult>> = _playlistActionResult

    val userPlaylists by lazy(LazyThreadSafetyMode.NONE) {
        client.getChildren(MediaId.encode(MediaId.TYPE_PLAYLISTS))
            .map { LoadRequest.Success(it) as LoadRequest<List<MediaBrowserCompat.MediaItem>> }
            .onStart { emit(LoadRequest.Pending) }
            .catch { if (it is MediaSubscriptionException) emit(LoadRequest.Error(it)) }
            .consumeAsLiveData(viewModelScope)
    }

    fun createPlaylist(playlistName: String, members: Array<MediaBrowserCompat.MediaItem>) {
        viewModelScope.launch {
            val membersTrackIds = Array(members.size) { members[it].mediaId }

            val params = Bundle(2).apply {
                putString(CustomActions.EXTRA_TITLE, playlistName)
                putStringArray(CustomActions.EXTRA_MEDIA_IDS, membersTrackIds)
            }

            client.executeAction(CustomActions.ACTION_MANAGE_PLAYLIST, params)
            _playlistActionResult.value = Event(
                PlaylistActionResult.Created(playlistName)
            )
        }
    }
    fun addTracksToPlaylist(
        targetPlaylist: MediaBrowserCompat.MediaItem,
        addedTracks: Array<MediaBrowserCompat.MediaItem>
    ) {
        viewModelScope.launch {
            val playlistId = targetPlaylist.mediaId
            val newTrackMediaIds = Array(addedTracks.size) { addedTracks[it].mediaId }

            val params = Bundle(2).apply {
                putString(CustomActions.EXTRA_PLAYLIST_ID, playlistId)
                putStringArray(CustomActions.EXTRA_MEDIA_IDS, newTrackMediaIds)
            }

            client.executeAction(CustomActions.ACTION_MANAGE_PLAYLIST, params)
            _playlistActionResult.value = Event(
                PlaylistActionResult.Edited(
                    targetPlaylist.description.title?.toString().orEmpty(),
                    addedTracks.size
                )
            )
        }
    }
}