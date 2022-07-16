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

import android.support.v4.media.MediaBrowserCompat.MediaItem
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.nihilus.music.core.media.MediaItems
import fr.nihilus.music.core.media.parse
import fr.nihilus.music.core.ui.client.BrowserClient
import fr.nihilus.music.core.ui.uiStateIn
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
internal class ArtistDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val client: BrowserClient
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
        val artist = checkNotNull(client.getItem(artistId)) {
            "Unable to load the detail of artist $artistId"
        }
        emit(artist.description.title?.toString() ?: "")
    }

    private fun getArtistAlbumsAndTracks(): Flow<Pair<List<ArtistAlbumUiState>, List<ArtistTrackUiState>>> {
        val children = client.getChildren(artistId).map { items ->
            val (albumItems, trackItems) = items.partition { it.isBrowsable }
            val albums = albumItems.map { it.toUiAlbum() }
            val tracks = trackItems.map { it.toUiTrack() }
            albums to tracks
        }
        return children
    }

    private fun MediaItem.toUiAlbum(): ArtistAlbumUiState = ArtistAlbumUiState(
        id = mediaId.parse(),
        title = description.title?.toString() ?: "",
        trackCount = description.extras!!.getInt(MediaItems.EXTRA_NUMBER_OF_TRACKS),
        artworkUri = description.iconUri,
    )

    private fun MediaItem.toUiTrack(): ArtistTrackUiState = ArtistTrackUiState(
        id = mediaId.parse(),
        title = description.title?.toString() ?: "",
        duration = description.extras!!.getLong(MediaItems.EXTRA_DURATION).milliseconds,
        iconUri = description.iconUri,
    )
}
