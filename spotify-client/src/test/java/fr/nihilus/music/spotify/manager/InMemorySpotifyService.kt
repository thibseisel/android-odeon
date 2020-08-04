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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * A test implementation of [SpotifyService] that mimics a network-based service
 * by returning predefined sets of artists, albums, tracks and audio features.
 */
internal object InMemorySpotifyService : SpotifyService {

    private val artists: Map<String, SpotifyArtist> = arrayOf(
        SpotifyArtist(
            id = "12Chz98pHFMPJEknJQMWvI",
            name = "Muse",
            popularity = 82,
            genres = listOf("modern rock", "permanent wave", "piano rock", "post-grunge", "rock"),
            images = emptyList()
        ),
        SpotifyArtist(
            id = "7jy3rLJdDQY21OgRLCZ9sD",
            name = "Foo Fighters",
            popularity = 79,
            genres = listOf("alternative metal", "alternative rock", "modern rock", "permanent wave", "post-grunge", "rock"),
            images = emptyList()
        ),
        SpotifyArtist(
            id = "3AMut7lAb1JjINkn8Fmkhu",
            name = "MAXIMUM THE HORMONE",
            popularity = 52,
            genres = listOf("j-metal", "japanese metalcore"),
            images = emptyList()
        )
    ).associateBy(SpotifyArtist::id)

    private val albums: Map<String, SpotifyAlbum> = arrayOf(
        SpotifyAlbum(
            "5OZgDtx180ZZPMpm36J2zC",
            "Simulation Theory (Super Deluxe)",
            "2018-11-09",
            "day",
            emptyList()
        ),
        SpotifyAlbum(
            "6KMkuqIwKkwUhUYRPL6dUc",
            "Concrete and Gold",
            "2017-09-15",
            "day",
            emptyList()
        ),
        SpotifyAlbum(
            "43KD7ooLIEkXriTaZA4drI",
            "Bu-ikikaesu",
            "2007-03-14",
            "day",
            emptyList()
        ),
        SpotifyAlbum(
            "3ilXDEG0xiajK8AbqboeJz",
            "Echoes, Silence, Patience & Grace",
            "2007-09-25",
            "day",
            emptyList()
        )
    ).associateBy(SpotifyAlbum::id)

    private val artistAlbums = mapOf(
        "12Chz98pHFMPJEknJQMWvI" to listOf("5OZgDtx180ZZPMpm36J2zC"),
        "7jy3rLJdDQY21OgRLCZ9sD" to listOf("6KMkuqIwKkwUhUYRPL6dUc", "3ilXDEG0xiajK8AbqboeJz"),
        "3AMut7lAb1JjINkn8Fmkhu" to listOf("43KD7ooLIEkXriTaZA4drI")
    )

    private val tracks: Map<String, SpotifyTrack> = arrayOf(
        SpotifyTrack("7f0vVL3xi4i78Rv5Ptn2s1", "Algorithm", 1, 1, 245960, false),
        SpotifyTrack("0dMYPDqcI4ca4cjqlmp9mE", "The Dark Side", 1, 2, 227213, false),
        SpotifyTrack("1esX5rtwwssnsEQNQk0HGg", "Something Human", 1, 6, 226786, false),
        SpotifyTrack("1D2ISRyHAs9QBHIWVQIbgM", "Something Human (Acoustic)", 1, 15, 226333, false),
        SpotifyTrack("5lnsL7pCg0fQKcWnlkD1F0", "Dirty Water", 1, 6, 320880, false),
        SpotifyTrack("7x8dCjCr0x6x2lXKujYD34", "The Pretender", 1, 1, 269373, false),
        SpotifyTrack("5Ppa3ayepketqhfXf9yO39", "ChuChu Lovely MuniMuni MuraMura PrinPrin Boron Nururu ReroRero", 1, 11, 186293, false),
        SpotifyTrack("1px2pUjdQreXBDQ5rqYj7U", "Kuso breakin' Nou breakin' Lily", 1, 3, 255973, false)
    ).associateBy(SpotifyTrack::id)

