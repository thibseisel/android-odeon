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

package fr.nihilus.music.media.tracks

import app.cash.turbine.test
import fr.nihilus.music.core.database.exclusion.TrackExclusion
import fr.nihilus.music.core.database.exclusion.TrackExclusionDao
import fr.nihilus.music.core.test.coroutines.flow.infiniteFlowOf
import fr.nihilus.music.core.test.os.TestClock
import fr.nihilus.music.media.albums.Albums.ConcreteAndGold
import fr.nihilus.music.media.albums.Albums.SunsetOnGoldenAge
import fr.nihilus.music.media.artists.Artists.Alestorm
import fr.nihilus.music.media.artists.Artists.FooFighters
import fr.nihilus.music.media.tracks.Tracks.Cartagena
import fr.nihilus.music.media.tracks.Tracks.DirtyWater
import fr.nihilus.music.media.tracks.Tracks.IsolatedSystem
import fr.nihilus.music.media.tracks.Tracks.Run
import fr.nihilus.music.media.tracks.Tracks.ThePretenders
import fr.nihilus.music.media.tracks.local.*
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import fr.nihilus.music.media.tracks.local.LocalTracks as Local

private const val TEST_TIMESTAMP = 1650830769210L

internal class TrackRepositoryTest {
    @MockK private lateinit var mockTracks: TrackLocalSource
    @MockK private lateinit var mockExclusions: TrackExclusionDao

    private lateinit var appScope: TestScope
    private lateinit var testScope: TestScope
    private lateinit var repository: TrackRepository

    @BeforeTest
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        val scheduler = TestCoroutineScheduler()
        appScope = TestScope(UnconfinedTestDispatcher(scheduler))
        testScope = TestScope(scheduler)

