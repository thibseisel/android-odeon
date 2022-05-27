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

package fr.nihilus.music.library.albums

import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.parse
import fr.nihilus.music.core.ui.client.BrowserClient
import fr.nihilus.music.core.ui.uiStateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * State holder for [AlbumsFragment].
 */
@HiltViewModel
internal class AlbumGridViewModel @Inject constructor(
    private val client: BrowserClient
) : ViewModel() {

    /**
     * Live UI state.
     */
    val state: StateFlow<AlbumGridUiState> by lazy {
        client.getChildren(MediaId.ALL_ALBUMS)
            .map { albumItems ->
                AlbumGridUiState(
                    albums = albumItems.map { it.toUiAlbum() },
                    isLoadingAlbums = false
                )
            }
            .uiStateIn(
                viewModelScope,
                initialState = AlbumGridUiState(
                    albums = emptyList(),
                    isLoadingAlbums = true
                )
            )
    }

    private fun MediaBrowserCompat.MediaItem.toUiAlbum() = AlbumUiState(
        id = mediaId.parse(),
        title = description.title?.toString() ?: "",
        artist = description.subtitle?.toString() ?: "",
        artworkUri = description.iconUri
    )
}
