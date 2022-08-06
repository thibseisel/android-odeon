/*
 * Copyright 2022 Thibault Seisel
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

package fr.nihilus.music.ui.library.playlists.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.parse
import fr.nihilus.music.core.ui.actions.ManagePlaylistAction
import fr.nihilus.music.core.ui.client.BrowserClient
import fr.nihilus.music.core.ui.uiStateIn
import fr.nihilus.music.media.AudioTrack
import fr.nihilus.music.media.browser.BrowserTree
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
internal class PlaylistDetailsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val client: BrowserClient,
    private val browser: BrowserTree,
    private val actions: ManagePlaylistAction
) : ViewModel() {
    private val playlistId =
        PlaylistDetailFragmentArgs.fromSavedStateHandle(savedState).playlistId.parse()

    val state: StateFlow<PlaylistDetailsUiState> by lazy {
        combine(getPlaylistTitle(), getPlaylistTracks()) { title, tracks ->
            PlaylistDetailsUiState(
                playlistTitle = title,
                tracks = tracks,
                isUserDefined = playlistId.type == MediaId.TYPE_PLAYLISTS,
                isLoadingTracks = false
            )
        }.uiStateIn(
            viewModelScope, PlaylistDetailsUiState(
                playlistTitle = "",
                tracks = emptyList(),
                isUserDefined = false,
                isLoadingTracks = true
            )
        )
    }

    fun play(track: PlaylistTrackUiState) {
        viewModelScope.launch {
            client.playFromMediaId(track.id)
        }
    }

    fun deleteThisPlaylist() {
        viewModelScope.launch {
            actions.deletePlaylist(playlistId)
        }
    }

    private fun getPlaylistTitle() = flow<String> {
        val playlist =
            browser.getItem(playlistId) ?: error("Expected playlist $playlistId to exist")
        emit(playlist.title)
    }

    private fun getPlaylistTracks(): Flow<List<PlaylistTrackUiState>> =
        browser.getChildren(playlistId).map { tracks ->
            tracks.filterIsInstance<AudioTrack>().map {
                PlaylistTrackUiState(
                    id = it.id,
                    title = it.title,
                    artistName = it.artist,
                    duration = it.duration.milliseconds,
                    artworkUri = it.iconUri,
                )
            }
        }.catch { emit(emptyList()) }
}
