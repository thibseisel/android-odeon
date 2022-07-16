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

package fr.nihilus.music.media.albums

import app.cash.turbine.test
import fr.nihilus.music.core.test.coroutines.flow.infiniteFlowOf
import fr.nihilus.music.media.albums.Albums.ConcreteAndGold
import fr.nihilus.music.media.albums.Albums.SimulationTheory
import fr.nihilus.music.media.albums.Albums.WastingLight
import fr.nihilus.music.media.artists.Artists.Alestorm
import fr.nihilus.music.media.artists.Artists.FooFighters
import fr.nihilus.music.media.artists.Artists.Muse
import fr.nihilus.music.media.tracks.*
import fr.nihilus.music.media.tracks.Tracks.Algorithm
import fr.nihilus.music.media.tracks.Tracks.DirtyWater
import fr.nihilus.music.media.tracks.Tracks.MatterOfTime
import fr.nihilus.music.media.tracks.Tracks.Run
import io.kotest.matchers.collections.shouldBeEmpty
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

private val ALBUMS = listOf(ConcreteAndGold, SimulationTheory, WastingLight)
private val TRACKS = listOf(Algorithm, DirtyWater, MatterOfTime, Run)

internal class AlbumRepositoryTest {
    @MockK private lateinit var mockAlbums: AlbumLocalSource
    @MockK private lateinit var mockTracks: TrackRepository

    private lateinit var repository: AlbumRepository

    @BeforeTest
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        repository = AlbumRepository(
            albumSource = mockAlbums,
            trackRepository = mockTracks,
        )
    }

    @Test
    fun `albums - returns all albums from local source`() = runTest {
        every { mockAlbums.albums } returns infiniteFlowOf(ALBUMS)
        every { mockTracks.tracks } returns infiniteFlowOf(TRACKS)

        val albums = repository.albums.first()

        albums.shouldContainExactly(
            ConcreteAndGold,
            SimulationTheory,
            WastingLight,
        )
    }

    @Test
    fun `albums - omits albums having no tracks`() = runTest {
        every { mockAlbums.albums } returns infiniteFlowOf(ALBUMS)
        every { mockTracks.tracks } returns infiniteFlowOf(
            TRACKS - MatterOfTime
        )

        val albums = repository.albums.first()

        albums.shouldContainExactly(ConcreteAndGold, SimulationTheory)
    }

    @Test
    fun `albums - recomputes album track count`() = runTest {
        every { mockAlbums.albums } returns infiniteFlowOf(ALBUMS)
        every { mockTracks.tracks } returns infiniteFlowOf(
            TRACKS - Run
        )

        val albums = repository.albums.first()

        albums.shouldContainExactly(
            ConcreteAndGold.copy(trackCount = 1),
            SimulationTheory,
            WastingLight,
        )
    }

    @Test
    fun `albums - emits whenever tracks or albums change`() = runTest {
        val liveAlbums = MutableStateFlow(ALBUMS)
        val liveTracks = MutableStateFlow(TRACKS)
        every { mockAlbums.albums } returns liveAlbums
        every { mockTracks.tracks } returns liveTracks

        repository.albums.drop(1).test {
            liveAlbums.value -= SimulationTheory
            awaitItem()

            liveTracks.value -= Algorithm
            awaitItem()
            expectNoEvents()
        }
    }

    @Test
    fun `getArtistAlbums - lists all albums from an artist`() = runTest {
        every { mockAlbums.albums } returns infiniteFlowOf(ALBUMS)
        every { mockTracks.tracks } returns infiniteFlowOf(TRACKS)

        val fooAlbums = repository.getArtistAlbums(FooFighters.id).first()
        val museAlbums = repository.getArtistAlbums(Muse.id).first()

        fooAlbums.shouldContainExactly(ConcreteAndGold, WastingLight)
        museAlbums.shouldContainExactly(SimulationTheory)
    }

    @Test
    fun `getArtistAlbums - returns empty list for unknown artist`() = runTest {
        every { mockAlbums.albums } returns infiniteFlowOf(ALBUMS)
        every { mockTracks.tracks } returns infiniteFlowOf(TRACKS)

        val albums = repository.getArtistAlbums(Alestorm.id).first()

        albums.shouldBeEmpty()
    }
}
