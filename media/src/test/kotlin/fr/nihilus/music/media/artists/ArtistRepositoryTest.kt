/*
 * Copyright 2022 Thibault Seisel
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

package fr.nihilus.music.media.artists

import app.cash.turbine.test
import fr.nihilus.music.core.test.coroutines.flow.infiniteFlowOf
import fr.nihilus.music.media.artists.Artists.Alestorm
import fr.nihilus.music.media.artists.Artists.FooFighters
import fr.nihilus.music.media.tracks.TrackRepository
import fr.nihilus.music.media.tracks.Tracks.Cartagena
import fr.nihilus.music.media.tracks.Tracks.DirtyWater
import fr.nihilus.music.media.tracks.Tracks.MatterOfTime
import fr.nihilus.music.media.tracks.Tracks.Run
import fr.nihilus.music.media.tracks.Tracks.ThePretenders
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

private val ARTISTS = listOf(Alestorm, FooFighters)
private val TRACKS = listOf(Cartagena, DirtyWater, MatterOfTime, ThePretenders, Run)

internal class ArtistRepositoryTest {
    @MockK private lateinit var mockArtists: ArtistLocalSource
    @MockK private lateinit var mockTracks: TrackRepository

    private lateinit var repository: ArtistRepository

    @BeforeTest
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        repository = ArtistRepository(
            artistSource = mockArtists,
            trackRepository = mockTracks,
        )
    }

    @Test
    fun `artists - returns all artists from local source`() = runTest {
        every { mockArtists.artists } returns infiniteFlowOf(ARTISTS)
        every { mockTracks.tracks } returns infiniteFlowOf(TRACKS)

        val artists = repository.artists.first()

        artists.shouldContainExactly(Alestorm, FooFighters)
    }

    @Test
    fun `artists - omits artists having no tracks`() = runTest {
        every { mockArtists.artists } returns infiniteFlowOf(ARTISTS)
        every { mockTracks.tracks } returns infiniteFlowOf(TRACKS - Cartagena)

        val artists = repository.artists.first()

        artists.shouldContainExactly(FooFighters)
    }

    @Test
    fun `artists - recomputes album and track count`() = runTest {
        every { mockArtists.artists } returns infiniteFlowOf(ARTISTS)
        every { mockTracks.tracks } returns infiniteFlowOf(TRACKS - MatterOfTime)

        val artists = repository.artists.first()

        artists.shouldContainExactly(
            Alestorm,
            FooFighters.copy(trackCount = 3, albumCount = 2),
        )
    }

    @Test
    fun `artists - emits whenever artists or tracks change`() = runTest {
        val liveArtists = MutableStateFlow(ARTISTS)
        val liveTracks = MutableStateFlow(TRACKS)
        every { mockArtists.artists } returns liveArtists
        every { mockTracks.tracks } returns liveTracks

        repository.artists.drop(1).test {
            liveArtists.value -= Alestorm
            awaitItem()

            liveTracks.value -= Cartagena
            awaitItem()

            expectNoEvents()
        }
    }
}
