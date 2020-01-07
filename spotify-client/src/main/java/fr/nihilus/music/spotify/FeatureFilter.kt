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

package fr.nihilus.music.spotify

import fr.nihilus.music.core.database.spotify.MusicalMode
import fr.nihilus.music.core.database.spotify.Pitch
import fr.nihilus.music.core.database.spotify.TrackFeature

/**
 * Defines search criteria for filtering tracks on the value of a specific audio feature.
 */
sealed class FeatureFilter {

    /**
     * Indicates whether the features of a given track matches the filter.
     *
     * @param feature The set of audio features of one track.
     * @return `true` if the track associated with the features matches the filter.
     */
    abstract fun matches(feature: TrackFeature): Boolean

    /**
     * Filters tracks on their global tone.
     * This filter may apply restrictions on either the [key], the [mode], or both.
     *
     * @param key The only accepted pitch key, or `null` to allow them all.
     * @param mode The only accepted mode, or `null` to allow them all.
     */
    class OnTone(
        private val key: Pitch?,
        private val mode: MusicalMode?
    ) : FeatureFilter() {

        override fun matches(feature: TrackFeature): Boolean {
            return (key == null || feature.key == key) && (mode == null || feature.mode == mode)
        }
    }

    /**
     * Filters tracks based on an accepted range of values for one specific feature.
     *
     * @param lowest The lowest accepted value.
     * @param highest The highest accepted value.
     */
    class OnRange(
        private val featureProvider: (TrackFeature) -> Float,
        private val lowest: Float,
        highest: Float
    ) : FeatureFilter() {
        private val highest: Float = highest.coerceAtLeast(lowest)

        override fun matches(feature: TrackFeature): Boolean {
            val featureValue = featureProvider(feature)
            return featureValue in lowest..highest
        }
    }
}