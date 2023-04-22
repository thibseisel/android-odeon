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
import fr.nihilus.music.core.database.spotify.SpotifyDao
import fr.nihilus.music.core.database.spotify.SpotifyLink
import fr.nihilus.music.core.database.spotify.TrackFeature
import fr.nihilus.music.core.test.coroutines.flow.infiniteFlowOf
import fr.nihilus.music.core.test.os.TestClock
import fr.nihilus.music.media.tracks.Track
import fr.nihilus.music.media.tracks.TrackRepository
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class SpotifyManagerSyncTest {
    private val clock = TestClock(123456789L)

    @MockK private lateinit var mockSpotifyDao: SpotifyDao
    @MockK private lateinit var mockTracks: TrackRepository

    private lateinit var manager: SpotifyManager

    @BeforeTest
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        manager = SpotifyManagerImpl(mockTracks, InMemorySpotifyService, mockSpotifyDao, clock)
    }

    @Test
    fun `Given properly tagged tracks, then create a link to the spotify ID for each`() = runTest {
        coEvery { mockSpotifyDao.getLinks() } returns emptyList()
        every { mockTracks.tracks } returns infiniteFlowOf(
            listOf(
                track(294, "Algorithm", "Muse", "Simulation Theory", 1),
                track(295, "The Dark Side", "Muse", "Simulation Theory", 2),
                track(165, "Dirty Water", "Foo Fighters", "Concrete and Gold", 6)
            )
        )

        manager.sync().collect()

        coVerify {
            mockSpotifyDao.saveTrackFeature(
                SpotifyLink(294, "7f0vVL3xi4i78Rv5Ptn2s1", 123456789L),
                TrackFeature(
                    id = "7f0vVL3xi4i78Rv5Ptn2s1",
                    key = Pitch.D,
                    mode = MusicalMode.MAJOR,
                    tempo = 170.057f,
                    signature = 4,
                    loudness = -4.56f,
                    acousticness = 0.0125f,
                    danceability = 0.522f,
                    energy = 0.923f,
                    instrumentalness = 0.017f,
                    liveness = 0.0854f,
                    speechiness = 0.0539f,
                    valence = 0.595f
                )
            )
            mockSpotifyDao.saveTrackFeature(
                SpotifyLink(295, "0dMYPDqcI4ca4cjqlmp9mE", 123456789L),
                TrackFeature(
                    id = "0dMYPDqcI4ca4cjqlmp9mE",
                    key = Pitch.D,
                    mode = MusicalMode.MAJOR,
                    tempo = 99.979f,
                    signature = 4,
                    loudness = -3.759f,
                    acousticness = 0.000884f,
                    danceability = 0.484f,
                    energy = 0.927f,
                    instrumentalness = 0.00000396f,
                    liveness = 0.223f,
                    speechiness = 0.0425f,
                    valence = 0.389f
                )
            )
            mockSpotifyDao.saveTrackFeature(
                SpotifyLink(165, "5lnsL7pCg0fQKcWnlkD1F0", 123456789L),
                TrackFeature(
                    id = "5lnsL7pCg0fQKcWnlkD1F0",
                    key = Pitch.G,
                    mode = MusicalMode.MAJOR,
                    tempo = 142.684f,
                    signature = 4,
                    loudness = -8.245f,
                    acousticness = 0.00365f,
                    danceability = 0.324f,
                    energy = 0.631f,
                    instrumentalness = 0.0459f,
                    liveness = 0.221f,
                    speechiness = 0.0407f,
                    valence = 0.346f
                )
            )
        }
    }

    @Test
    fun `It should dissociate tracks with similar title`() = runTest {
        coEvery { mockSpotifyDao.getLinks() } returns emptyList()
        every { mockTracks.tracks } returns infiniteFlowOf(
            listOf(
                track(299, "Something Human", "Muse", "Simulation Theory", 6),
                track(306, "Something Human (Acoustic)", "Muse", "Simulation Theory", 15)
            )
        )

        manager.sync().collect()

        coVerify {
            mockSpotifyDao.saveTrackFeature(
                SpotifyLink(299, "1esX5rtwwssnsEQNQk0HGg", 123456789L),
                TrackFeature(
                    id = "1esX5rtwwssnsEQNQk0HGg",
                    key = Pitch.A,
                    mode = MusicalMode.MAJOR,
                    tempo = 105.018f,
                    signature = 4,
                    loudness = -4.841f,
                    acousticness = 0.0536f,
                    danceability = 0.59f,
                    energy = 0.903f,
                    instrumentalness = 0.00289f,
                    liveness = 0.12f,
                    speechiness = 0.0542f,
                    valence = 0.545f
                )
            )
            mockSpotifyDao.saveTrackFeature(
                SpotifyLink(306, "1D2ISRyHAs9QBHIWVQIbgM", 123456789L),
                TrackFeature(
                    "1D2ISRyHAs9QBHIWVQIbgM",
                    Pitch.A,
                    MusicalMode.MAJOR,
                    105.023f,
                    4,
                    -5.284f,
                    0.425f,
                    0.609f,
                    0.832f,
                    0.000044f,
                    0.17f,
                    0.039f,
                    0.654f
                )
            )
        }
    }

    @Test
    fun `When no track matched, then create no link`() = runTest {
        coEvery { mockSpotifyDao.getLinks() } returns emptyList()
        every { mockTracks.tracks } returns infiniteFlowOf(
            listOf(
                track(294, "Mechanical Rhythm", "ACE+", "Xenoblade Original Soundtrack", 15)
            )
        )

        manager.sync().collect()

        coVerify(inverse = true) {
            mockSpotifyDao.saveTrackFeature(any(), any())
        }
    }

    @Test
    fun `Given deleted tracks, then delete corresponding links`() = runTest {
        coEvery { mockSpotifyDao.getLinks() } returns listOf(
            SpotifyLink(289, "MH5U9eiW1fgFukImkVf9cq", 0L),
            SpotifyLink(134, "NNcqs3H84QCpqpJXF5WCly", 0L),
            SpotifyLink(879, "MRngff0u5EMK5kEOm62c6P", 0L)
        )
        every { mockTracks.tracks } returns infiniteFlowOf(
            listOf(
                track(134, "Track", "Artist", "Album", 1)
            )
        )

        manager.sync().collect()

        coVerify {
            mockSpotifyDao.deleteLinks(longArrayOf(289, 879))
        }
    }

    @Test
    fun `Returned flow should report progress`() = runTest {
        coEvery { mockSpotifyDao.getLinks() } returns emptyList()
        every { mockTracks.tracks } returns infiniteFlowOf(
            listOf(
                track(294, "Algorithm", "Muse", "Simulation Theory", 1),
                track(165, "Dirty Water", "Foo Fighters", "Concrete and Gold", 6),
                track(294, "Mechanical Rhythm", "ACE+", "Xenoblade Original Soundtrack", 15),
                track(295, "The Dark Side", "Muse", "Simulation Theory", 2)
            )
        )

        val progressEvents = manager.sync().toList()

        progressEvents.shouldContainExactly(
            SyncProgress(0, 0, 4),
            SyncProgress(1, 0, 4),
            SyncProgress(2, 0, 4),
            SyncProgress(2, 1, 4),
            SyncProgress(3, 1, 4)
        )
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
        fileSize = 0,
        exclusionTime = null,
    )
}
