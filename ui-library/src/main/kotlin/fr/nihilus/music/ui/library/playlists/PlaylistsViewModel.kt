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

package fr.nihilus.music.ui.library.playlists

import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.parse
import fr.nihilus.music.core.ui.client.BrowserClient
import fr.nihilus.music.core.ui.uiStateIn
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * State holder [PlaylistsFragment].
 */
@HiltViewModel
internal class PlaylistsViewModel @Inject constructor(
    private val client: BrowserClient,
) : ViewModel() {

    /**
     * Live UI state.
     */
    val state: StateFlow<PlaylistsScreenUiState> by lazy {
        combine(builtInPlaylists, userPlaylists) { builtIns, playlists ->
            PlaylistsScreenUiState(
                playlists = builtIns + playlists,
                isLoadingPlaylists = false
            )
        }
            .uiStateIn(
                viewModelScope,
                initialState = PlaylistsScreenUiState(
                    playlists = emptyList(),
                    isLoadingPlaylists = true
                )
            )
    }

    private val builtInPlaylists: Flow<List<PlaylistUiState>>
        get() = flow {
            val playlists = coroutineScope {
                val recentlyAdded = async { loadBuiltIn(MediaId.CATEGORY_RECENTLY_ADDED) }
                val mostRated = async { loadBuiltIn(MediaId.CATEGORY_MOST_RATED) }
                listOf(
                    recentlyAdded.await(),
                    mostRated.await()
                )
            }
            emit(playlists)
        }

    private val userPlaylists: Flow<List<PlaylistUiState>>
        get() = client.getChildren(MediaId.ALL_PLAYLISTS)
            .map { playlists -> playlists.map { it.toUiPlaylist() } }
            .catch { emit(emptyList()) }

    private suspend fun loadBuiltIn(category: String): PlaylistUiState {
        val itemId = MediaId(MediaId.TYPE_TRACKS, category)
        return client.getItem(itemId)
            ?.toUiPlaylist()
            ?: error("Item with $itemId should always exist")
    }

    private fun MediaBrowserCompat.MediaItem.toUiPlaylist() = PlaylistUiState(
        id = mediaId.parse(),
        title = description.title?.toString() ?: "",
        subtitle = description.subtitle?.toString() ?: "",
        iconUri = description.iconUri,
    )
}
