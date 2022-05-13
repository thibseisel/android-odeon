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

package fr.nihilus.music.core.ui.actions

import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.nihilus.music.core.database.playlists.Playlist
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ALBUMS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ARTISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_PLAYLISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.media.playlist.PlaylistRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.file.shouldNotExist
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test

private const val NEW_PLAYLIST_NAME = "My favorites"
private val SAMPLE_PLAYLIST = Playlist(
    id = 1L,
    title = "Zen",
    created = 0L,
    iconUri = "content://fr.nihilus.music.test.provider/icons/zen.png".toUri()
)

@RunWith(AndroidJUnit4::class)
internal class ManagePlaylistActionTest {

    @MockK private lateinit var mockRepository: PlaylistRepository
    private lateinit var action: ManagePlaylistAction

    @BeforeTest
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        action = ManagePlaylistAction(mockRepository)
    }

    @Test
    fun `createPlaylist - creates a playlist without tracks in repository`() = runTest {
        coEvery { mockRepository.createUserPlaylist(any()) } answers {
            Playlist(
                id = 42,
                title = firstArg(),
                created = 0,
                iconUri = "content://fr.nihilus.music.test.provider/icons/My_favorites.png".toUri()
            )
        }

        action.createPlaylist(NEW_PLAYLIST_NAME, emptyList())

        coVerifySequence {
            mockRepository.createUserPlaylist(NEW_PLAYLIST_NAME)
        }
    }

    @Test
    fun `createPlaylist - creates a playlist with tracks in repository`() = runTest {
        coEvery { mockRepository.createUserPlaylist(any()) } answers {
            Playlist(
                id = 42,
                title = firstArg(),
                created = 0,
                iconUri = "content://fr.nihilus.music.test.provider/icons/My_favorites.png".toUri()
            )
        }

        action.createPlaylist(
            name = NEW_PLAYLIST_NAME,
            members = listOf(
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 16L),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 42L)
            )
        )

        coVerifySequence {
            mockRepository.createUserPlaylist(NEW_PLAYLIST_NAME)
            mockRepository.addTracksToPlaylist(42, longArrayOf(16, 42))
        }
    }

    @Test
    fun `createPlaylist - throws IAE when creating an playlist with non-track media ids`() =
        runTest {
            for (mediaId in invalidTrackIds()) {
                shouldThrow<IllegalArgumentException> {
                    action.createPlaylist(
                        name = NEW_PLAYLIST_NAME,
                        members = listOf(mediaId)
                    )
                }
            }

            confirmVerified(mockRepository)
        }

    @Test
    fun `appendMembers - add tracks to an existing playlist in repository`() = runTest {
        action.appendMembers(
            targetPlaylist = MediaId(TYPE_PLAYLISTS, SAMPLE_PLAYLIST.id.toString()),
            members = listOf(
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 16L),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 42L)
            )
        )

        coVerifySequence {
            mockRepository.addTracksToPlaylist(SAMPLE_PLAYLIST.id, longArrayOf(16, 42))
        }
    }

    @Test
    fun `appendMembers - throws IAE given an invalid playlist media id`() = runTest {
        val newMemberIds = listOf(
            MediaId(TYPE_TRACKS, CATEGORY_ALL, 16L),
            MediaId(TYPE_TRACKS, CATEGORY_ALL, 42L)
        )

        for (mediaId in invalidPlaylistIds()) {
            shouldThrow<IllegalArgumentException> {
                action.appendMembers(
                    targetPlaylist = mediaId,
                    members = newMemberIds
                )
            }
        }

        confirmVerified(mockRepository)
    }

    @Test
    fun `deletePlaylist - deletes playlist from repository`() = runTest {
        action.deletePlaylist(MediaId(TYPE_PLAYLISTS, SAMPLE_PLAYLIST.id.toString()))

        coVerifySequence {
            mockRepository.deletePlaylist(SAMPLE_PLAYLIST.id)
        }
    }

    @Test
    fun `deletePlaylist - throws IAE given an invalid playlist id`() = runTest {
        for (mediaId in invalidPlaylistIds()) {
            shouldThrow<IllegalArgumentException> {
                action.deletePlaylist(mediaId)
            }
        }

        confirmVerified(mockRepository)
    }

    private fun invalidTrackIds() = listOf(
        MediaId(TYPE_TRACKS, CATEGORY_ALL),
        MediaId(TYPE_ALBUMS, "16"),
        MediaId(TYPE_ARTISTS, "42"),
        MediaId(TYPE_PLAYLISTS, "77")
    )

    private fun invalidPlaylistIds() = listOf(
        MediaId(TYPE_TRACKS, CATEGORY_ALL),
        MediaId(TYPE_ALBUMS, "43"),
        MediaId(TYPE_ARTISTS, "89"),
        MediaId(TYPE_PLAYLISTS, "1", 16L)
    )
}
