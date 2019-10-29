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

package fr.nihilus.music.library.albums

import androidx.lifecycle.*
import fr.nihilus.music.core.media.MediaItems
import fr.nihilus.music.core.ui.client.BrowserClient
import fr.nihilus.music.service.extensions.id
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class AlbumDetailViewModel
@Inject constructor(
    private val client: BrowserClient
) : ViewModel() {

    private val albumId = MutableLiveData<String>()

    /**
     * Live UI state describing detail of the viewed album with its tracks.
     * The value of this [LiveData] will only be available after [setAlbumId] has been called.
     */
    val state: LiveData<AlbumDetailState> = albumId.asFlow()
        .distinctUntilChanged()
        .flatMapLatest { albumId ->
            val albumFlow = flow { emit(client.getItem(albumId)) }
            val albumTracksFlow = client.getChildren(albumId)

            combine(albumFlow, albumTracksFlow, client.nowPlaying) { album, tracks, nowPlaying ->
                val currentlyPlayingMediaId = nowPlaying?.id
                checkNotNull(album)

                AlbumDetailState(
                    id = checkNotNull(album.mediaId),
                    title = album.description.title?.toString() ?: "",
                    subtitle = album.description.subtitle?.toString() ?: "",
                    artworkUri = album.description.iconUri,
                    tracks = tracks.map {
                        val trackId = checkNotNull(it.mediaId)
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
            }
        }.asLiveData()

    /**
     * Loads the detail of the album with the specified [media id][albumId].
     * If an album is already loading or loaded it will be replaced.
     *
     * @param albumId The media id of the album.
     */
    fun setAlbumId(albumId: String) {
        this.albumId.value = albumId
    }

    /**
     * Start playback of all tracks from the currently displayed album.
     */
    fun playAlbum() {
        val albumId = this.albumId.value
        if (albumId != null) {
            playMedia(albumId)
        }
    }

    /**
     * Start playback of a given track in the album.
     *
     * @param track The track to be played.
     */
    fun playTrack(track: AlbumDetailState.Track) {
        playMedia(track.id)
    }

    private fun playMedia(mediaId: String) = viewModelScope.launch {
        client.playFromMediaId(mediaId)
    }
}