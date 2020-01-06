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
import fr.nihilus.music.spotify.model.AudioFeature
import fr.nihilus.music.spotify.model.SpotifyTrack
import io.kotlintest.matchers.collections.shouldContain
import org.junit.Rule
import kotlin.test.Test

class SpotifyManagerTest {

    @get:Rule
    val test = CoroutineTestRule()

    private val clock = TestClock(123456789L)
    private val dispatchers = AppDispatchers(test.dispatcher)

    @Test
    fun `When syncing tracks, then create a link to the spotify ID for each`() = test.run {
        val localDao = FakeSpotifyDao()
        val repository = FakeMediaRepository(
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
}

private fun sampleTrack(
    id: Long,
    title: String,
    artist: String,
    album: String,
    trackNumber: Int,
    discNumber: Int = 1
): Track = Track(id, title, artist, album, 0L, discNumber, trackNumber, "", null, 0L, 1L, 1L, 0L)