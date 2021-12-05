/*
 * Copyright 2021 Thibault Seisel
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

package fr.nihilus.music.media.exclusion

import app.cash.turbine.test
import fr.nihilus.music.core.database.exclusion.TrackExclusion
import fr.nihilus.music.core.os.PermissionDeniedException
import fr.nihilus.music.core.test.coroutines.CoroutineTestRule
import fr.nihilus.music.core.test.coroutines.withinScope
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.flow.first
import org.junit.Rule
import kotlin.test.Test

internal class RestrictedMediaDaoTest {

    @get:Rule
    val test = CoroutineTestRule()

    @Test
    fun `It should return unmodified tracks when having no exclusions`() = test.run {
        withinScope {
            val sut = RestrictedMediaDao(this, StubMediaDao, FakeExclusionDao())
            val restrictedTracks = sut.tracks.first()
            restrictedTracks.shouldContainExactly(ALGORITHM, DIRTY_WATER, MATTER_OF_TIME, RUN)
        }
    }

    @Test
    fun `It should return unmodified albums when having no exclusions`() = test.run {
        withinScope {
            val sut = RestrictedMediaDao(this, StubMediaDao, FakeExclusionDao())
            val restrictedAlbums = sut.albums.first()
            restrictedAlbums.shouldContainExactly(CONCRETE_GOLD, SIMULATION_THEORY, WASTING_LIGHT)
        }
    }

    @Test
    fun `It should return unmodified artists when having no exclusions`() = test.run {
        withinScope {
            val sut = RestrictedMediaDao(this, StubMediaDao, FakeExclusionDao())
            val restrictedArtists = sut.artists.first()
            restrictedArtists.shouldContainExactly(FOO_FIGHTERS, MUSE)
        }
    }

    @Test
    fun `It should omit tracks that are in exclude list`() = test.run {
        withinScope {
            val sut = RestrictedMediaDao(this, StubMediaDao,
                FakeExclusionDao(ALGORITHM.id, MATTER_OF_TIME.id)
            )
            val filteredTracks = sut.tracks.first()
            filteredTracks.shouldContainExactly(DIRTY_WATER, RUN)
        }
    }

    @Test
    fun `Albums should subtract excluded tracks from track count`() = test.run {
        withinScope {
            val sut = RestrictedMediaDao(this, StubMediaDao, FakeExclusionDao(RUN.id))
            val alteredAlbums = sut.albums.first()
            alteredAlbums.shouldContainExactly(
                CONCRETE_GOLD.copy(trackCount = 1),
                SIMULATION_THEORY,
                WASTING_LIGHT
            )
        }
    }

    @Test
    fun `Albums having only excluded tracks should be omitted`() = test.run {
        withinScope {
            val sut = RestrictedMediaDao(this, StubMediaDao, FakeExclusionDao(MATTER_OF_TIME.id))
            val filteredAlbums = sut.albums.first()
            filteredAlbums.shouldContainExactly(CONCRETE_GOLD, SIMULATION_THEORY)
        }
    }

    @Test
    fun `Artists should subtract excluded tracks and albums from count`() = test.run {
        withinScope {
            val sut = RestrictedMediaDao(this, StubMediaDao, FakeExclusionDao(MATTER_OF_TIME.id))
            val alteredArtists = sut.artists.first()
            alteredArtists.shouldContainExactly(
                FOO_FIGHTERS.copy(trackCount = 2, albumCount = 1),
                MUSE
            )
        }
    }

    @Test
    fun `Artists having only excluded tracks should be omitted`() = test.run {
        withinScope {
            val sut = RestrictedMediaDao(this, StubMediaDao, FakeExclusionDao(ALGORITHM.id))
            val filteredArtists = sut.artists.first()
            filteredArtists.shouldContainExactly(FOO_FIGHTERS)
        }
    }

    @Test
    fun `It should update track list when exclude list changed`() = test.run {
        withinScope {
            val exclusions = FakeExclusionDao(MATTER_OF_TIME.id)
            val sut = RestrictedMediaDao(this, StubMediaDao, exclusions)

            sut.tracks.test {
                awaitItem()

                exclusions.exclude(TrackExclusion(ALGORITHM.id, 0))
                awaitItem().shouldContainExactly(DIRTY_WATER, RUN)

                exclusions.allow(MATTER_OF_TIME.id)
                awaitItem().shouldContainExactly(DIRTY_WATER, MATTER_OF_TIME, RUN)
            }
        }
    }

    @Test
    fun `it should preserve exceptions thrown by source dao`() = test.run {
        withinScope {
            val sut = RestrictedMediaDao(this, PermissionDeniedDao, FakeExclusionDao())
            shouldThrow<PermissionDeniedException> {
                sut.tracks.first()
            }
            shouldThrow<PermissionDeniedException> {
                sut.albums.first()
            }
            shouldThrow<PermissionDeniedException> {
                sut.artists.first()
            }
        }
    }
}