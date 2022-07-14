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
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ALBUMS
import fr.nihilus.music.core.test.coroutines.flow.infiniteFlowOf
import fr.nihilus.music.media.albums.AlbumRepository
import fr.nihilus.music.media.tracks.TrackRepository
import fr.nihilus.music.service.AudioTrack
import fr.nihilus.music.service.MediaCategory
import fr.nihilus.music.service.MediaContent
import fr.nihilus.music.service.browser.SAMPLE_ALBUMS
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
internal class AlbumChildrenProviderTest {

    @MockK private lateinit var mockAlbums: AlbumRepository
    @MockK private lateinit var mockTracks: TrackRepository
    private lateinit var provider: ChildrenProvider

    @BeforeTest
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        provider = AlbumChildrenProvider(
            albumRepository = mockAlbums,
            trackRepository = mockTracks,
        )
    }

    @Test
    fun `Given 'albums' root, returns list of all albums`() = runTest {
        every { mockAlbums.albums } returns infiniteFlowOf(SAMPLE_ALBUMS)

        val albums = provider.getChildren(MediaId(TYPE_ALBUMS)).first()

        albums.forAll {
            it.shouldBeTypeOf<MediaCategory>()
            it.browsable shouldBe true
        }
        extracting(albums, MediaContent::id).shouldContainExactly(
            MediaId(TYPE_ALBUMS, "40"),
            MediaId(TYPE_ALBUMS, "38"),
            MediaId(TYPE_ALBUMS, "102"),
            MediaId(TYPE_ALBUMS, "95"),
            MediaId(TYPE_ALBUMS, "7"),
            MediaId(TYPE_ALBUMS, "6"),
            MediaId(TYPE_ALBUMS, "65"),
            MediaId(TYPE_ALBUMS, "26")
        )
        val the2ndLaw = albums.first()
        the2ndLaw.shouldBeTypeOf<MediaCategory>()
        assertSoftly(the2ndLaw) {
            title shouldBe "The 2nd Law"
            subtitle shouldBe "Muse"
            count shouldBe 1
        }
    }

    @Test
    fun `Given id of an album, returns a list of its tracks`() = runTest {
        every { mockTracks.tracks } returns infiniteFlowOf(SAMPLE_TRACKS)

        val albumTracks = provider.getChildren(MediaId(TYPE_ALBUMS, "102")).first()

        albumTracks.forAll {
            it.shouldBeTypeOf<AudioTrack>()
            it.browsable shouldBe false
            it.playable shouldBe true
        }
        extracting(albumTracks, MediaContent::id).shouldContainExactly(
            MediaId(TYPE_ALBUMS, "102", 477),
            MediaId(TYPE_ALBUMS, "102", 481)
        )
    }

    @Test
    fun `Given unsupported category, returns NoSuchElementException flow`() = runTest {
        every { mockTracks.tracks } returns infiniteFlowOf(SAMPLE_TRACKS)

        val children = provider.getChildren(MediaId(TYPE_ALBUMS, "1234"))

        shouldThrow<NoSuchElementException> {
            children.collect()
        }
    }
}
