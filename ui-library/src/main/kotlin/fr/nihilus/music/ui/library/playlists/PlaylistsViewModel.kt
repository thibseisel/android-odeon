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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.ui.uiStateIn
import fr.nihilus.music.media.MediaCategory
import fr.nihilus.music.media.browser.BrowserTree
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * State holder [PlaylistsFragment].
 */
@HiltViewModel
internal class PlaylistsViewModel @Inject constructor(
    private val browser: BrowserTree,
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
        get() = browser.getChildren(MediaId.ALL_PLAYLISTS).map { playlists ->
                playlists.filterIsInstance<MediaCategory>().map { it.toUiPlaylist() }
            }

    private suspend fun loadBuiltIn(category: String): PlaylistUiState {
        val itemId = MediaId(MediaId.TYPE_TRACKS, category)
        return (browser.getItem(itemId) as? MediaCategory)?.toUiPlaylist()
            ?: error("Item with $itemId should always exist")
    }

    private fun MediaCategory.toUiPlaylist() = PlaylistUiState(
        id = id,
        title = title,
        subtitle = subtitle.orEmpty(),
        iconUri = iconUri,
    )
}
