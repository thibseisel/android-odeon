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

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ARTISTS
import fr.nihilus.music.core.test.coroutines.flow.infiniteFlowOf
import fr.nihilus.music.media.provider.MediaDao
import fr.nihilus.music.service.AudioTrack
import fr.nihilus.music.service.MediaCategory
import fr.nihilus.music.service.MediaContent
import fr.nihilus.music.service.browser.SAMPLE_ALBUMS
import fr.nihilus.music.service.browser.SAMPLE_ARTISTS
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
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
internal class ArtistChildrenProviderTest {

    @MockK private lateinit var mockDao: MediaDao
    private lateinit var provider: ArtistChildrenProvider

    @BeforeTest
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        provider = ArtistChildrenProvider(
            context = ApplicationProvider.getApplicationContext(),
            mediaDao = mockDao
        )
    }

    @Test
    fun `Given 'artists' type, returns list of all artists`() = runTest {
        every { mockDao.artists } returns infiniteFlowOf(SAMPLE_ARTISTS)

        val allArtists = provider.getChildren(MediaId(TYPE_ARTISTS)).first()

        allArtists.forAll {
            it.shouldBeTypeOf<MediaCategory>()
            it.browsable shouldBe true
            it.playable shouldBe false
        }
        extracting(allArtists, MediaContent::id).shouldContainExactly(
            MediaId(TYPE_ARTISTS, "5"),
            MediaId(TYPE_ARTISTS, "26"),
            MediaId(TYPE_ARTISTS, "4"),
            MediaId(TYPE_ARTISTS, "13"),
            MediaId(TYPE_ARTISTS, "18")
        )
        val acdc = allArtists.first()
        acdc.shouldBeTypeOf<MediaCategory>()
        assertSoftly(acdc) {
            title shouldBe "AC/DC"
            // TODO Use a plural string resource instead.
            subtitle shouldBe "1 albums, 2 tracks"
            count shouldBe 2
        }
    }

    @Test
    fun `Given id of an artist, returns list of its albums followed by its tracks`() = runTest {
        every { mockDao.albums } returns infiniteFlowOf(SAMPLE_ALBUMS)
        every { mockDao.tracks } returns infiniteFlowOf(SAMPLE_TRACKS)

        val children = provider.getChildren(MediaId(TYPE_ARTISTS, "13")).first()

        extracting(children, MediaContent::id).shouldContainExactly(
            MediaId(MediaId.TYPE_ALBUMS, "102"),
            MediaId(MediaId.TYPE_ALBUMS, "26"),
            MediaId(MediaId.TYPE_ALBUMS, "95"),
            MediaId(TYPE_ARTISTS, "13", 481),
            MediaId(TYPE_ARTISTS, "13", 219),
            MediaId(TYPE_ARTISTS, "13", 464),
            MediaId(TYPE_ARTISTS, "13", 477),
        )
        val concreteAndGold = children.first { it is MediaCategory } as MediaCategory
        assertSoftly(concreteAndGold) {
            title shouldBe "Concrete and Gold"
            count shouldBe 2
        }
        val dirtyWater = children.first { it is AudioTrack } as AudioTrack
        assertSoftly(dirtyWater) {
            title shouldBe "Dirty Water"
            duration shouldBe 320914
        }
    }

    @Test
    fun `Given unsupported category, returns NoSuchElementException flow`() = runTest {
        every { mockDao.albums } returns infiniteFlowOf(SAMPLE_ALBUMS)
        every { mockDao.tracks } returns infiniteFlowOf(SAMPLE_TRACKS)

        val children = provider.getChildren(MediaId(TYPE_ARTISTS, "1234"))

        shouldThrow<NoSuchElementException> {
            children.collect()
        }
    }
}
