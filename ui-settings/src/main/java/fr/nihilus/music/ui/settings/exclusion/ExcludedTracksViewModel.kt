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

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.nihilus.music.core.database.exclusion.TrackExclusionDao
import fr.nihilus.music.media.dagger.SourceDao
import fr.nihilus.music.media.provider.MediaDao
import fr.nihilus.music.media.provider.Track
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
internal class ExcludedTracksViewModel @Inject constructor(
    @SourceDao private val trackSource: MediaDao,
    private val exclusionList: TrackExclusionDao
) : ViewModel() {

    /**
     * List of tracks that are excluded from the music library.
     */
    val tracks: LiveData<List<ExcludedTrack>> =
        combine(trackSource.tracks, exclusionList.trackExclusions) { tracks, exclusions ->
            val tracksById = tracks.associateBy(Track::id)
            exclusions.mapNotNull { exclusion ->
                tracksById[exclusion.trackId]?.let { track ->
                    ExcludedTrack(
                        id = track.id,
                        title = track.title,
                        artistName = track.artist,
                        excludeDate = exclusion.excludeDate
                    )
                }
            }
        }.asLiveData()

    /**
     * Remove a track from the exclusion list, displaying it again in the whole application.
     */
    fun restore(track: ExcludedTrack) {
        viewModelScope.launch {
            exclusionList.allow(track.id)
        }
    }
}
