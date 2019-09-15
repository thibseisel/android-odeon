/*
 * Copyright 2019 Thibault Seisel
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

import fr.nihilus.music.spotify.model.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface SpotifyService {

    /**
     * Get Spotify catalog information for a single artist identified by its unique Spotify ID.
     *
     * @param id The Spotify ID for the artist.
     * @return The detailed information of this artist.
     *
     * @throws ResourceNotFound If the requested [id] does not match an existing artist.
     */
    @GET("artists/{id}")
    suspend fun getArtist(@Path("id") id: String): Artist

    /**
     * Get Spotify catalog information for several artists based on their Spotify IDs.
     * If an artist is not found, a `null` is returned at the appropriate position.
     * Duplicates in [ids] results in duplicates in the returned artists.
     *
     * @param ids The Spotify IDs for the artists. Maximum `50` IDs.
     * @return The information for each artist, in the order requested.
     */
    @GET("artists")
    suspend fun getSeveralArtists(@Query("ids") ids: List<String>): List<Artist?>

    /**
     * Get Spotify catalog information about an artist’s albums.
     *
     * @param artistId The Spotify ID for the artist.
     * @param offset The index of the first album to return. Default: `0` (i.e., the first album).
     * Use with [limit] to get the next set of albums.
     * @param limit The number of albums to return. Default: `20`. Minimum: `1`. Maximum: `50`.
     *
     * @return A paginated list of albums where the requested artist participates.
     * @throws ResourceNotFound If the requested artist does not exist.
     */
    @GET("artists/{id}/albums")
    suspend fun getArtistAlbums(
        @Path("id") artistId: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Paging<Album>

    /**
     * Get Spotify catalog information for a single album.
     *
     * @param id The Spotify ID for the album.
     *
     * @return The detailed information of this album.
     * @throws ResourceNotFound If the requested album does not exist.
     */
    @GET("albums/{id}")
    suspend fun getAlbum(@Path("id") id: String): Album

    /**
     * Get Spotify catalog information for multiple albums identified by their Spotify IDs.
     * If an album is not found, a `null` is returned at the appropriate position.
     * Duplicates in [ids] results in duplicates in the returned albums.
     *
     * @param ids The Spotify IDs for the albums. Maximum: `20` IDs.
     * @return The information for each album, in the order requested.
     */
    @GET("albums")
    suspend fun getSeveralAlbums(@Query("ids") ids: List<String>): List<Album?>

    /**
     * Get Spotify catalog information about an album’s tracks.
     *
     * @param albumId The SpotifyID for the album.
     * @param offset The index of the first track to return. Default: `0`.
     * Use with limit to get the next set of tracks.
     * @param limit The maximum number of tracks to return. Default: `20`. Minimum: `1`. Maximum: `50`.
     *
     * @return A paginated list of tracks from the requested album.
     * @throws ResourceNotFound If the requested album does not exist.
     */
    @GET("albums/{id}/tracks")
    suspend fun getAlbumTracks(
        @Path("id") albumId: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Paging<Track>

    /**
     * Get Spotify catalog information for a single track identified by its unique Spotify ID.
     * @param id The Spotify ID for the track.
     *
     * @return The detailed information of this track.
     * @throws ResourceNotFound If the requested track does not exist.
     */
    @GET("tracks/{id}")
    suspend fun getTrack(@Path("id") id: String): Track

    /**
     * Get Spotify catalog information for multiple tracks identified by their Spotify IDs.
     * If a track is not found, a `null` is returned at the appropriate position.
     * Duplicates in [ids] results in duplicates in the returned tracks.
     *
     * @param ids The Spotify IDs for the tracks. Maximum: `50` IDs.
     * @return The information for each track, in the order requested.
     */
    @GET("tracks")
    suspend fun getSeveralTracks(@Query("ids") ids: List<String>): List<Track?>

    /**
     * Get audio feature information for a single track identified by its unique Spotify ID.
     * @param trackId The Spotify ID for the track.
     *
     * @return The audio features for the requested track.
     * @throws ResourceNotFound If the requested track does not exist.
     */
    @GET("audio-features/{id}")
    suspend fun getTrackFeatures(@Path("id") trackId: String): AudioFeatures

    /**
     * Get audio features for multiple tracks based on their Spotify IDs.
     * If a track is not found, a `null` is returned at the appropriate position.
     * Duplicates in [trackIds] results in duplicates in the returned tracks' features.
     *
     * @param trackIds The Spotify IDs for the tracks. Maximum: `100` IDs.
     * @return The audio features for each track, in the order requested.
     */
    @GET("audio-features")
    suspend fun getSeveralTrackFeatures(@Query("ids") trackIds: List<String>): List<AudioFeatures?>

    /**
     * Get Spotify catalog information about artists, albums or tracks that match a keyword string.
     * Search results include hits from all the specified item [types][type].
     * For example, if [query] = `abacab` and [type] = `[album,track]`
     *
     * @param query Search keywords and optional fields filters and operators.
     * @param type The list of types to search across.
     * @param limit Maximum number of results to return. Default `20`, minimum `1` and maximum `50`.
     * __Note__: the limit is applied within each type, not on the total response.
     * For example, if the limit value is 3 and the type is `[artist,album]`, the result contains 3 artists and 3 albums.
     * @param offset The index of the first result to return. Defaults to `0` (the first result).
     * The maximum offset (including limit) is `10000`.
     */
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: List<String>,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): SearchResults
}