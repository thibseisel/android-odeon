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

import fr.nihilus.music.spotify.model.AudioFeature
import fr.nihilus.music.spotify.model.SpotifyAlbum
import fr.nihilus.music.spotify.model.SpotifyArtist
import fr.nihilus.music.spotify.model.SpotifyTrack
import fr.nihilus.music.spotify.service.HttpResource
import fr.nihilus.music.spotify.service.SpotifyQuery
import fr.nihilus.music.spotify.service.SpotifyService
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * An implementation of [SpotifyService] that always fail due to having no network connectivity.
 * This could be used as a stub implementation.
 */
internal object OfflineSpotifyService : SpotifyService {

    override suspend fun getArtist(id: String): HttpResource<SpotifyArtist> {
        failDueToNoNetwork()
    }

    override suspend fun getSeveralArtists(ids: List<String>): HttpResource<List<SpotifyArtist?>> {
        failDueToNoNetwork()
    }

    override fun getArtistAlbums(artistId: String): Flow<SpotifyAlbum> = flow {
        failDueToNoNetwork()
    }

    override suspend fun getAlbum(id: String): HttpResource<SpotifyAlbum> {
        failDueToNoNetwork()
    }

    override suspend fun getSeveralAlbums(ids: List<String>): HttpResource<List<SpotifyAlbum?>> {
        failDueToNoNetwork()
    }

    override fun getAlbumTracks(albumId: String): Flow<SpotifyTrack> = flow {
        failDueToNoNetwork()
    }

    override suspend fun getTrack(id: String): HttpResource<SpotifyTrack> {
        failDueToNoNetwork()
    }

    override suspend fun getSeveralTracks(ids: List<String>): HttpResource<List<SpotifyTrack?>> {
        failDueToNoNetwork()
    }

    override suspend fun getTrackFeatures(trackId: String): HttpResource<AudioFeature> {
        failDueToNoNetwork()
    }

    override suspend fun getSeveralTrackFeatures(trackIds: List<String>): HttpResource<List<AudioFeature?>> {
        failDueToNoNetwork()
    }

    override fun <T : Any> search(query: SpotifyQuery<T>): Flow<T> = flow {
        failDueToNoNetwork()
    }

    private fun failDueToNoNetwork(): Nothing {
        throw IOException("No network connectivity.")
    }

}