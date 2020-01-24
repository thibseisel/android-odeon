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

package fr.nihilus.music.spotify.manager

import fr.nihilus.music.core.collections.associateByLong
import fr.nihilus.music.core.collections.values
import fr.nihilus.music.core.database.spotify.LocalizedTrackFeature
import fr.nihilus.music.core.database.spotify.SpotifyDao
import fr.nihilus.music.core.database.spotify.SpotifyLink
import fr.nihilus.music.core.database.spotify.TrackFeature

/**
 * An fake implementation of [SpotifyDao] that simulates a database
 * by storing in-memory collections of [SpotifyLink]s and [TrackFeature].
 *
 * @param links Initial track links in the simulated database.
 * If a local track has more that one link, then only the latest link is considered.
 * @param features Initial audio features in the simulated database.
 * There should be no features for a track that has no link.
 */
internal class FakeSpotifyDao(
    links: List<SpotifyLink> = emptyList(),
    features: List<TrackFeature> = emptyList()
) : SpotifyDao {

    private val _links = links.associateByLong { it.trackId }
    private val _features = features.associateByTo(mutableMapOf()) { it.id }

    /**
     * List of all links stored in the simulated database.
     */
    val links: List<SpotifyLink>
        get() = _links.values

    /**
     * List of all audio features stored in the simulated database.
     */
    val features: List<TrackFeature>
        get() = _features.values.toList()

    override suspend fun getLinks(): List<SpotifyLink> = links

    override suspend fun getLocalizedFeatures(): List<LocalizedTrackFeature> {
        return links.mapNotNull { link ->
            _features[link.spotifyId]?.let { feature ->
                LocalizedTrackFeature(link.trackId, feature)
            }
        }
    }

    override suspend fun saveTrackFeature(link: SpotifyLink, feature: TrackFeature) {
        _links.put(link.trackId, link)
        _features[feature.id] = feature
    }

    override suspend fun deleteLinks(localTrackIds: LongArray) {
        for (trackId in localTrackIds) {
            _links[trackId]?.let { droppedLink ->
                _features.remove(droppedLink.spotifyId)
                _links.remove(trackId)
            }
        }
    }
}