        repository = TrackRepository(
            appScope = appScope,
            clock = TestClock(TEST_TIMESTAMP),
            sourceDao = mockTracks,
            exclusionDao = mockExclusions
        )
    }

    @AfterTest
    fun teardown() {
        appScope.cancel()
    }

    @Test
    fun `tracks - returns flow of tracks from track source`() = testScope.runTest {
        every { mockTracks.tracks } returns infiniteFlowOf(
            listOf(Local.Cartagena, Local.IsolatedSystem)
        )
        every { mockExclusions.trackExclusions } returns infiniteFlowOf(
            emptyList()
        )

        val tracks = repository.tracks.first()

        tracks.shouldContainExactly(
            Cartagena,
            IsolatedSystem,
        )
    }

    @Test
    fun `tracks - omits excluded tracks`() = testScope.runTest {
        every { mockTracks.tracks } returns infiniteFlowOf(
            listOf(Local.Cartagena, Local.IsolatedSystem)
        )
        every { mockExclusions.trackExclusions } returns infiniteFlowOf(
            listOf(TrackExclusion(trackId = Cartagena.id, excludeDate = 0L))
        )

        val tracks = repository.tracks.first()

        tracks.shouldContainExactly(IsolatedSystem)
    }

    @Test
    fun `tracks - emits whenever source changes`() = testScope.runTest {
        val tracksFlow = MutableStateFlow(
            listOf(Local.Cartagena)
        )
        every { mockTracks.tracks } returns tracksFlow
        every { mockExclusions.trackExclusions } returns infiniteFlowOf(emptyList())

        repository.tracks.drop(1).test {
            tracksFlow.value = listOf(Local.Cartagena, Local.IsolatedSystem)
            awaitItem().shouldContainExactly(Cartagena, IsolatedSystem)

            tracksFlow.value = listOf(Local.IsolatedSystem)
            awaitItem().shouldContainExactly(IsolatedSystem)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tracks - emits whenever exclusion list changes`() = testScope.runTest {
        val exclusionFlow = MutableStateFlow(
            listOf(TrackExclusion(Cartagena.id, 0L))
        )
        every { mockTracks.tracks } returns infiniteFlowOf(
            listOf(Local.Cartagena, Local.IsolatedSystem)
        )
        every { mockExclusions.trackExclusions } returns exclusionFlow

        repository.tracks.drop(1).test {
            exclusionFlow.value = emptyList()
            awaitItem().shouldContainExactly(Cartagena, IsolatedSystem)

            exclusionFlow.value = listOf(
                TrackExclusion(Cartagena.id, TEST_TIMESTAMP),
                TrackExclusion(IsolatedSystem.id, TEST_TIMESTAMP)
            )
            awaitItem().shouldBeEmpty()
        }
    }

    @Test
    fun `tracks - emits cached source on new subscriber`() = testScope.runTest {
        val tracksFlow = MutableStateFlow(listOf(Local.Cartagena))
        every { mockTracks.tracks } returns tracksFlow
        every { mockExclusions.trackExclusions } returns infiniteFlowOf(emptyList())

        // Simulates a short-lived subscriber
        repository.tracks.take(1).collect()
        // Source changes while no subscriber
        tracksFlow.value = listOf(Local.IsolatedSystem)

        // New subscriber should collect from the cache first
        repository.tracks.test {
            awaitItem().shouldContainExactly(Cartagena)
            awaitItem().shouldContainExactly(IsolatedSystem)
            expectNoEvents()
        }
    }

    @Test
    fun `excludedTracks - returns only excluded tracks`() = testScope.runTest {
        every { mockTracks.tracks } returns infiniteFlowOf(
            listOf(Local.Cartagena, Local.IsolatedSystem, Local.DirtyWater, Local.ThePretenders)
        )
        every { mockExclusions.trackExclusions } returns infiniteFlowOf(
            listOf(
                TrackExclusion(ThePretenders.id, 1652552100),
                TrackExclusion(IsolatedSystem.id, 1647869400),
                TrackExclusion(DirtyWater.id, 1636274520),
            )
        )

        val excludedTracks = repository.excludedTracks.first()

        excludedTracks.shouldContainExactly(
            IsolatedSystem.copy(exclusionTime = 1647869400),
            DirtyWater.copy(exclusionTime = 1636274520),
            ThePretenders.copy(exclusionTime = 1652552100),
        )
    }

    @Test
    fun `excludeTrack - adds a track exclusion`() = testScope.runTest {
        repository.excludeTrack(Cartagena.id)

        coVerifySequence {
            mockExclusions.exclude(
                TrackExclusion(Cartagena.id, TEST_TIMESTAMP)
            )
        }
    }

    @Test
    fun `allowTrack - removes a track exclusion`() = testScope.runTest {
        repository.allowTrack(Cartagena.id)

        coVerifySequence {
            mockExclusions.allow(Cartagena.id)
        }
    }

    @Test
    fun `deleteTracks - deletes tracks from source`() = testScope.runTest {
        coEvery { mockTracks.deleteTracks(any()) } returns DeleteTracksResult.Deleted(2)


        val result = repository.deleteTracks(
            longArrayOf(Cartagena.id, IsolatedSystem.id)
        )

        result shouldBe DeleteTracksResult.Deleted(2)
        coVerifySequence {
            mockTracks.deleteTracks(longArrayOf(Cartagena.id, IsolatedSystem.id))
        }
    }

    @Test
    fun `getAlbumTracks - returns all tracks part of an album sorted by number`() = runTest {
        every { mockTracks.tracks } returns infiniteFlowOf(
            listOf(Local.IsolatedSystem, Local.Algorithm, Local.DirtyWater, Local.ThePretenders, Local.Run)
        )
        every { mockExclusions.trackExclusions } returns infiniteFlowOf(emptyList())

        val albumTracks = repository.getAlbumTracks(ConcreteAndGold.id).first()

        albumTracks.shouldContainExactly(Run, DirtyWater)
    }

    @Test
    fun `getAlbumTracks - returns empty list for unknown album`() = runTest {
        every { mockTracks.tracks } returns infiniteFlowOf(
            listOf(Local.IsolatedSystem, Local.Algorithm, Local.DirtyWater, Local.ThePretenders, Local.Run)
        )
        every { mockExclusions.trackExclusions } returns infiniteFlowOf(emptyList())

        val tracks = repository.getAlbumTracks(SunsetOnGoldenAge.id).first()
        tracks.shouldBeEmpty()
    }

    @Test
    fun `getArtistTracks - returns all tracks produced by an artist sorted alphabetically`() =
        runTest {
            every { mockTracks.tracks } returns infiniteFlowOf(
                listOf(Local.IsolatedSystem, Local.Algorithm, Local.DirtyWater, Local.ThePretenders, Local.Run)
            )
            every { mockExclusions.trackExclusions } returns infiniteFlowOf(emptyList())

            val artistTracks = repository.getArtistTracks(FooFighters.id).first()

            artistTracks.shouldContainExactly(DirtyWater, ThePretenders, Run)
        }

    @Test
    fun `getArtistTracks - returns empty list for unknown artist`() = runTest {
        every { mockTracks.tracks } returns infiniteFlowOf(
            listOf(Local.IsolatedSystem, Local.Algorithm, Local.DirtyWater, Local.ThePretenders, Local.Run)
        )
        every { mockExclusions.trackExclusions } returns infiniteFlowOf(emptyList())

        val tracks = repository.getArtistTracks(Alestorm.id).first()
        tracks.shouldBeEmpty()
    }
}
