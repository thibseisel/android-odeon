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
import fr.nihilus.music.core.test.coroutines.CoroutineTestRule
import fr.nihilus.music.core.test.os.TestClock
import fr.nihilus.music.media.provider.Track
import io.kotlintest.extracting
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.Rule
import kotlin.test.Test

internal class SpotifyManagerTest {

    @get:Rule
    val test = CoroutineTestRule()

    private val clock = TestClock(123456789L)

    @Test
    fun `Given no filter, when finding tracks by feature then only return linked tracks`() = test.run {
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
                TrackFeature("EAzDgVCnoZnJZjIq1Bx4FW", Pitch.D, MusicalMode.MAJOR, 100f, 4, -13f, 0.04f, 0.2f, 0.7f, 0.1f, 0.2f, 0.08f, 0.75f),
                TrackFeature("vJy3wp8BworPoz6o30oDxy", Pitch.A, MusicalMode.MINOR, 82f, 4, -8f, 0f, 0.4f, 0.9f, 0.1f, 0.1f, 0f, 0.3f)
            )
        )

        val manager = SpotifyManagerImpl(repository, OfflineSpotifyService, localDao, clock)

        val tracksIds = manager.findTracksHavingFeatures(emptyList()).map { (track, _) -> track.id }
        tracksIds.shouldContainExactlyInAnyOrder(481L, 75L)
    }

    @Test
    fun `When finding tracks by feature then only return tracks matching all filters`() = test.run {
        val dMajorFilter = FeatureFilter.OnTone(Pitch.D, MusicalMode.MAJOR)
        val moderatoFilter = FeatureFilter.OnRange(TrackFeature::tempo,88f, 112f)
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
                TrackFeature("wRYMoiM19LRkOJt9PmbTaG", Pitch.D, MusicalMode.MAJOR, 133f, 4, -4f, 0.1f, 0.8f, 0.9f, 0.1f, 0.2f, 0f, 0.8f),
                TrackFeature("ZEIul98mdUL4rFtTj6u0m5", Pitch.G, MusicalMode.MAJOR, 90f, 4, -23f, 0.8f, 0.4f, 0.5f, 0.2f, 0.3f, 0f, 0.6f),
                TrackFeature("oC6CfwQNurKxuDMAPFi4GC", Pitch.D, MusicalMode.MAJOR, 101f, 4, -12f, 0f, 0.5f, 0.7f, 0f, 0f, 0f, 0.9f),
                TrackFeature("GMQwHtRCwNz1iljZIr8tIV", Pitch.B_FLAT, MusicalMode.MINOR, 145f, 4, -7f, 0f, 0.4f, 0.9f, 0.3f, 0.1f, 0.1f, 0.2f),
                TrackFeature("sF8v98pUor1SnL3s9Gaxev", Pitch.D, MusicalMode.MAJOR, 60f, 4, -15f, 0f, 0.2f, 0.76f, 0f, 0f, 0f, 0.4f)
            )
        )

        val manager = SpotifyManagerImpl(repository, OfflineSpotifyService, localDao, clock)

        val happyTrackIds = manager.findTracksHavingFeatures(listOf(happyFilter)).map { (track, _) -> track.id }
        happyTrackIds.shouldContainExactlyInAnyOrder(1L, 2L, 3L)

        val filters = listOf(dMajorFilter, moderatoFilter, happyFilter)
        val happyModeratoDMajorTrackIds = manager.findTracksHavingFeatures(filters).map { (track, _) -> track.id }
        happyModeratoDMajorTrackIds.shouldContainExactlyInAnyOrder(3)
    }

    @Test
    fun `When finding tracks by feature, then return tracks in the repository order`() = test.run {
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
                TrackFeature("oC6CfwQNurKxuDMAPFi4GC", Pitch.D, MusicalMode.MAJOR, 101f, 4, -12f, 0f, 0.5f, 0.7f, 0f, 0f, 0f, 0.9f),
                TrackFeature("GMQwHtRCwNz1iljZIr8tIV", Pitch.B_FLAT, MusicalMode.MINOR, 145f, 4, -7f, 0f, 0.4f, 0.9f, 0.3f, 0.1f, 0.1f, 0.2f),
                TrackFeature("wRYMoiM19LRkOJt9PmbTaG", Pitch.D, MusicalMode.MAJOR, 133f, 4, -4f, 0.1f, 0.8f, 0.9f, 0.1f, 0.2f, 0f, 0.8f),
                TrackFeature("sF8v98pUor1SnL3s9Gaxev", Pitch.D, MusicalMode.MAJOR, 60f, 4, -15f, 0f, 0.2f, 0.76f, 0f, 0f, 0f, 0.4f),
                TrackFeature("ZEIul98mdUL4rFtTj6u0m5", Pitch.G, MusicalMode.MAJOR, 90f, 4, -23f, 0.8f, 0.4f, 0.5f, 0.2f, 0.3f, 0f, 0.6f)
            )
        )

        val manager = SpotifyManagerImpl(repository, OfflineSpotifyService, localDao, clock)
        val tracks = manager.findTracksHavingFeatures(emptyList())

        tracks.map { it.first.title }.shouldContainExactly("A", "B", "J", "U", "Z")
    }

    @Test
    fun `When listing unlinked tracks, then return tracks not mapped to Spotify`() = test.run {
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
                TrackFeature("GMQwHtRCwNz1iljZIr8tIV", Pitch.B_FLAT, MusicalMode.MINOR, 145f, 4, -7f, 0f, 0.4f, 0.9f, 0.3f, 0.1f, 0.1f, 0.2f),
                TrackFeature("ZEIul98mdUL4rFtTj6u0m5", Pitch.G, MusicalMode.MAJOR, 90f, 4, -23f, 0.8f, 0.4f, 0.5f, 0.2f, 0.3f, 0f, 0.6f)
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
    ): Track = Track(id, title, artist, album, 0L, discNumber, trackNumber, "", null, 0L, 1L, 1L, 0L)
}
