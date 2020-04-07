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

import fr.nihilus.music.core.context.AppDispatchers
import fr.nihilus.music.core.database.spotify.MusicalMode
import fr.nihilus.music.core.database.spotify.Pitch
import fr.nihilus.music.core.database.spotify.SpotifyLink
import fr.nihilus.music.core.database.spotify.TrackFeature
import fr.nihilus.music.core.test.coroutines.CoroutineTestRule
import fr.nihilus.music.core.test.os.TestClock
import fr.nihilus.music.media.provider.Track
import fr.nihilus.music.spotify.model.AudioFeature
import fr.nihilus.music.spotify.model.SpotifyTrack
import io.kotlintest.extracting
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldContain
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotlintest.shouldNotThrowAny
import org.junit.Rule
import kotlin.random.Random
import kotlin.test.Test

internal class SpotifyManagerTest {

    @get:Rule
    val test = CoroutineTestRule()

    private val clock = TestClock(123456789L)
    private val dispatchers = AppDispatchers(test.dispatcher)

    @Test
    fun `When syncing tracks, then create a link to the spotify ID for each`() = test.run {
        val localDao = FakeSpotifyDao()
        val repository = FakeMediaDao(
            sampleTrack(294, "Algorithm", "Muse", "Simulation Theory", 1, 1)
        )

        val service = FakeSpotifyService(
            tracks = listOf(
                SpotifyTrack("7f0vVL3xi4i78Rv5Ptn2s1", "Algorithm", 1, 1, 245960, false)
            ),
            features = listOf(
                AudioFeature("7f0vVL3xi4i78Rv5Ptn2s1", 2, 1, 170.057f, 4, -4.56f, 0.0125f, 0.522f, 0.923f, 0.017f, 0.0854f, 0.0539f, 0.595f)
            )
        )

        val manager = SpotifyManagerImpl(repository, service, localDao, dispatchers, clock)
        manager.sync()

        localDao.links shouldContain SpotifyLink(294, "7f0vVL3xi4i78Rv5Ptn2s1", 123456789L)
        localDao.features shouldContain TrackFeature("7f0vVL3xi4i78Rv5Ptn2s1", Pitch.D, MusicalMode.MAJOR, 170.057f, 4, -4.56f, 0.0125f, 0.522f, 0.923f, 0.017f, 0.0854f, 0.0539f, 0.595f)
    }

    @Test
    fun `When syncing and no track matched, then create no link`() = test.run {
        val localDao = FakeSpotifyDao()
        val repository = FakeMediaDao(
            sampleTrack(294, "Algorithm", "Muse", "Simulation Theory", 1, 1)
        )

        val service = FakeSpotifyService(
            tracks = emptyList(),
            features = emptyList()
        )

        val manager = SpotifyManagerImpl(repository, service, localDao, dispatchers, clock)
        manager.sync()

        localDao.links.shouldBeEmpty()
        localDao.features.shouldBeEmpty()
    }

    @Test
    fun `When syncing and features are not found, then create no link`() = test.run {
        val localDao = FakeSpotifyDao()
        val repository = FakeMediaDao(
            sampleTrack(294, "Algorithm", "Muse", "Simulation Theory", 1, 1)
        )

        val service = FakeSpotifyService(
            tracks = listOf(
                SpotifyTrack("7f0vVL3xi4i78Rv5Ptn2s1", "Algorithm", 1, 1, 245960, false)
            ),
            features = emptyList()
        )

        val manager = SpotifyManagerImpl(repository, service, localDao, dispatchers, clock)
        manager.sync()

        localDao.links.shouldBeEmpty()
        localDao.features.shouldBeEmpty()
    }

