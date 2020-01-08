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

import fr.nihilus.music.core.test.stub
import fr.nihilus.music.spotify.model.AudioFeature
import fr.nihilus.music.spotify.model.SpotifyAlbum
import fr.nihilus.music.spotify.model.SpotifyArtist
import fr.nihilus.music.spotify.model.SpotifyTrack
import fr.nihilus.music.spotify.service.HttpResource
import fr.nihilus.music.spotify.service.SpotifyQuery
import fr.nihilus.music.spotify.service.SpotifyService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow

internal class FakeSpotifyService(
    tracks: List<SpotifyTrack>,
    features: List<AudioFeature>
) : SpotifyService {

    private val tracks = tracks.associateBy { it.id }
    private val features = features.associateBy { it.id }

    override suspend fun getArtist(id: String): HttpResource<SpotifyArtist> = stub()

    override suspend fun getSeveralArtists(ids: List<String>) = stub()

    override fun getArtistAlbums(artistId: String): Flow<SpotifyAlbum> = stub()

    override suspend fun getAlbum(id: String): HttpResource<SpotifyAlbum> = stub()

    override suspend fun getSeveralAlbums(ids: List<String>) = stub()

    override fun getAlbumTracks(albumId: String): Flow<SpotifyTrack> = stub()

    override suspend fun getTrack(id: String): HttpResource<SpotifyTrack> {
        return tracks[id]
            ?.let { HttpResource.Loaded(it) }
            ?: HttpResource.NotFound
    }

    override suspend fun getSeveralTracks(ids: List<String>): HttpResource<List<SpotifyTrack?>> =
        HttpResource.Loaded(data = ids.map { tracks[it] })

    override suspend fun getTrackFeatures(trackId: String): HttpResource<AudioFeature> {
        return features[trackId]
            ?.let { HttpResource.Loaded(it) }
            ?: HttpResource.NotFound
    }

    override suspend fun getSeveralTrackFeatures(trackIds: List<String>): HttpResource<List<AudioFeature?>> =
        HttpResource.Loaded(data = trackIds.map { features[it] })

    override fun <T : Any> search(query: SpotifyQuery<T>): Flow<T> {
        return when (query) {
            is SpotifyQuery.Track -> tracks.values.asFlow() as Flow<T>
            else -> emptyFlow()
        }
    }
}