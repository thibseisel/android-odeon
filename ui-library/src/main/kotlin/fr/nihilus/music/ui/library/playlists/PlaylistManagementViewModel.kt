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

package fr.nihilus.music.ui.library.playlists

import android.support.v4.media.MediaBrowserCompat.MediaItem
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.nihilus.music.core.media.MediaId
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
 * A shared ViewModel to handle playlist creation and edition.
 */
@HiltViewModel
internal class PlaylistManagementViewModel @Inject constructor(
    client: BrowserClient,
    private val action: ManagePlaylistAction
) : ViewModel() {

    val userPlaylists: LiveData<LoadRequest<List<MediaItem>>> =
        client.getChildren(MediaId(MediaId.TYPE_PLAYLISTS))
            .map<List<MediaItem>, LoadRequest<List<MediaItem>>> { LoadRequest.Success(it) }
            .onStart { emit(LoadRequest.Pending) }
            .asLiveData()

    fun createPlaylist(playlistName: String, members: List<MediaId>) {
        viewModelScope.launch {
            action.createPlaylist(playlistName, members)
        }
    }

    fun addTracksToPlaylist(
        targetPlaylistId: MediaId,
        addedTrackIds: List<MediaId>
    ) {
        viewModelScope.launch {
            action.appendMembers(targetPlaylistId, addedTrackIds)
        }
    }
}