    private val features: Map<String, AudioFeature> = arrayOf(
        AudioFeature("7f0vVL3xi4i78Rv5Ptn2s1", 2, 1, 170.057f, 4, -4.56f, 0.0125f, 0.522f, 0.923f, 0.017f, 0.0854f, 0.0539f, 0.595f),
        AudioFeature("0dMYPDqcI4ca4cjqlmp9mE", 2, 1, 99.979f, 4, -3.759f, 0.000884f, 0.484f, 0.927f, 0.00000396f, 0.223f, 0.0425f, 0.389f),
        AudioFeature("1esX5rtwwssnsEQNQk0HGg", 9, 1, 105.018f, 4, -4.841f, 0.0536f, 0.59f, 0.903f, 0.00289f, 0.12f, 0.0542f,0.545f),
        AudioFeature("1D2ISRyHAs9QBHIWVQIbgM", 9, 1, 105.023f, 4, -5.284f, 0.425f, 0.609f, 0.832f, 0.000044f, 0.17f, 0.039f, 0.654f),
        AudioFeature("5lnsL7pCg0fQKcWnlkD1F0", 7, 1, 142.684f, 4, -8.245f, 0.00365f, 0.324f, 0.631f, 0.0459f, 0.221f, 0.0407f, 0.346f),
        AudioFeature("7x8dCjCr0x6x2lXKujYD34", 9, 1, 172.984f, 4, -4.04f, 0.000917f, 0.433f, 0.959f, 0f, 0.028f, 0.0431f, 0.365f),
        AudioFeature("5Ppa3ayepketqhfXf9yO39", 8, 1, 128.903f, 4, -1.343f, 0.000488f, 0.518f, 0.961f, 0.00000563f, 0.34f, 0.000488f, 0.928f),
        AudioFeature("1px2pUjdQreXBDQ5rqYj7U", 1, 1, 163.654f, 4, -1.803f, 0.00656f, 0.322f, 0.936f, 0f, 0.183f, 0.184f, 0.663f)
    ).associateBy(AudioFeature::id)

    private val albumTracks = mapOf(
        "5OZgDtx180ZZPMpm36J2zC" to listOf("7f0vVL3xi4i78Rv5Ptn2s1", "0dMYPDqcI4ca4cjqlmp9mE", "1esX5rtwwssnsEQNQk0HGg", "1D2ISRyHAs9QBHIWVQIbgM"),
        "6KMkuqIwKkwUhUYRPL6dUc" to listOf("5lnsL7pCg0fQKcWnlkD1F0"),
        "43KD7ooLIEkXriTaZA4drI" to listOf("5Ppa3ayepketqhfXf9yO39", "1px2pUjdQreXBDQ5rqYj7U"),
        "3ilXDEG0xiajK8AbqboeJz" to listOf("7x8dCjCr0x6x2lXKujYD34")
    )

    override suspend fun getArtist(id: String): HttpResource<SpotifyArtist> {
        return artists[id]
            ?.let { HttpResource.Loaded(it) }
            ?: HttpResource.NotFound
    }

    override suspend fun getSeveralArtists(ids: List<String>): HttpResource<List<SpotifyArtist?>> =
        HttpResource.Loaded(data = ids.map { artists[it] })

    override fun getArtistAlbums(artistId: String): Flow<SpotifyAlbum> {
        return artistAlbums[artistId]
            ?.map { albums.getValue(it) }?.asFlow()
            ?: emptyFlow()
    }

    override suspend fun getAlbum(id: String): HttpResource<SpotifyAlbum> {
        return albums[id]
            ?.let { HttpResource.Loaded(it) }
            ?: HttpResource.NotFound
    }

    override suspend fun getSeveralAlbums(ids: List<String>): HttpResource<List<SpotifyAlbum?>> =
        HttpResource.Loaded(data = ids.map { albums[it] })

    override fun getAlbumTracks(albumId: String): Flow<SpotifyTrack> {
        return albumTracks[albumId]
            ?.map { tracks.getValue(it) }?.asFlow()
            ?: emptyFlow()
    }

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
        val results: List<Any> = when (query) {
            is SpotifyQuery.Artist -> {
                searchArtistsByName(query.name)
            }

            is SpotifyQuery.Album -> {
                val sourceAlbums = if (query.artist == null) {
                    albums.values
                } else {
                    searchAlbumsByArtistName(query.artist)
                }

                sourceAlbums.filter { it.name.contains(query.title, ignoreCase = true) }
            }

            is SpotifyQuery.Track -> {
                val albumsMatchingArtist = if (query.artist != null) {
                    searchAlbumsByArtistName(query.artist)
                } else {
                    albums.values
                }

                val matchingAlbums = if (query.album != null) {
                    albumsMatchingArtist.filter { it.name.contains(query.album, ignoreCase = true) }
                } else {
                    albumsMatchingArtist
                }

                val sourceTracks = matchingAlbums.flatMap { album ->
                    val trackIds = albumTracks.getValue(album.id)
                    trackIds.map { tracks.getValue(it) }
                }

                sourceTracks.filter { it.name.contains(query.title, ignoreCase = true) }
            }
        }

        @Suppress("UNCHECKED_CAST")
        return results.asFlow() as Flow<T>
    }

    private fun searchArtistsByName(name: String): List<SpotifyArtist> =
        artists.values.filter { it.name.contains(name, ignoreCase = true) }

    private fun searchAlbumsByArtistName(artistName: String): List<SpotifyAlbum> {
        val matchingArtists = searchArtistsByName(artistName)
        return matchingArtists.flatMap { artist ->
            val albumIds = artistAlbums.getValue(artist.id)
            albumIds.map { albums.getValue(it) }
        }
    }

}
