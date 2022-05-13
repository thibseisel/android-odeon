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

import fr.nihilus.music.core.database.spotify.MusicalMode
import fr.nihilus.music.core.database.spotify.Pitch
import fr.nihilus.music.core.database.spotify.SpotifyLink
import fr.nihilus.music.core.database.spotify.TrackFeature
import fr.nihilus.music.core.test.os.TestClock
import fr.nihilus.music.media.tracks.Track
import io.kotest.assertions.extracting
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class SpotifyManagerTest {
    private val clock = TestClock(123456789L)

    @Test
    fun `Given no filter, when finding tracks by feature then only return linked tracks`() =
        runTest {
            val repository = FakeMediaDao(
                sampleTrack(481, "Dirty Water", "Foo Fighters", "Concrete and Gold", 6),
                sampleTrack(125, "Give It Up", "AC/DC", "Stiff Upper Lip", 12),
                sampleTrack(75, "Nightmare", "Avenged Sevenfold", "Nightmare", 1)
            )

            val localDao = FakeSpotifyDao(
                links = listOf(
                    SpotifyLink(481, "EAzDgVCnoZnJZjIq1Bx4FW", 0),
                    SpotifyLink(75, "vJy3wp8BworPoz6o30oDxy", 0)
                ),
                features = listOf(
                    TrackFeature(
                        id = "EAzDgVCnoZnJZjIq1Bx4FW",
                        key = Pitch.D,
                        mode = MusicalMode.MAJOR,
                        tempo = 100f,
                        signature = 4,
                        loudness = -13f,
                        acousticness = 0.04f,
                        danceability = 0.2f,
                        energy = 0.7f,
                        instrumentalness = 0.1f,
                        liveness = 0.2f,
                        speechiness = 0.08f,
                        valence = 0.75f
                    ),
                    TrackFeature(
                        id = "vJy3wp8BworPoz6o30oDxy",
                        key = Pitch.A,
                        mode = MusicalMode.MINOR,
                        tempo = 82f,
                        signature = 4,
                        loudness = -8f,
                        acousticness = 0f,
                        danceability = 0.4f,
                        energy = 0.9f,
                        instrumentalness = 0.1f,
                        liveness = 0.1f,
                        speechiness = 0f,
                        valence = 0.3f
                    )
                )
            )

            val manager = SpotifyManagerImpl(repository, OfflineSpotifyService, localDao, clock)

            val tracksIds =
                manager.findTracksHavingFeatures(emptyList()).map { (track, _) -> track.id }
            tracksIds.shouldContainExactlyInAnyOrder(481L, 75L)
        }

    @Test
    fun `When finding tracks by feature then only return tracks matching all filters`() = runTest {
        val dMajorFilter = FeatureFilter.OnTone(Pitch.D, MusicalMode.MAJOR)
        val moderatoFilter = FeatureFilter.OnRange(TrackFeature::tempo, 88f, 112f)
        val happyFilter = FeatureFilter.OnRange(TrackFeature::valence, 0.6f, 1.0f)

        val repository = FakeMediaDao(
            sampleTrack(1, "1", "A", "A", 1),
            sampleTrack(2, "2", "B", "B", 1),
            sampleTrack(3, "3", "B", "B", 2),
            sampleTrack(4, "4", "C", "C", 1),
            sampleTrack(5, "5", "C", "D", 1)
        )

        val localDao = FakeSpotifyDao(
            links = listOf(
                SpotifyLink(1, "wRYMoiM19LRkOJt9PmbTaG", 0),
                SpotifyLink(2, "ZEIul98mdUL4rFtTj6u0m5", 0),
                SpotifyLink(3, "oC6CfwQNurKxuDMAPFi4GC", 0),
                SpotifyLink(4, "GMQwHtRCwNz1iljZIr8tIV", 0),
                SpotifyLink(5, "sF8v98pUor1SnL3s9Gaxev", 0)
            ),
            features = listOf(
                TrackFeature(
                    id = "wRYMoiM19LRkOJt9PmbTaG",
                    key = Pitch.D,
                    mode = MusicalMode.MAJOR,
                    tempo = 133f,
                    signature = 4,
                    loudness = -4f,
                    acousticness = 0.1f,
                    danceability = 0.8f,
                    energy = 0.9f,
                    instrumentalness = 0.1f,
                    liveness = 0.2f,
                    speechiness = 0f,
                    valence = 0.8f
                ),
                TrackFeature(
                    id = "ZEIul98mdUL4rFtTj6u0m5",
                    key = Pitch.G,
                    mode = MusicalMode.MAJOR,
                    tempo = 90f,
                    signature = 4,
                    loudness = -23f,
                    acousticness = 0.8f,
                    danceability = 0.4f,
                    energy = 0.5f,
                    instrumentalness = 0.2f,
                    liveness = 0.3f,
                    speechiness = 0f,
                    valence = 0.6f
                ),
                TrackFeature(
                    id = "oC6CfwQNurKxuDMAPFi4GC",
                    key = Pitch.D,
                    mode = MusicalMode.MAJOR,
                    tempo = 101f,
                    signature = 4,
                    loudness = -12f,
                    acousticness = 0f,
                    danceability = 0.5f,
                    energy = 0.7f,
                    instrumentalness = 0f,
                    liveness = 0f,
                    speechiness = 0f,
                    valence = 0.9f
                ),
                TrackFeature(
                    id = "GMQwHtRCwNz1iljZIr8tIV",
                    key = Pitch.B_FLAT,
                    mode = MusicalMode.MINOR,
                    tempo = 145f,
                    signature = 4,
                    loudness = -7f,
                    acousticness = 0f,
                    danceability = 0.4f,
                    energy = 0.9f,
                    instrumentalness = 0.3f,
                    liveness = 0.1f,
                    speechiness = 0.1f,
                    valence = 0.2f
                ),
                TrackFeature(
                    id = "sF8v98pUor1SnL3s9Gaxev",
                    key = Pitch.D,
                    mode = MusicalMode.MAJOR,
                    tempo = 60f,
                    signature = 4,
                    loudness = -15f,
                    acousticness = 0f,
                    danceability = 0.2f,
                    energy = 0.76f,
                    instrumentalness = 0f,
                    liveness = 0f,
                    speechiness = 0f,
                    valence = 0.4f
                )
            )
        )

        val manager = SpotifyManagerImpl(repository, OfflineSpotifyService, localDao, clock)

        val happyTrackIds =
            manager.findTracksHavingFeatures(listOf(happyFilter)).map { (track, _) -> track.id }
        happyTrackIds.shouldContainExactlyInAnyOrder(1L, 2L, 3L)

        val filters = listOf(dMajorFilter, moderatoFilter, happyFilter)
        val happyModeratoDMajorTrackIds =
            manager.findTracksHavingFeatures(filters).map { (track, _) -> track.id }
        happyModeratoDMajorTrackIds.shouldContainExactlyInAnyOrder(3)
    }

    @Test
    fun `When finding tracks by feature, then return tracks in the repository order`() = runTest {
        val repository = FakeMediaDao(
            sampleTrack(3, "A", "B", "B", 2),
            sampleTrack(4, "B", "C", "C", 7),
            sampleTrack(2, "J", "B", "B", 1),
            sampleTrack(5, "U", "C", "D", 3),
            sampleTrack(1, "Z", "A", "A", 1)
        )

        val localDao = FakeSpotifyDao(
            links = listOf(
                SpotifyLink(2, "ZEIul98mdUL4rFtTj6u0m5", 0),
                SpotifyLink(4, "GMQwHtRCwNz1iljZIr8tIV", 0),
                SpotifyLink(3, "oC6CfwQNurKxuDMAPFi4GC", 0),
                SpotifyLink(5, "sF8v98pUor1SnL3s9Gaxev", 0),
                SpotifyLink(1, "wRYMoiM19LRkOJt9PmbTaG", 0)
            ),
            features = listOf(
                TrackFeature(
                    id = "oC6CfwQNurKxuDMAPFi4GC",
                    key = Pitch.D,
                    mode = MusicalMode.MAJOR,
                    tempo = 101f,
                    signature = 4,
                    loudness = -12f,
                    acousticness = 0f,
                    danceability = 0.5f,
                    energy = 0.7f,
                    instrumentalness = 0f,
                    liveness = 0f,
                    speechiness = 0f,
                    valence = 0.9f
                ),
                TrackFeature(
                    id = "GMQwHtRCwNz1iljZIr8tIV",
                    key = Pitch.B_FLAT,
                    mode = MusicalMode.MINOR,
                    tempo = 145f,
                    signature = 4,
                    loudness = -7f,
                    acousticness = 0f,
                    danceability = 0.4f,
                    energy = 0.9f,
                    instrumentalness = 0.3f,
                    liveness = 0.1f,
                    speechiness = 0.1f,
                    valence = 0.2f
                ),
                TrackFeature(
                    id = "wRYMoiM19LRkOJt9PmbTaG",
                    key = Pitch.D,
                    mode = MusicalMode.MAJOR,
                    tempo = 133f,
                    signature = 4,
                    loudness = -4f,
                    acousticness = 0.1f,
                    danceability = 0.8f,
                    energy = 0.9f,
                    instrumentalness = 0.1f,
                    liveness = 0.2f,
                    speechiness = 0f,
                    valence = 0.8f
                ),
                TrackFeature(
                    id = "sF8v98pUor1SnL3s9Gaxev",
                    key = Pitch.D,
                    mode = MusicalMode.MAJOR,
                    tempo = 60f,
                    signature = 4,
                    loudness = -15f,
                    acousticness = 0f,
                    danceability = 0.2f,
                    energy = 0.76f,
                    instrumentalness = 0f,
                    liveness = 0f,
                    speechiness = 0f,
                    valence = 0.4f
                ),
                TrackFeature(
                    id = "ZEIul98mdUL4rFtTj6u0m5",
                    key = Pitch.G,
                    mode = MusicalMode.MAJOR,
                    tempo = 90f,
                    signature = 4,
                    loudness = -23f,
                    acousticness = 0.8f,
                    danceability = 0.4f,
                    energy = 0.5f,
                    instrumentalness = 0.2f,
                    liveness = 0.3f,
                    speechiness = 0f,
                    valence = 0.6f
                )
            )
        )

        val manager = SpotifyManagerImpl(repository, OfflineSpotifyService, localDao, clock)
        val tracks = manager.findTracksHavingFeatures(emptyList())

        tracks.map { it.first.title }.shouldContainExactly("A", "B", "J", "U", "Z")
    }

    @Test
    fun `When listing unlinked tracks, then return tracks not mapped to Spotify`() = runTest {
        val dao = FakeMediaDao(
            sampleTrack(3, "A", "B", "B", 2),
            sampleTrack(4, "B", "C", "C", 7),
            sampleTrack(2, "J", "B", "B", 1),
            sampleTrack(5, "U", "C", "D", 3),
            sampleTrack(1, "Z", "A", "A", 1)
        )

        val localDao = FakeSpotifyDao(
            links = listOf(
                SpotifyLink(2, "ZEIul98mdUL4rFtTj6u0m5", 0),
                SpotifyLink(4, "GMQwHtRCwNz1iljZIr8tIV", 0)
            ),
            features = listOf(
                TrackFeature(
                    id = "GMQwHtRCwNz1iljZIr8tIV",
                    key = Pitch.B_FLAT,
                    mode = MusicalMode.MINOR,
                    tempo = 145f,
                    signature = 4,
                    loudness = -7f,
                    acousticness = 0f,
                    danceability = 0.4f,
                    energy = 0.9f,
                    instrumentalness = 0.3f,
                    liveness = 0.1f,
                    speechiness = 0.1f,
                    valence = 0.2f
                ),
                TrackFeature(
                    id = "ZEIul98mdUL4rFtTj6u0m5",
                    key = Pitch.G,
                    mode = MusicalMode.MAJOR,
                    tempo = 90f,
                    signature = 4,
                    loudness = -23f,
                    acousticness = 0.8f,
                    danceability = 0.4f,
                    energy = 0.5f,
                    instrumentalness = 0.2f,
                    liveness = 0.3f,
                    speechiness = 0f,
                    valence = 0.6f
                )
            )
        )

        val manager = SpotifyManagerImpl(dao, OfflineSpotifyService, localDao, clock)

        val unlinkedTracks = manager.listUnlinkedTracks()
        extracting(unlinkedTracks, Track::id).shouldContainExactly(3L, 5L, 1L)
    }

    private fun sampleTrack(
        id: Long,
        title: String,
        artist: String,
        album: String,
        trackNumber: Int,
        discNumber: Int = 1
    ): Track =
        Track(id, title, artist, album, 0L, discNumber, trackNumber, "", null, 0L, 1L, 1L, 0L)
}
