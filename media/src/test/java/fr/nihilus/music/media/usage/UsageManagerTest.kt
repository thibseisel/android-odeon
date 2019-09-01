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

package fr.nihilus.music.media.usage

import fr.nihilus.music.media.os.TestClock
import fr.nihilus.music.media.provider.Track
import fr.nihilus.music.media.repo.MediaRepository
import io.kotlintest.inspectors.forNone
import io.kotlintest.inspectors.forOne
import io.kotlintest.matchers.collections.containExactly
import io.kotlintest.matchers.collections.shouldBeSortedWith
import io.kotlintest.matchers.collections.shouldNotContain
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

private val CARTAGENA = Track(161, "1741 (The Battle of Cartagena)", "Alestorm", "Sunset on the Golden Age", 437603, 1, 4, "", null, 1466283480, 26, 65, 17_506_481)
private val DIRTY_WATER = Track(481, "Dirty Water", "Foo Fighters", "Concrete and Gold", 320914, 1, 6, "", null, 1506374520, 13, 102, 12_912_282)
private val GIVE_IT_UP = Track(48, "Give It Up", "AC/DC", "Greatest Hits 30 Anniversary Edition", 233592, 1, 19, "", null, 1455310080, 5, 7, 5_716_578)
private val CYDONIA = Track(294, "Knights of Cydonia", "Muse", "Black Holes and Revelations", 366946, 1, 11, "", null, 1414880700, 18, 38, 11_746_572)
private val NIGHTMARE = Track(75, "Nightmare", "Avenged Sevenfold", "Nightmare", 374648, 1, 1, "", null, 1439590380, 4, 6, 10_828_662)

class UsageManagerTest {
    private val clock = TestClock(1234567890L)

    @Test
    fun `When reporting playback completion of a track, then record a new event at current time`() = runBlockingTest {
        val usageDao = mockk<MediaUsageDao>(relaxUnitFun = true)
        val manager = UsageManager(mockk(), usageDao)

        manager.reportCompletion(42L)

        val eventSlot = slot<MediaUsageEvent>()
        coVerify(exactly = 1) { usageDao.recordEvent(capture(eventSlot)) }

        with(eventSlot.captured) {
            uid shouldBe 0L
            trackId shouldBe 42L
            eventTime shouldBe 1234567890L
        }
    }

    @Test
    fun `When loading most rated tracks, then return them ordered by descending score`() = runBlockingTest {
        val repository = givenRepositoryWithTracks(DIRTY_WATER, GIVE_IT_UP, CYDONIA, NIGHTMARE)

        val usageDao = givenDaoHavingScores(
            TrackScore(75L, 82),  // Nightmare
            TrackScore(294L, 43), // Knights of Cydonia
            TrackScore(481L, 20), // Dirty Water
            TrackScore(48L, 12)   // Give It Up
        )

        val manager = UsageManager(repository, usageDao)
        val mostRatedTracks = manager.getMostRatedTracks()

        mostRatedTracks should containExactly(NIGHTMARE, CYDONIA, DIRTY_WATER, GIVE_IT_UP)
    }

    @Test
    fun `When loading most rated tracks, then omit tracks without score`() = runBlockingTest {
        val repository = givenRepositoryWithTracks(NIGHTMARE, GIVE_IT_UP, CYDONIA, NIGHTMARE)

        val usageDao = givenDaoHavingScores(
            TrackScore(75L, 82), // Nightmare
            TrackScore(294L, 43) // Knights of Cydonia
        )

        val manager = UsageManager(repository, usageDao)
        val mostRatedTracks = manager.getMostRatedTracks()

        mostRatedTracks.shouldNotContain(DIRTY_WATER)
        mostRatedTracks.shouldNotContain(GIVE_IT_UP)
    }

    @Test
    fun `When loading most rated tracks, then ignore scores of unknown tracks`() = runBlockingTest {
        val repository = givenRepositoryWithTracks(DIRTY_WATER)
        val dao = givenDaoHavingScores(
            TrackScore(42L, 120), // Unknown
            TrackScore(481L, 20), // Dirty Water
            TrackScore(100L, 56)  // Unknown
        )

        val manager = UsageManager(repository, dao)
        val mostRatedTracks = manager.getMostRatedTracks()

        mostRatedTracks.forNone { it.id shouldBe 42L }
        mostRatedTracks.forNone { it.id shouldBe 100L }
    }

    @Test
    fun `When loading disposable tracks, then list tracks with no events`() = runBlockingTest {
        val repository = mockk<MediaRepository> {
            coEvery { getAllTracks() } returns listOf(DIRTY_WATER, GIVE_IT_UP, CYDONIA, NIGHTMARE)
        }

        val dao = mockk<MediaUsageDao> {
            coEvery { getTracksUsage() } returns listOf(
                TrackUsage(75L, 82, 0),
                TrackUsage(294L, 43, 0)
            )
        }

        val manager = UsageManager(repository, dao)
        val disposableTracks = manager.getDisposableTracks()

        disposableTracks.forOne { it.trackId shouldBe 48L }
        disposableTracks.forOne { it.trackId shouldBe 481L }
    }

    @Test
    fun `When loading disposable tracks, then list larger files first`() = runBlockingTest {
        val repository = givenRepositoryWithTracks(CARTAGENA, DIRTY_WATER, CYDONIA, GIVE_IT_UP, NIGHTMARE)
        val dao = mockk<MediaUsageDao> {
            coEvery { getTracksUsage() } returns emptyList()
        }

        val manager = UsageManager(repository, dao)
        val disposableTracks = manager.getDisposableTracks()

        disposableTracks.shouldBeSortedWith(compareByDescending { it.fileSizeBytes })
    }

    private fun CoroutineScope.UsageManager(repository: MediaRepository, usageDao: MediaUsageDao): MediaUsageManager =
        UsageManagerImpl(this, repository, usageDao, clock)

    private fun givenRepositoryWithTracks(vararg tracks: Track): MediaRepository = mockk("MediaRepository") {
        coEvery { getAllTracks() } returns tracks.toList()
    }

    private fun givenDaoHavingScores(vararg scores: TrackScore): MediaUsageDao = mockk("UsageDao") {
        val slot = slot<Int>()
        coEvery {
            getMostRatedTracks(capture(slot))
        } answers {
            val limit = slot.captured
            scores.take(limit)
        }
    }
}