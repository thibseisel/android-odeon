/*
 * Copyright 2020 Thibault Seisel
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

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.parse
import fr.nihilus.music.core.ui.Event
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.actions.ManagePlaylistAction
import fr.nihilus.music.core.ui.client.BrowserClient
import fr.nihilus.music.core.ui.client.MediaSubscriptionException
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
@HiltViewModel
internal class PlaylistManagementViewModel @Inject constructor(
    client: BrowserClient,
    private val action: ManagePlaylistAction
) : ViewModel() {

    private val _playlistActionResult = MutableLiveData<Event<PlaylistActionResult>>()
    val playlistActionResult: LiveData<Event<PlaylistActionResult>> = _playlistActionResult

    val userPlaylists: LiveData<LoadRequest<List<MediaItem>>> =
        client.getChildren(MediaId(MediaId.TYPE_PLAYLISTS))
            .map<List<MediaItem>, LoadRequest<List<MediaItem>>> { LoadRequest.Success(it) }
            .onStart { emit(LoadRequest.Pending) }
            .catch { if (it is MediaSubscriptionException) emit(LoadRequest.Error(it)) }
            .asLiveData()

    fun createPlaylist(playlistName: String, members: Array<MediaItem>) {
        viewModelScope.launch {
            val membersTrackIds = members.map { it.mediaId.parse() }

            action.createPlaylist(playlistName, membersTrackIds)
            _playlistActionResult.value = Event(
                PlaylistActionResult.Created(playlistName)
            )
        }
    }

    fun addTracksToPlaylist(
        targetPlaylist: MediaItem,
        addedTracks: Array<MediaItem>
    ) {
        viewModelScope.launch {
            val playlistId = targetPlaylist.mediaId.parse()
            val newTrackMediaIds = addedTracks.map { it.mediaId.parse() }

            action.appendMembers(playlistId, newTrackMediaIds)
            _playlistActionResult.value = Event(
                PlaylistActionResult.Edited(
                    targetPlaylist.description.title?.toString().orEmpty(),
                    addedTracks.size
                )
            )
        }
    }
}