    @Test
    fun `When syncing, then delete links for tracks that have been deleted`() = test.run {
        val localDao = FakeSpotifyDao(
            links = listOf(
                SpotifyLink(289, "MH5U9eiW1fgFukImkVf9cq", 0L),
                SpotifyLink(134, "NNcqs3H84QCpqpJXF5WCly", 0L),
                SpotifyLink(879, "MRngff0u5EMK5kEOm62c6P", 0L)
            ),
            features = listOf(
                TrackFeature("NNcqs3H84QCpqpJXF5WCly", null, MusicalMode.MINOR, 80f, 4, -60f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
                TrackFeature("MH5U9eiW1fgFukImkVf9cq", null, MusicalMode.MINOR, 80f, 4, -60f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
                TrackFeature("MRngff0u5EMK5kEOm62c6P", null, MusicalMode.MINOR, 80f, 4, -60f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
            )
        )

        val repository = FakeMediaDao(
            sampleTrack(134, "Track", "Artist", "Album", 1)
        )

        val manager = SpotifyManagerImpl(repository, OfflineSpotifyService, localDao, dispatchers, clock)
        manager.sync()

        localDao.links.map { it.trackId }.shouldContainExactly(134)
        localDao.features.map { it.id }.shouldContainExactly("NNcqs3H84QCpqpJXF5WCly")
    }

    @Test
    fun `When syncing more than 100 tracks at once, then sync should not fail`() = test.run {
        val sampleTracks = generateDummyTracks().take(200).toList()
        val spotifyIds = randomSpotifyIds().take(200).toList()

        val remoteTracks = spotifyIds.map { id ->
            SpotifyTrack(id, "Title", 1, 1, 0, false)
        }
        val sampleFeatures = spotifyIds.map { id ->
            AudioFeature(id, null, 1, 120f, 4, -10f, .5f, .5f, .5f, .5f, .5f, .5f, .5f)
        }

        val localDao = FakeSpotifyDao()
        val repository = FakeMediaDao(*sampleTracks.toTypedArray())

        val service = FakeSpotifyService(
            tracks = remoteTracks,
            features = sampleFeatures
        )

        val manager = SpotifyManagerImpl(repository, service, localDao, dispatchers, clock)
        shouldNotThrowAny {
            manager.sync()
        }
    }

    private fun generateDummyTracks() = sequence<Track> {
        var trackId = 0L

        while (true) {
            yield(
                Track(
                    id = trackId++,
                    title ="Title",
                    artist ="Artist",
                    album ="Album",
                    duration = 0,
                    discNumber = 0,
                    trackNumber = 1,
                    mediaUri = "",
                    albumArtUri = null,
                    availabilityDate = 0,
                    artistId = 0,
                    albumId = 0,
                    fileSize = 1_000_000
                )
            )
        }
    }

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

        val manager = SpotifyManagerImpl(repository, OfflineSpotifyService, localDao, dispatchers, clock)

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
                TrackFeature("GMQwHtRCwNz1iljZIr8tIV", Pitch.A_SHARP, MusicalMode.MINOR, 145f, 4, -7f, 0f, 0.4f, 0.9f, 0.3f, 0.1f, 0.1f, 0.2f),
                TrackFeature("sF8v98pUor1SnL3s9Gaxev", Pitch.D, MusicalMode.MAJOR, 60f, 4, -15f, 0f, 0.2f, 0.76f, 0f, 0f, 0f, 0.4f)
            )
        )

        val manager = SpotifyManagerImpl(repository, OfflineSpotifyService, localDao, dispatchers, clock)

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
                TrackFeature("GMQwHtRCwNz1iljZIr8tIV", Pitch.A_SHARP, MusicalMode.MINOR, 145f, 4, -7f, 0f, 0.4f, 0.9f, 0.3f, 0.1f, 0.1f, 0.2f),
                TrackFeature("wRYMoiM19LRkOJt9PmbTaG", Pitch.D, MusicalMode.MAJOR, 133f, 4, -4f, 0.1f, 0.8f, 0.9f, 0.1f, 0.2f, 0f, 0.8f),
                TrackFeature("sF8v98pUor1SnL3s9Gaxev", Pitch.D, MusicalMode.MAJOR, 60f, 4, -15f, 0f, 0.2f, 0.76f, 0f, 0f, 0f, 0.4f),
                TrackFeature("ZEIul98mdUL4rFtTj6u0m5", Pitch.G, MusicalMode.MAJOR, 90f, 4, -23f, 0.8f, 0.4f, 0.5f, 0.2f, 0.3f, 0f, 0.6f)
            )
        )

        val manager = SpotifyManagerImpl(repository, OfflineSpotifyService, localDao, dispatchers, clock)
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
                TrackFeature("GMQwHtRCwNz1iljZIr8tIV", Pitch.A_SHARP, MusicalMode.MINOR, 145f, 4, -7f, 0f, 0.4f, 0.9f, 0.3f, 0.1f, 0.1f, 0.2f),
                TrackFeature("ZEIul98mdUL4rFtTj6u0m5", Pitch.G, MusicalMode.MAJOR, 90f, 4, -23f, 0.8f, 0.4f, 0.5f, 0.2f, 0.3f, 0f, 0.6f)
            )
        )

        val manager = SpotifyManagerImpl(dao, OfflineSpotifyService, localDao, dispatchers, clock)

        val unlinkedTracks = manager.listUnlinkedTracks()
        extracting(unlinkedTracks, Track::id).shouldContainExactly(3L, 5L, 1L)
    }
}

private fun randomSpotifyIds(): Sequence<String> = sequence {
    val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    val buffer = StringBuilder()

    while(true) {
        buffer.setLength(0)
        repeat(22) { index ->
            val randomCharIndex = Random.nextInt(0, chars.length)
            buffer.append(index, chars[randomCharIndex])
        }

        yield(buffer.toString())
    }
}

private fun sampleTrack(
    id: Long,
    title: String,
    artist: String,
    album: String,
    trackNumber: Int,
    discNumber: Int = 1
): Track = Track(id, title, artist, album, 0L, discNumber, trackNumber, "", null, 0L, 1L, 1L, 0L)