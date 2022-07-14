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
import fr.nihilus.music.core.database.playlists.PlaylistDao
import fr.nihilus.music.core.database.playlists.PlaylistTrack
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_PLAYLISTS
import fr.nihilus.music.core.test.coroutines.flow.infiniteFlowOf
import fr.nihilus.music.media.provider.MediaDao
import fr.nihilus.music.service.AudioTrack
import fr.nihilus.music.service.MediaCategory
import fr.nihilus.music.service.MediaContent
import fr.nihilus.music.service.browser.SAMPLE_PLAYLISTS
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
import org.junit.Before
import org.junit.runner.RunWith
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
internal class PlaylistChildrenProviderTest {
    @MockK private lateinit var mockDao: MediaDao
    @MockK private lateinit var mockPlaylists: PlaylistDao

    private lateinit var provider: ChildrenProvider

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        provider = PlaylistChildrenProvider(
            mediaDao = mockDao,
            playlistDao = mockPlaylists,
        )
    }

    @Test
    fun `Given 'playlists' type, returns a list of all playlists`() = runTest {
        every { mockPlaylists.playlists } returns infiniteFlowOf(SAMPLE_PLAYLISTS)

        val allPlaylists = provider.getChildren(MediaId(TYPE_PLAYLISTS)).first()

        allPlaylists.forAll {
            it.shouldBeTypeOf<MediaCategory>()
            it.browsable shouldBe true
            it.playable shouldBe true
        }
        extracting(allPlaylists, MediaContent::id).shouldContainExactly(
            MediaId(TYPE_PLAYLISTS, "1"),
            MediaId(TYPE_PLAYLISTS, "2"),
            MediaId(TYPE_PLAYLISTS, "3")
        )
        val zen = allPlaylists.first()
        assertSoftly(zen as MediaCategory) {
            title shouldBe "Zen"
        }
    }

    @Test
    fun `Given id of a playlist, returns list of its tracks`() = runTest {
        every { mockDao.tracks } returns infiniteFlowOf(SAMPLE_TRACKS)
        every { mockPlaylists.getPlaylistTracks(eq(2)) } returns infiniteFlowOf(
            listOf(
                PlaylistTrack(2L, 477L),
                PlaylistTrack(2L, 48L),
                PlaylistTrack(2L, 125L),
            )
        )

        val playlistTracks = provider.getChildren(MediaId(TYPE_PLAYLISTS, "2")).first()

        playlistTracks.forAll {
            it.shouldBeTypeOf<AudioTrack>()
            it.browsable shouldBe false
            it.playable shouldBe true
        }
        extracting(playlistTracks, MediaContent::id).shouldContainExactly(
            MediaId(TYPE_PLAYLISTS, "2", 477),
            MediaId(TYPE_PLAYLISTS, "2", 48),
            MediaId(TYPE_PLAYLISTS, "2", 125)
        )
        val run = playlistTracks.first()
        assertSoftly(run as AudioTrack) {
            title shouldBe "Run"
            duration shouldBe 323424L
        }
    }

    @Test
    fun `Given unsupported category, returns NoSuchElementException flow`() = runTest {
        every { mockDao.tracks } returns infiniteFlowOf(SAMPLE_TRACKS)
        every { mockPlaylists.getPlaylistTracks(1234) } returns infiniteFlowOf(emptyList())

        val children = provider.getChildren(MediaId(TYPE_PLAYLISTS, "1234"))

        shouldThrow<NoSuchElementException> {
            children.collect()
        }
    }
}
