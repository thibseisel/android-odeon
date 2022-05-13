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

package fr.nihilus.music.media.usage

import fr.nihilus.music.core.database.usage.MediaUsageEvent
import fr.nihilus.music.core.database.usage.TrackUsage
import fr.nihilus.music.core.database.usage.UsageDao
import fr.nihilus.music.core.test.coroutines.flow.infiniteFlowOf
import fr.nihilus.music.core.test.os.TestClock
import fr.nihilus.music.media.tracks.Track
import fr.nihilus.music.media.tracks.TrackRepository
import io.kotest.inspectors.forNone
import io.kotest.matchers.collections.containExactly
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.collections.shouldStartWith
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

private val DIRTY_WATER = Track(
    481,
    "Dirty Water",
    "Foo Fighters",
    "Concrete and Gold",
    320914,
    1,
    6,
    "",
    null,
    1506374520,
    13,
    102,
    12_912_282
)
private val GIVE_IT_UP = Track(
    48,
    "Give It Up",
    "AC/DC",
    "Greatest Hits 30 Anniversary Edition",
    233592,
    1,
    19,
    "",
    null,
    1455310080,
    5,
    7,
    5_716_578
)
private val CYDONIA = Track(
    294,
    "Knights of Cydonia",
    "Muse",
    "Black Holes and Revelations",
    366946,
    1,
    11,
    "",
    null,
    1414880700,
    18,
    38,
    11_746_572
)
private val NIGHTMARE = Track(
    75,
    "Nightmare",
    "Avenged Sevenfold",
    "Nightmare",
    374648,
    1,
    1,
    "",
    null,
    1439590380,
    4,
    6,
    10_828_662
)

private const val TIME_NOW = 1234567890L
private const val ONE_HOUR = 3600L
private const val ONE_DAY = ONE_HOUR * 24
private const val THREE_DAYS = ONE_DAY * 3

class UsageManagerTest {
    private val clock = TestClock(TIME_NOW)

    @MockK private lateinit var mockRepository: TrackRepository
    @MockK private lateinit var mockUsage: UsageDao

    private lateinit var manager: UsageManager

