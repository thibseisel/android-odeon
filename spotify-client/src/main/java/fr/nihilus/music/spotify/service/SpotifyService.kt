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

package fr.nihilus.music.spotify.service

import fr.nihilus.music.spotify.model.AudioFeature
import fr.nihilus.music.spotify.model.SpotifyAlbum
import fr.nihilus.music.spotify.model.SpotifyArtist
import fr.nihilus.music.spotify.model.SpotifyTrack
import kotlinx.coroutines.flow.Flow

/**
 * A remote service that reads media metadata from the Spotify API endpoints.
 * All API calls are main-safe and will suspend the caller until the result is available.
 */
internal interface SpotifyService {

    /**
     * Get Spotify catalog information for a single artist identified by its unique Spotify ID.
     *
     * @param id The Spotify ID for the artist.
     * @return The detailed information of this artist.
     */
    suspend fun getArtist(id: String): HttpResource<SpotifyArtist>

    /**
     * Get Spotify catalog information for several artists based on their Spotify IDs.
     * If an artist is not found, a `null` is returned at the appropriate position.
     * Duplicates in [ids] results in duplicates in the returned artists.
     *
     * @param ids The Spotify IDs for the artists.
     * @return The information for each artist, in the order requested.
     */
    suspend fun getSeveralArtists(ids: List<String>): HttpResource<List<SpotifyArtist?>>

    /**
     * Get Spotify catalog information about an artist’s albums.
     * @param artistId The Spotify ID for the artist.
     *
     * @return An asynchronous flow of all albums the requested artist has participated.
     * That flow will be empty if the requested artist does not exist
     * or will fail with [ApiException] if the server returns an error.
     */
    fun getArtistAlbums(artistId: String): Flow<SpotifyAlbum>

    /**
     * Get Spotify catalog information for a single album.
     *
     * @param id The Spotify ID for the album.
     *
     * @return The detailed information of this album.
     */
    suspend fun getAlbum(id: String): HttpResource<SpotifyAlbum>

    /**
     * Get Spotify catalog information for multiple albums identified by their Spotify IDs.
     * If an album is not found, a `null` is returned at the appropriate position.
     * Duplicates in [ids] results in duplicates in the returned albums.
     *
     * @param ids The Spotify IDs for the albums.
     * @return The information for each album, in the order requested.
     */
    suspend fun getSeveralAlbums(ids: List<String>): HttpResource<List<SpotifyAlbum?>>

    /**
     * Get Spotify catalog information about an album’s tracks.
     * @param albumId The SpotifyID for the album.
     *
     * @return An asynchronous flow of all tracks from the requested album.
     * That flow will be empty if the requested album does not exist
     * or will fail with [ApiException] if the server returns an error.
     */
    fun getAlbumTracks(albumId: String): Flow<SpotifyTrack>

    /**
     * Get Spotify catalog information for a single track identified by its unique Spotify ID.
     * @param id The Spotify ID for the track.
     *
     * @return The detailed information of this track.
     */
    suspend fun getTrack(id: String): HttpResource<SpotifyTrack>

    /**
     * Get Spotify catalog information for multiple tracks identified by their Spotify IDs.
     * If a track is not found, a `null` is returned at the appropriate position.
     * Duplicates in [ids] results in duplicates in the returned tracks.
     *
     * @param ids The Spotify IDs for the tracks.
     * @return The information for each track, in the order requested.
     */
    suspend fun getSeveralTracks(ids: List<String>): HttpResource<List<SpotifyTrack?>>

    /**
     * Get audio feature information for a single track identified by its unique Spotify ID.
     * @param trackId The Spotify ID for the track.
     *
     * @return The audio features for the requested track.
     */
    suspend fun getTrackFeatures(trackId: String): HttpResource<AudioFeature>

    /**
     * Get audio features for multiple tracks based on their Spotify IDs.
     * If a track is not found, a `null` is returned at the appropriate position.
     * Duplicates in [trackIds] results in duplicates in the returned tracks' features.
     *
     * @param trackIds The Spotify IDs for the tracks.
     * @return The audio features for each track, in the order requested.
     */
    suspend fun getSeveralTrackFeatures(trackIds: List<String>): HttpResource<List<AudioFeature?>>

    /**
     * Get Spotify catalog information about artists, albums or tracks that match a keyword string.
     *
     * For example searching...
     * - `SpotifyQuery.Artist("rammstein")` returns all tracks by Rammstein ordered by descending popularity.
     * - `SpotifyQuery.Album("rammstein")` returns all tracks from the RAMMSTEIN album.
     * - `SpotifyQuery.Track("rammstein")` returns one track named "Rammstein" (from Herzeleid).
     *
     * @param query The query to be submitted to the search endpoint.
     * That query hints both what to search (the type of the media to return)
     * and how to search (special criteria used to refine the search).
     */
    fun <T : Any> search(query: SpotifyQuery<T>): Flow<T>

    companion object {
        internal const val QUERY_Q = "q"
        internal const val QUERY_IDS = "ids"
        internal const val QUERY_LIMIT = "limit"
        internal const val QUERY_OFFSET = "offset"
        internal const val QUERY_TYPE = "type"
        internal const val QUERY_INCLUDE_GROUPS = "include_groups"
    }
}