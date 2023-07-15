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

package fr.nihilus.music.ui.library.albums

import android.support.v4.media.MediaMetadataCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.nihilus.music.core.media.MediaId
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
internal class AlbumDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val client: BrowserClient,
    private val browser: BrowserTree,
) : ViewModel() {
    private val albumId: MediaId

    init {
        val navArgs = AlbumDetailFragmentArgs.fromSavedStateHandle(savedState)
        this.albumId = navArgs.albumId.parse()
    }

    /**
     * Live UI state describing detail of the viewed album with its tracks.
     */
    val state: StateFlow<AlbumDetailUiState> = combine(
        albumFlow(),
        getAlbumTracks(),
        client.nowPlaying,
    ) { album, tracks, nowPlaying ->
        val currentlyPlayingMediaId =
            nowPlaying?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)?.parse()

        AlbumDetailUiState(
            id = album.id,
            title = album.title,
            subtitle = album.subtitle.orEmpty(),
            artworkUri = album.iconUri,
            isLoading = true,
            tracks = tracks.map { track ->
                AlbumDetailUiState.Track(
                    id = track.id,
                    title = track.title,
                    number = track.number,
                    duration = track.duration.milliseconds,
                    isPlaying = track.id == currentlyPlayingMediaId
                )
            },
        )
    }.uiStateIn(
        viewModelScope, initialState = AlbumDetailUiState(
            id = albumId,
            title = "",
            subtitle = "",
            artworkUri = null,
            tracks = emptyList(),
            isLoading = true,
        )
    )

    private fun getAlbumTracks(): Flow<List<AudioTrack>> =
        browser.getChildren(albumId).map { it.filterIsInstance<AudioTrack>() }

    private fun albumFlow(): Flow<MediaCategory> = flow {
        val album = browser.getItem(albumId) as? MediaCategory
        checkNotNull(album) { "Expected album $albumId to be a browsable media" }
        emit(album)
    }

    /**
     * Start playback of all tracks from the currently displayed album.
     */
    fun playAlbum() {
        playMedia(albumId)
    }

    /**
     * Start playback of a given track in the album.
     *
     * @param track The track to be played.
     */
    fun playTrack(track: AlbumDetailUiState.Track) {
        playMedia(track.id)
    }

    private fun playMedia(mediaId: MediaId) = viewModelScope.launch {
        client.playFromMediaId(mediaId)
    }
}
