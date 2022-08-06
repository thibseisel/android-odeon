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

package fr.nihilus.music.ui.library.artists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.ui.uiStateIn
import fr.nihilus.music.media.MediaCategory
import fr.nihilus.music.media.browser.BrowserTree
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * State holder of [ArtistsFragment].
 */
@HiltViewModel
internal class ArtistsViewModel @Inject constructor(
    private val browser: BrowserTree,
) : ViewModel() {

    /**
     * Live UI state.
     */
    val state: StateFlow<ArtistsScreenUiState> by lazy {
        browser.getChildren(MediaId.ALL_ARTISTS)
            .map { artists ->
                ArtistsScreenUiState(
                    isLoadingArtists = false,
                    artists = artists.filterIsInstance<MediaCategory>().map {
                            ArtistUiState(
                                id = it.id,
                                name = it.title,
                                trackCount = it.count,
                                iconUri = it.iconUri,
                            )
                        },
                )
            }
            .uiStateIn(
                viewModelScope,
                initialState = ArtistsScreenUiState(
                    artists = emptyList(),
                    isLoadingArtists = true
                )
            )
    }

}
