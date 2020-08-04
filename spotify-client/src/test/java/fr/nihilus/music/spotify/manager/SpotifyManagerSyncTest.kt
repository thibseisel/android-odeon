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
import fr.nihilus.music.media.provider.Track
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlinx.coroutines.test.runBlockingTest
import kotlin.test.Test

internal class SpotifyManagerSyncTest {

    private val clock = TestClock(123456789L)

    @Test
    fun `Given properly tagged tracks, then create a link to the spotify ID for each`() = runBlockingTest {
        val localDao = FakeSpotifyDao()
        val repository = FakeMediaDao(
            track(294, "Algorithm", "Muse", "Simulation Theory", 1),
            track(295, "The Dark Side", "Muse", "Simulation Theory", 2),
            track(165, "Dirty Water", "Foo Fighters", "Concrete and Gold", 6)
        )

        val manager = SpotifyManagerImpl(repository, InMemorySpotifyService, localDao, clock)
        manager.sync()

        localDao.links.shouldContainExactlyInAnyOrder(
            SpotifyLink(294, "7f0vVL3xi4i78Rv5Ptn2s1", 123456789L),
            SpotifyLink(295, "0dMYPDqcI4ca4cjqlmp9mE", 123456789L),
            SpotifyLink(165, "5lnsL7pCg0fQKcWnlkD1F0", 123456789L)
        )
        localDao.features.shouldContainExactlyInAnyOrder(
            TrackFeature("7f0vVL3xi4i78Rv5Ptn2s1", Pitch.D, MusicalMode.MAJOR, 170.057f, 4, -4.56f, 0.0125f, 0.522f, 0.923f, 0.017f, 0.0854f, 0.0539f, 0.595f),
            TrackFeature("0dMYPDqcI4ca4cjqlmp9mE", Pitch.D, MusicalMode.MAJOR, 99.979f, 4, -3.759f, 0.000884f, 0.484f, 0.927f, 0.00000396f, 0.223f, 0.0425f, 0.389f),
            TrackFeature("5lnsL7pCg0fQKcWnlkD1F0", Pitch.G, MusicalMode.MAJOR, 142.684f, 4, -8.245f, 0.00365f, 0.324f, 0.631f, 0.0459f, 0.221f, 0.0407f, 0.346f)
        )
    }

    @Test
    fun `It should dissociate tracks with similar title`() = runBlockingTest {
        val localDao = FakeSpotifyDao()
        val repository = FakeMediaDao(
            track(299, "Something Human", "Muse", "Simulation Theory", 6),
            track(306, "Something Human (Acoustic)", "Muse", "Simulation Theory", 15)
        )

        val manager = SpotifyManagerImpl(repository, InMemorySpotifyService, localDao, clock)
        manager.sync()

        localDao.links.shouldContainExactlyInAnyOrder(
            SpotifyLink(299, "1esX5rtwwssnsEQNQk0HGg", 123456789L),
            SpotifyLink(306, "1D2ISRyHAs9QBHIWVQIbgM", 123456789L)
        )
        localDao.features.shouldContainExactlyInAnyOrder(
            TrackFeature("1esX5rtwwssnsEQNQk0HGg", Pitch.A, MusicalMode.MAJOR, 105.018f, 4, -4.841f, 0.0536f, 0.59f, 0.903f, 0.00289f, 0.12f, 0.0542f,0.545f),
            TrackFeature("1D2ISRyHAs9QBHIWVQIbgM", Pitch.A, MusicalMode.MAJOR, 105.023f, 4, -5.284f, 0.425f, 0.609f, 0.832f, 0.000044f, 0.17f, 0.039f, 0.654f)
        )
    }

    @Test
    fun `When no track matched, then create no link`() = runBlockingTest {
        val localDao = FakeSpotifyDao()
        val repository = FakeMediaDao(
            track(294, "Mechanical Rhythm", "ACE+", "Xenoblade Original Soundtrack", 15)
        )

        val manager = SpotifyManagerImpl(repository, InMemorySpotifyService, localDao, clock)
        manager.sync()

        localDao.links.shouldBeEmpty()
        localDao.features.shouldBeEmpty()
    }

    @Test
    fun `Given deleted tracks, then delete corresponding links`() = runBlockingTest {
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
            track(134, "Track", "Artist", "Album", 1)
        )

        val manager = SpotifyManagerImpl(repository, OfflineSpotifyService, localDao, clock)
        manager.sync()

        localDao.links.map { it.trackId }.shouldContainExactly(134)
        localDao.features.map { it.id }.shouldContainExactly("NNcqs3H84QCpqpJXF5WCly")
    }

    private fun track(
        id: Long,
        title: String,
        artist: String,
        album: String,
        trackNumber: Int
    ): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = 0,
        discNumber = 1,
        trackNumber = trackNumber,
        mediaUri = "",
        albumArtUri = null,
        availabilityDate = 0,
        artistId = artist.hashCode().toLong(),
        albumId = album.hashCode().toLong(),
        fileSize = 0
    )
}