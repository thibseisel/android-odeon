/*
 * Copyright 2020 Thibault Seisel
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

package fr.nihilus.music.devmenu.features

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import fr.nihilus.music.core.database.spotify.TrackFeature
import fr.nihilus.music.spotify.manager.FeatureFilter
import fr.nihilus.music.spotify.manager.FeaturedTrack
import fr.nihilus.music.spotify.manager.SpotifyManager
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import javax.inject.Inject

internal class ComposerViewModel @Inject constructor(
    private val manager: SpotifyManager
) : ViewModel() {

    private val _filters = ConflatedBroadcastChannel<List<FeatureFilterState>>(emptyList())

    val filters: LiveData<List<FeatureFilterState>> = liveData {
        _filters.asFlow().collectLatest {
            delay(1000)
            emit(it)
        }
    }

    val tracks: LiveData<List<FeaturedTrack>> = liveData {
        _filters.asFlow()
            .debounce(300)
            .collectLatest {
                val filters = it.map { specs ->
                    val featureSelector = when (specs.feature) {
                        Feature.TEMPO -> TrackFeature::tempo
                        Feature.LOUDNESS -> TrackFeature::loudness
                        Feature.ENERGY -> TrackFeature::energy
                        Feature.DANCEABILITY -> TrackFeature::danceability
                        Feature.INSTRUMENTALNESS -> TrackFeature::instrumentalness
                        Feature.VALENCE -> TrackFeature::valence
                    }

                    FeatureFilter.OnRange(featureSelector, specs.minValue, specs.maxValue)
                }

                val tracks = manager.findTracksHavingFeatures(filters)
                emit(tracks)
            }
    }

    fun setFilter(specs: FeatureFilterState) {
        val currentFilters = _filters.valueOrNull ?: emptyList()
        val existingFilterIndex = currentFilters.indexOfFirst { it.feature == specs.feature }

        if (existingFilterIndex >= 0) {
            val updatedFilters = currentFilters.toMutableList()
            updatedFilters[existingFilterIndex] = specs
            _filters.offer(updatedFilters)

        } else {
            _filters.offer(currentFilters + specs)
        }
    }

    fun removeFilter(specs: FeatureFilterState) {
        val currentFilters = _filters.valueOrNull ?: emptyList()
        val removedFilterIndex = currentFilters.indexOfFirst { it.feature == specs.feature }

        if (removedFilterIndex >= 0) {
            val updatedFilters = currentFilters.toMutableList()
            updatedFilters.removeAt(removedFilterIndex)
            _filters.offer(updatedFilters)
        }
    }

    override fun onCleared() {
        _filters.close()
        super.onCleared()
    }
}

internal class FeatureFilterState(
    val feature: Feature,
    val minValue: Float,
    val maxValue: Float
)