    @BeforeTest
    fun setupMocks() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        manager = UsageManagerImpl(mockRepository, mockUsage, clock)
    }

    @Test
    fun `When reporting playback completion of a track, then record a new event at current time`() =
        runTest {
            manager.reportCompletion(42L)

            coVerify(exactly = 1) {
                mockUsage.recordEvent(
                    MediaUsageEvent(
                        uid = 0L,
                        trackId = 42L,
                        eventTime = TIME_NOW
                    )
                )
            }
        }

    @Test
    fun `When loading most rated tracks, then return them ordered by descending score`() = runTest {
        every { mockRepository.tracks } returns infiniteFlowOf(
            listOf(DIRTY_WATER, GIVE_IT_UP, CYDONIA, NIGHTMARE)
        )
        coEvery { mockUsage.getTracksUsage(0L) } returns listOf(
            TrackUsage(NIGHTMARE.id, 82, TIME_NOW),
            TrackUsage(CYDONIA.id, 43, TIME_NOW),
            TrackUsage(DIRTY_WATER.id, 20, TIME_NOW),
            TrackUsage(GIVE_IT_UP.id, 12, TIME_NOW)
        )

        val mostRatedTracks = manager.getMostRatedTracks().first()

        mostRatedTracks should containExactly(NIGHTMARE, CYDONIA, DIRTY_WATER, GIVE_IT_UP)
    }

    @Test
    fun `When loading most rated tracks, then omit tracks without usage`() = runTest {
        every { mockRepository.tracks } returns infiniteFlowOf(
            listOf(NIGHTMARE, GIVE_IT_UP, CYDONIA, NIGHTMARE)
        )
        coEvery { mockUsage.getTracksUsage(0L) } returns listOf(
            TrackUsage(NIGHTMARE.id, 82, TIME_NOW),
            TrackUsage(CYDONIA.id, 43, TIME_NOW)
        )

        val mostRatedTracks = manager.getMostRatedTracks().first()

        mostRatedTracks.shouldNotContain(DIRTY_WATER)
        mostRatedTracks.shouldNotContain(GIVE_IT_UP)
    }

    @Test
    fun `When loading most rated tracks, then ignore scores of unknown tracks`() = runTest {
        every { mockRepository.tracks } returns infiniteFlowOf(
            listOf(DIRTY_WATER)
        )
        coEvery { mockUsage.getTracksUsage(0L) } returns listOf(
            TrackUsage(42L, 120, TIME_NOW), // Unknown
            TrackUsage(DIRTY_WATER.id, 20, TIME_NOW), // Dirty Water
            TrackUsage(100L, 56, TIME_NOW)  // Unknown
        )

        val mostRatedTracks = manager.getMostRatedTracks().first()

        mostRatedTracks.forNone { it.id shouldBe 42L }
        mostRatedTracks.forNone { it.id shouldBe 100L }
    }

    @Test
    fun `When loading disposable, then list tracks that have never been played first`() = runTest {
        every { mockRepository.tracks } returns infiniteFlowOf(
            listOf(
                sampleTrack(1, "Highway To Hell"),
                sampleTrack(2, "Nothing Else Matters"),
                sampleTrack(3, "Time"),
                sampleTrack(4, "The Stage")
            )
        )

        coEvery { mockUsage.getTracksUsage(0L) } returns listOf(
            TrackUsage(2, 10, TIME_NOW),
            TrackUsage(3, 7, TIME_NOW)
        )

        val tracks = manager.getDisposableTracks().first()

        tracks.map { it.title }.shouldStartWith(
            listOf(
                "Highway To Hell",
                "The Stage"
            )
        )
    }

    @Test
    fun `When loading disposable, then list tracks that have not been played for the longest time first`() =
        runTest {
            every { mockRepository.tracks } returns infiniteFlowOf(
                listOf(
                    sampleTrack(1, "Another One Bites the Dust"),
                    sampleTrack(2, "Ready To Rock"),
                    sampleTrack(3, "Wish You Were Here")
                )
            )
            coEvery { mockUsage.getTracksUsage(0L) } returns listOf(
                TrackUsage(1, 1, TIME_NOW - ONE_DAY),
                TrackUsage(2, 1, TIME_NOW - THREE_DAYS),
                TrackUsage(3, 1, TIME_NOW - ONE_HOUR)
            )


            val tracks = manager.getDisposableTracks().first()
            tracks.map { it.title }.shouldContainExactly(
                "Ready To Rock",
                "Another One Bites the Dust",
                "Wish You Were Here"
            )
        }

    @Test
    fun `Given tracks that have never been played, when loading disposable then list them in descending file size`() =
        runTest {
            every { mockRepository.tracks } returns infiniteFlowOf(
                listOf(
                    sampleTrack(1, "Knights Of Cydonia", fileSize = 11_746_572),
                    sampleTrack(2, "Murderer", fileSize = 12_211_377),
                    sampleTrack(3, "Nothing Else Matters", fileSize = 15_629_765),
                    sampleTrack(4, "The Stage", fileSize = 20_655_114),
                    sampleTrack(5, "Torn Apart", fileSize = 15_654_501)
                )
            )
            coEvery { mockUsage.getTracksUsage(0L) } returns emptyList()

            val tracks = manager.getDisposableTracks().first()
            tracks.map { it.title }.shouldContainExactly(
                "The Stage",
                "Torn Apart",
                "Nothing Else Matters",
                "Murderer",
                "Knights Of Cydonia"
            )
        }

    @Test
    fun `When loading disposable, then list tracks that have been played the least first`() =
        runTest {
            every { mockRepository.tracks } returns infiniteFlowOf(
                listOf(
                    sampleTrack(1, "Antisocial"),
                    sampleTrack(2, "Come As You Are"),
                    sampleTrack(3, "Hysteria"),
                    sampleTrack(4, "Under The Bridge")
                )
            )
            coEvery { mockUsage.getTracksUsage(0L) } returns listOf(
                TrackUsage(2, 45, TIME_NOW),
                TrackUsage(1, 178, TIME_NOW),
                TrackUsage(4, 8, TIME_NOW),
                TrackUsage(3, 32, TIME_NOW)
            )

            val tracks = manager.getDisposableTracks().first()

            tracks.map { it.title }.shouldContainExactly(
                "Under The Bridge",
                "Hysteria",
                "Come As You Are",
                "Antisocial"
            )
        }

    @Test
    fun `When loading disposable, then list largest files first`() = runTest {
        every { mockRepository.tracks } returns infiniteFlowOf(
            listOf(
                sampleTrack(1, "American Idiot", fileSize = 5_643_502),
                sampleTrack(2, "Crazy Train", fileSize = 6_549_949),
                sampleTrack(3, "Master of Puppets", fileSize = 20_999_159),
                sampleTrack(4, "Welcome to the Jungle", fileSize = 4_420_827)
            )
        )
        coEvery { mockUsage.getTracksUsage(0L) } returns emptyList()

        val tracks = manager.getDisposableTracks().first()

        tracks.map { it.title }.shouldContainExactly(
            "Master of Puppets",
            "Crazy Train",
            "American Idiot",
            "Welcome to the Jungle"
        )
    }

    /**
     * Convenience function for defining a track with only parameters
     * that are relevant for selecting disposable tracks.
     *
     * Defining multiple tracks this way allows to check ordering of tracks that are very similar
     * so that comparisons could be based on a limited set of criteria.
     *
     * @param id The unique identifier of the track.
     * @param title An optional title for the sample, could be used for comparison.
     * @param availabilityDate The epoch time at which the track has been added.
     * @param fileSize The size of the file associated with the track in bytes.
     */
    private fun sampleTrack(
        id: Long,
        title: String = "",
        availabilityDate: Long = TIME_NOW,
        fileSize: Long = 0L
    ) = Track(id, title, "", "", 0, 0, 0, "", null, availabilityDate, 0, 0, fileSize)
}
