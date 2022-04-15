/*
 * Copyright 2021 Thibault Seisel
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

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaItems
import fr.nihilus.music.core.media.parse
import fr.nihilus.music.core.ui.client.BrowserClient
import fr.nihilus.music.service.extensions.id
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class AlbumDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val client: BrowserClient
) : ViewModel() {
    private val albumId: MediaId

    init {
        val navArgs = AlbumDetailFragmentArgs.fromSavedStateHandle(savedState)
        this.albumId = navArgs.albumId.parse()
    }

    /**
     * Live UI state describing detail of the viewed album with its tracks.
     */
    val state: LiveData<AlbumDetailState> = combine(
        flow { this.emit(client.getItem(albumId)) },
        client.getChildren(albumId).catch { cause ->
            Timber.e(cause, "Failed to load album children")
        },
        client.nowPlaying
    ) { album, tracks, nowPlaying ->
        val currentlyPlayingMediaId = nowPlaying?.id?.parse()
        checkNotNull(album)

        AlbumDetailState(
            id = album.mediaId.parse(),
            title = album.description.title?.toString() ?: "",
            subtitle = album.description.subtitle?.toString() ?: "",
            artworkUri = album.description.iconUri,
            tracks = tracks.map {
                val trackId = it.mediaId.parse()
                val extras = checkNotNull(it.description.extras)

                AlbumDetailState.Track(
                    id = trackId,
                    title = it.description.title?.toString() ?: "",
                    number = extras.getInt(MediaItems.EXTRA_TRACK_NUMBER),
                    duration = extras.getLong(MediaItems.EXTRA_DURATION),
                    isPlaying = trackId == currentlyPlayingMediaId
                )
            }
        )
    }.asLiveData()

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
    fun playTrack(track: AlbumDetailState.Track) {
        playMedia(track.id)
    }

    private fun playMedia(mediaId: MediaId) = viewModelScope.launch {
        client.playFromMediaId(mediaId)
    }
}