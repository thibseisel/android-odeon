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

package fr.nihilus.music.ui.settings.exclusion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.nihilus.music.core.ui.uiStateIn
import fr.nihilus.music.media.tracks.TrackRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ExcludedTracksViewModel @Inject constructor(
    private val repository: TrackRepository,
) : ViewModel() {

    /**
     * List of tracks that are excluded from the music library.
     */
    val tracks: StateFlow<List<ExcludedTrackUiState>> by lazy {
        repository.excludedTracks
            .map { tracks ->
                tracks.map {
                    ExcludedTrackUiState(
                        id = it.id,
                        title = it.title,
                        artistName = it.artist,
                        excludeDate = checkNotNull(it.exclusionTime) {
                            "Excluded track \"${it.title}\" should have a non-null exclusionTime"
                        },
                    )
                }
            }
            .uiStateIn(viewModelScope, initialState = emptyList())
    }

    /**
     * Remove a track from the exclusion list, displaying it again in the whole application.
     */
    fun restore(track: ExcludedTrackUiState) {
        viewModelScope.launch {
            repository.allowTrack(track.id)
        }
    }
}
