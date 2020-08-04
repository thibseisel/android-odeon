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
import fr.nihilus.music.spotify.model.AudioFeature
import fr.nihilus.music.spotify.model.SpotifyTrack
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldContain
import io.kotlintest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.test.runBlockingTest
import kotlin.test.Test

internal class SpotifyManagerSyncTest {

    private val clock = TestClock(123456789L)

    @Test
    fun `When syncing tracks, then create a link to the spotify ID for each`() = runBlockingTest {
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

        val manager = SpotifyManagerImpl(repository, service, localDao, clock)
        manager.sync()

        localDao.links shouldContain SpotifyLink(294, "7f0vVL3xi4i78Rv5Ptn2s1", 123456789L)
        localDao.features shouldContain TrackFeature("7f0vVL3xi4i78Rv5Ptn2s1", Pitch.D, MusicalMode.MAJOR, 170.057f, 4, -4.56f, 0.0125f, 0.522f, 0.923f, 0.017f, 0.0854f, 0.0539f, 0.595f)
    }

    @Test
    fun `When syncing and no track matched, then create no link`() = runBlockingTest {
        val localDao = FakeSpotifyDao()
        val repository = FakeMediaDao(
            sampleTrack(294, "Algorithm", "Muse", "Simulation Theory", 1, 1)
        )

        val service = FakeSpotifyService(
            tracks = emptyList(),
            features = emptyList()
        )

        val manager = SpotifyManagerImpl(repository, service, localDao, clock)
        manager.sync()

        localDao.links.shouldBeEmpty()
        localDao.features.shouldBeEmpty()
    }

    @Test
    fun `When syncing and features are not found, then create no link`() = runBlockingTest {
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

        val manager = SpotifyManagerImpl(repository, service, localDao, clock)
        manager.sync()

        localDao.links.shouldBeEmpty()
        localDao.features.shouldBeEmpty()
    }

    @Test
    fun `When syncing, then delete links for tracks that have been deleted`() = runBlockingTest {
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

        val manager = SpotifyManagerImpl(repository, OfflineSpotifyService, localDao, clock)
        manager.sync()

        localDao.links.map { it.trackId }.shouldContainExactly(134)
        localDao.features.map { it.id }.shouldContainExactly("NNcqs3H84QCpqpJXF5WCly")
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