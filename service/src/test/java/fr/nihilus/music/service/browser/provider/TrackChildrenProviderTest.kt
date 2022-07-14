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

package fr.nihilus.music.service.browser.provider

import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_MOST_RATED
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_RECENTLY_ADDED
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.core.test.coroutines.flow.infiniteFlowOf
import fr.nihilus.music.media.tracks.TrackRepository
import fr.nihilus.music.media.usage.UsageManager
import fr.nihilus.music.service.AudioTrack
import fr.nihilus.music.service.MediaContent
import fr.nihilus.music.service.browser.SAMPLE_MOST_RATED_TRACKS
import fr.nihilus.music.service.browser.SAMPLE_TRACKS
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.extracting
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
internal class TrackChildrenProviderTest {

    @MockK private lateinit var mockTracks: TrackRepository
    @MockK private lateinit var mockUsage: UsageManager

    private lateinit var provider: TrackChildrenProvider

    @BeforeTest
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        provider = TrackChildrenProvider(
            trackRepository = mockTracks,
            usageManager = mockUsage
        )
    }

    @Test
    fun `Given 'all tracks' category, loads all tracks`() = runTest {
        every { mockTracks.tracks } returns infiniteFlowOf(SAMPLE_TRACKS)

        val allTracks = provider.getChildren(MediaId(TYPE_TRACKS, CATEGORY_ALL)).first()

        allTracks.shouldAllBePlayable()
        extracting(allTracks, MediaContent::id).shouldContainExactly(
            MediaId(TYPE_TRACKS, CATEGORY_ALL, 161),
            MediaId(TYPE_TRACKS, CATEGORY_ALL, 309),
            MediaId(TYPE_TRACKS, CATEGORY_ALL, 481),
            MediaId(TYPE_TRACKS, CATEGORY_ALL, 48),
            MediaId(TYPE_TRACKS, CATEGORY_ALL, 125),
            MediaId(TYPE_TRACKS, CATEGORY_ALL, 294),
            MediaId(TYPE_TRACKS, CATEGORY_ALL, 219),
            MediaId(TYPE_TRACKS, CATEGORY_ALL, 75),
            MediaId(TYPE_TRACKS, CATEGORY_ALL, 464),
            MediaId(TYPE_TRACKS, CATEGORY_ALL, 477)
        )
        assertSoftly(allTracks[4] as AudioTrack) {
            title shouldBe "Jailbreak"
            artist shouldBe "AC/DC"
            album shouldBe "Greatest Hits 30 Anniversary Edition"
            duration shouldBe 276668L
            disc shouldBe 2
            number shouldBe 14
        }
    }

    @Test
    fun `Given 'most rated' category, loads most rated tracks`() = runTest {
        every { mockUsage.getMostRatedTracks() } returns infiniteFlowOf(
            SAMPLE_MOST_RATED_TRACKS
        )

        val mostRated = provider.getChildren(MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED)).first()

        mostRated.shouldAllBePlayable()
        extracting(mostRated, MediaContent::id).shouldContainExactly(
            MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED, 75),
            MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED, 464),
            MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED, 48),
            MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED, 477),
            MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED, 294)
        )
        val nightmare = mostRated.first()
        nightmare.shouldBeTypeOf<AudioTrack>()
        assertSoftly(nightmare) {
            title shouldBe "Nightmare"
            artist shouldBe "Avenged Sevenfold"
            album shouldBe "Nightmare"
            duration shouldBe 374648L
            disc shouldBe 1
            number shouldBe 1
        }
    }

    @Test
    @Ignore("This feature is not properly specified.")
    fun `Given 'popular' category, loads tracks popular this month`() = runTest {
        TODO()
    }

    @Test
    fun `Given 'most recent' category, loads last added tracks`() = runTest {
        every { mockTracks.tracks } returns infiniteFlowOf(SAMPLE_TRACKS)

        val mostRecentTracks =
            provider.getChildren(MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED)).first()

        mostRecentTracks.shouldAllBePlayable()
        extracting(mostRecentTracks, MediaContent::id).shouldContainExactly(
            MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED, 481), // September 25th, 2019 (21:22)
            MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED, 477), // September 25th, 2019 (21:22)
            MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED, 161), // June 18th, 2016
            MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED, 125), // February 12th, 2016 (21:49)
            MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED, 48),  // February 12th, 2016 (21:48)
            MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED, 309), // August 15th, 2015 (15:49)
            MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED, 464), // August 15th, 2015 (15:49)
            MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED, 75),  // August 14th, 2015
            MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED, 294), // November 1st, 2014
            MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED, 219)  // February 12th, 2013
        )
        val dirtyWater = mostRecentTracks.first()
        dirtyWater.shouldBeTypeOf<AudioTrack>()
        assertSoftly(dirtyWater) {
            title shouldBe "Dirty Water"
            artist shouldBe "Foo Fighters"
            album shouldBe "Concrete and Gold"
            duration shouldBe 320914L
            disc shouldBe 1
            number shouldBe 6
        }
    }

    @Test
    fun `Given unsupported category, returns NoSuchElementException flow`() = runTest {
        val children = provider.getChildren(MediaId(TYPE_TRACKS, "unknown"))

        shouldThrow<NoSuchElementException> {
            children.collect()
        }
    }
}

private fun List<MediaContent>.shouldAllBePlayable() {
    forAll {
        it.shouldBeTypeOf<AudioTrack>()
        it.browsable shouldBe false
        it.playable shouldBe true
    }
}
