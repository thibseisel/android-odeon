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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.ui.actions.ManagePlaylistAction
import fr.nihilus.music.core.ui.uiStateIn
import fr.nihilus.music.media.browser.BrowserTree
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A shared ViewModel to handle playlist creation and edition.
 */
@HiltViewModel
internal class PlaylistManagementViewModel @Inject constructor(
    private val browser: BrowserTree,
    private val action: ManagePlaylistAction
) : ViewModel() {

    /**
     * Live UI state of the "add to playlist" dialog.
     */
    val state: StateFlow<PlaylistDialogUiState> by lazy {
        browser.getChildren(MediaId(MediaId.TYPE_PLAYLISTS))
            .map { playlists ->
                PlaylistDialogUiState(
                    playlists = playlists.map {
                        PlaylistDialogUiState.Playlist(
                            id = it.id,
                            title = it.title,
                            iconUri = it.iconUri,
                        )
                    }
                )
            }
            .uiStateIn(
                viewModelScope,
                initialState = PlaylistDialogUiState(
                    playlists = emptyList(),
                )
            )
    }

    /**
     * Creates a new playlist.
     *
     * @param playlistName Name given to the newly created playlist by the user.
     * Should not be empty.
     * @param members Tracks to be initially added to the new playlist.
     */
    fun createPlaylist(playlistName: String, members: List<MediaId>) {
        viewModelScope.launch {
            action.createPlaylist(playlistName, members)
        }
    }

    /**
     * Add tracks to an existing playlist.
     *
     * @param targetPlaylistId Identifier of an existing playlist to which tracks should be appended.
     * @param addedTrackIds Identifiers of tracks to be added to the playlist.
     */
    fun addTracksToPlaylist(
        targetPlaylistId: MediaId,
        addedTrackIds: List<MediaId>
    ) {
        viewModelScope.launch {
            action.appendMembers(targetPlaylistId, addedTrackIds)
        }
    }
}
