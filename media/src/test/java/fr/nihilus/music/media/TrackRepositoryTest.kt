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

package fr.nihilus.music.media

import app.cash.turbine.test
import fr.nihilus.music.core.database.exclusion.TrackExclusion
import fr.nihilus.music.core.database.exclusion.TrackExclusionDao
import fr.nihilus.music.core.test.coroutines.flow.infiniteFlowOf
import fr.nihilus.music.core.test.os.TestClock
import fr.nihilus.music.media.provider.*
import fr.nihilus.music.media.provider.ALGORITHM
import fr.nihilus.music.media.provider.CARTAGENA
import fr.nihilus.music.media.provider.ISOLATED_SYSTEM
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
        val allTracks = listOf(CARTAGENA, ISOLATED_SYSTEM)
        every { mockTracks.tracks } returns infiniteFlowOf(allTracks)
        every { mockExclusions.trackExclusions } returns infiniteFlowOf(emptyList())

        val tracks = repository.tracks.first()

        tracks.shouldContainExactly(allTracks)
    }

    @Test
    fun `tracks - omits excluded tracks`() = testScope.runTest {
        every { mockTracks.tracks } returns infiniteFlowOf(listOf(CARTAGENA, ISOLATED_SYSTEM))
        every { mockExclusions.trackExclusions } returns infiniteFlowOf(
            listOf(TrackExclusion(trackId = CARTAGENA.id, excludeDate = 0L))
        )

        val tracks = repository.tracks.first()

        tracks.shouldContainExactly(ISOLATED_SYSTEM)
    }

    @Test
    fun `tracks - emits whenever source changes`() = testScope.runTest {
        val tracksFlow = MutableStateFlow(
            listOf(CARTAGENA)
        )
        every { mockTracks.tracks } returns tracksFlow
        every { mockExclusions.trackExclusions } returns infiniteFlowOf(emptyList())

        repository.tracks.drop(1).test {
            tracksFlow.value = listOf(CARTAGENA, ISOLATED_SYSTEM)
            awaitItem().shouldContainExactly(CARTAGENA, ISOLATED_SYSTEM)

            tracksFlow.value = listOf(ISOLATED_SYSTEM)
            awaitItem().shouldContainExactly(ISOLATED_SYSTEM)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tracks - emits whenever exclusion list changes`() = testScope.runTest {
        val exclusionFlow = MutableStateFlow(
            listOf(TrackExclusion(CARTAGENA.id, 0L))
        )
        every { mockTracks.tracks } returns infiniteFlowOf(listOf(CARTAGENA, ISOLATED_SYSTEM))
        every { mockExclusions.trackExclusions } returns exclusionFlow

        repository.tracks.drop(1).test {
            exclusionFlow.value = emptyList()
            awaitItem().shouldContainExactly(CARTAGENA, ISOLATED_SYSTEM)

            exclusionFlow.value = listOf(
                TrackExclusion(CARTAGENA.id, TEST_TIMESTAMP),
                TrackExclusion(ISOLATED_SYSTEM.id, TEST_TIMESTAMP)
            )
            awaitItem().shouldBeEmpty()
        }
    }

    @Test
    fun `tracks - emits cached source on new subscriber`() = testScope.runTest {
        val tracksFlow = MutableStateFlow(listOf(CARTAGENA))
        every { mockTracks.tracks } returns tracksFlow
        every { mockExclusions.trackExclusions } returns infiniteFlowOf(emptyList())

        // Simulates a short-lived subscriber
        repository.tracks.take(1).collect()
        // Source changes while no subscriber
        tracksFlow.value = listOf(ISOLATED_SYSTEM)

        // New subscriber should collect from the cache first
        repository.tracks.test {
            awaitItem().shouldContainExactly(CARTAGENA)
            awaitItem().shouldContainExactly(ISOLATED_SYSTEM)
            expectNoEvents()
        }
    }

    @Test
    fun `excludeTrack - adds a track exclusion`() = testScope.runTest {
        repository.excludeTrack(CARTAGENA.id)

        coVerifySequence {
            mockExclusions.exclude(
                TrackExclusion(CARTAGENA.id, TEST_TIMESTAMP)
            )
        }
    }

    @Test
    fun `allowTrack - removes a track exclusion`() = testScope.runTest {
        repository.allowTrack(CARTAGENA.id)

        coVerifySequence {
            mockExclusions.allow(CARTAGENA.id)
        }
    }

    @Test
    fun `deleteTracks - deletes tracks from source`() = testScope.runTest {
        coEvery { mockTracks.deleteTracks(any()) } returns DeleteTracksResult.Deleted(2)


        val result = repository.deleteTracks(
            longArrayOf(CARTAGENA.id, ISOLATED_SYSTEM.id)
        )

        result shouldBe DeleteTracksResult.Deleted(2)
        coVerifySequence {
            mockTracks.deleteTracks(longArrayOf(CARTAGENA.id, ISOLATED_SYSTEM.id))
        }
    }

    @Test
    fun `getAlbumTracks - returns all tracks part of an album sorted by number`() = runTest {
        every { mockTracks.tracks } returns infiniteFlowOf(
            listOf(ISOLATED_SYSTEM, ALGORITHM, DIRTY_WATER, THE_PRETENDERS, RUN)
        )
        every { mockExclusions.trackExclusions } returns infiniteFlowOf(emptyList())

        val albumTracks = repository.getAlbumTracks(CONCRETE_AND_GOLD.id).first()

        albumTracks.shouldContainExactly(RUN, DIRTY_WATER)
    }

    @Test
    fun `getAlbumTracks - returns empty list for unknown album`() = runTest {
        every { mockTracks.tracks } returns infiniteFlowOf(
            listOf(ISOLATED_SYSTEM, ALGORITHM, DIRTY_WATER, THE_PRETENDERS, RUN)
        )
        every { mockExclusions.trackExclusions } returns infiniteFlowOf(emptyList())

        val tracks = repository.getAlbumTracks(SUNSET_ON_GOLDEN_AGE.id).first()
        tracks.shouldBeEmpty()
    }

    @Test
    fun `getArtistTracks - returns all tracks produced by an artist sorted alphabetically`() = runTest {
        every { mockTracks.tracks } returns infiniteFlowOf(
            listOf(ISOLATED_SYSTEM, ALGORITHM, DIRTY_WATER, THE_PRETENDERS, RUN)
        )
        every { mockExclusions.trackExclusions } returns infiniteFlowOf(emptyList())

        val artistTracks = repository.getArtistTracks(FOO_FIGHTERS.id).first()

        artistTracks.shouldContainExactly(DIRTY_WATER, THE_PRETENDERS, RUN)
    }

    @Test
    fun `getArtistTracks - returns empty list for unknown artist`() = runTest {
        every { mockTracks.tracks } returns infiniteFlowOf(
            listOf(ISOLATED_SYSTEM, ALGORITHM, DIRTY_WATER, THE_PRETENDERS, RUN)
        )
        every { mockExclusions.trackExclusions } returns infiniteFlowOf(emptyList())

        val tracks = repository.getArtistTracks(ALESTORM.id).first()
        tracks.shouldBeEmpty()
    }
}

