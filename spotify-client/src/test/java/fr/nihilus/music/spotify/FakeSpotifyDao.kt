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

import fr.nihilus.music.core.database.spotify.SpotifyDao
import fr.nihilus.music.core.database.spotify.SpotifyLink
import fr.nihilus.music.core.database.spotify.TrackFeature

internal class FakeSpotifyDao(
    links: List<SpotifyLink> = emptyList(),
    features: List<TrackFeature> = emptyList()
) : SpotifyDao {

    private val _links = links.toMutableSet()
    private val _features = features.associateByTo(mutableMapOf()) { it.id }

    val links: List<SpotifyLink>
        get() = _links.toList()

    val features: List<TrackFeature>
        get() = _features.values.toList()

    override suspend fun getLinks(): List<SpotifyLink> = links

    override suspend fun saveTrackFeature(link: SpotifyLink, feature: TrackFeature) {
        _links += link
        _features[feature.id] = feature
    }
}