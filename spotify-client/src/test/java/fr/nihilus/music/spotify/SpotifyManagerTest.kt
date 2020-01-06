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

package fr.nihilus.music.spotify

import fr.nihilus.music.core.context.AppDispatchers
import fr.nihilus.music.core.database.spotify.*
import fr.nihilus.music.core.test.coroutines.CoroutineTestRule
import fr.nihilus.music.core.test.os.TestClock
import fr.nihilus.music.media.provider.Track
import fr.nihilus.music.media.repo.MediaRepository
import fr.nihilus.music.spotify.model.AudioFeature
import fr.nihilus.music.spotify.model.SpotifyTrack
import fr.nihilus.music.spotify.service.HttpResource
import fr.nihilus.music.spotify.service.SpotifyQuery
import fr.nihilus.music.spotify.service.SpotifyService
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import kotlin.test.BeforeTest
import kotlin.test.Test

class SpotifyManagerTest {

    @get:Rule
    val test = CoroutineTestRule()

    private val clock = TestClock(123456789L)
    private val dispatchers = AppDispatchers(test.dispatcher)

    @MockK private lateinit var repository: MediaRepository
    @MockK private lateinit var service: SpotifyService
    @MockK private lateinit var localDao: SpotifyDao

    private lateinit var manager: SpotifyManager

    @BeforeTest
    fun setupMocks() = MockKAnnotations.init(this)

    @BeforeTest
    fun setupSubject() {
        manager = SpotifyManagerImpl(repository, service, localDao, dispatchers, clock)
    }

    @Test
    fun `When syncing tracks, then create a link to the spotify ID for each`() = test.run {
        coEvery { repository.getTracks() } returns listOf(
            sampleTrack(294, "Algorithm", "Muse", "Simulation Theory", 1, 1)
        )

        coEvery { localDao.getLinks() } returns emptyList()

        every { service.search(any<SpotifyQuery.Track>()) } returns flowOf(
            SpotifyTrack("7f0vVL3xi4i78Rv5Ptn2s1", "Algorithm", 1, 1, 245960, false)
        )

        coEvery {
            service.getSeveralTrackFeatures(listOf("7f0vVL3xi4i78Rv5Ptn2s1"))
        } returns HttpResource.Loaded(
            listOf(AudioFeature("7f0vVL3xi4i78Rv5Ptn2s1", 2, 1, 170.057f, 4, -4.56f, 0.0125f, 0.522f, 0.923f, 0.017f, 0.0854f, 0.0539f, 0.595f))
        )

        coEvery { localDao.saveTrackFeature(any(), any()) } just Runs

        manager.sync()

        coVerify(exactly = 1) {
            localDao.saveTrackFeature(
                eq(SpotifyLink(294, "7f0vVL3xi4i78Rv5Ptn2s1", 123456789L)),
                eq(TrackFeature("7f0vVL3xi4i78Rv5Ptn2s1", Pitch.D, MusicalMode.MAJOR, 170.057f, 4, -4.56f, 0.0125f, 0.522f, 0.923f, 0.017f, 0.0854f, 0.0539f, 0.595f))
            )
        }
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