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

package fr.nihilus.music.ui.library.artists.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.nihilus.music.core.media.parse
import fr.nihilus.music.core.ui.client.BrowserClient
import fr.nihilus.music.core.ui.uiStateIn
import fr.nihilus.music.media.AudioTrack
import fr.nihilus.music.media.MediaCategory
import fr.nihilus.music.media.browser.BrowserTree
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
internal class ArtistDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val client: BrowserClient,
    private val browser: BrowserTree,
) : ViewModel() {
    private val artistId =
        ArtistDetailFragmentArgs.fromSavedStateHandle(savedState).artistId.parse()

    val state: StateFlow<ArtistDetailUiState> by lazy {
        combine(getArtistName(), getArtistAlbumsAndTracks()) { artistName, (albums, tracks) ->
            ArtistDetailUiState(
                name = artistName,
                albums = albums,
                tracks = tracks,
                isLoading = false,
            )
        }.uiStateIn(
            viewModelScope,
            initialState = ArtistDetailUiState(
                name = "",
                albums = emptyList(),
                tracks = emptyList(),
                isLoading = true
            )
        )
    }

    fun play(track: ArtistTrackUiState) {
        viewModelScope.launch {
            client.playFromMediaId(track.id)
        }
    }

    private fun getArtistName(): Flow<String> = flow {
        val artist = checkNotNull(browser.getItem(artistId)) {
            "Unable to load the detail of artist $artistId"
        }
        emit(artist.title)
    }

    private fun getArtistAlbumsAndTracks(): Flow<Pair<List<ArtistAlbumUiState>, List<ArtistTrackUiState>>> {
        val children = browser.getChildren(artistId).map { items ->
            val albums = items.filterIsInstance<MediaCategory>().map { it.toUiAlbum() }
            val tracks = items.filterIsInstance<AudioTrack>().map { it.toUiTrack() }
            albums to tracks
        }
        return children
    }

    private fun MediaCategory.toUiAlbum(): ArtistAlbumUiState = ArtistAlbumUiState(
        id = id,
        title = title,
        trackCount = count,
        artworkUri = iconUri,
    )

    private fun AudioTrack.toUiTrack(): ArtistTrackUiState = ArtistTrackUiState(
        id = id,
        title = title,
        duration = duration.milliseconds,
        iconUri = iconUri,
    )
}
