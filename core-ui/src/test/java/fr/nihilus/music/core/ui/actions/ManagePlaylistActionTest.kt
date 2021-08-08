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
import fr.nihilus.music.core.context.AppDispatchers
import fr.nihilus.music.core.database.playlists.Playlist
import fr.nihilus.music.core.database.playlists.PlaylistDao
import fr.nihilus.music.core.database.playlists.PlaylistTrack
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ALBUMS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ARTISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_PLAYLISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.core.test.coroutines.CoroutineTestRule
import fr.nihilus.music.core.test.os.TestClock
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.extracting
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forAll
import io.kotest.inspectors.forNone
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.file.shouldContainFile
import io.kotest.matchers.file.shouldNotBeEmpty
import io.kotest.matchers.file.shouldNotExist
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.Test

private const val TEST_TIME = 1585662510L
private const val NEW_PLAYLIST_NAME = "My favorites"
private const val BASE_ICON_URI = "content://fr.nihilus.music.test.provider/icons"

private val SAMPLE_PLAYLIST = Playlist(
    id = 1L,
    title = "Zen",
    created = 0L,
    iconUri = "content://fr.nihilus.music.test.provider/icons/zen.png".toUri()
)

@RunWith(AndroidJUnit4::class)
internal class ManagePlaylistActionTest {

    @get:Rule
    val test = CoroutineTestRule()

    @get:Rule
    val iconDir = TemporaryFolder()

    private val clock = TestClock(TEST_TIME)
    private val dispatchers = AppDispatchers(test.dispatcher)

    @Test
    fun `When creating a playlist without tracks then record it to PlaylistDao`() = test.run {
        val dao = InMemoryPlaylistDao()
        val action = ManagePlaylistAction(dao)

        action.createPlaylist(NEW_PLAYLIST_NAME, emptyList())

        val playlists = dao.savedPlaylists
        playlists shouldHaveSize 1
        assertSoftly(playlists[0]) {
            title shouldBe NEW_PLAYLIST_NAME
            created shouldBe TEST_TIME
            iconUri shouldBe "content://fr.nihilus.music.test.provider/icons/My_favorites.png".toUri()
        }

        dao.savedTracks.shouldBeEmpty()
    }

    @Test
    fun `When creating a playlist then generate and save its icon`() = test.run {
        val action = ManagePlaylistAction(InMemoryPlaylistDao())

        action.createPlaylist(NEW_PLAYLIST_NAME, emptyList())

        iconDir.root shouldContainFile "My_favorites.png"
        val iconFile = File(iconDir.root, "My_favorites.png")
        iconFile.shouldBeAFile()
        iconFile.shouldNotBeEmpty()
    }

    @Test
    fun `Given blank name, when creating a playlist then fail with IAE`() = test.run {
        val action = ManagePlaylistAction(InMemoryPlaylistDao())

        shouldThrow<IllegalArgumentException> {
            action.createPlaylist("", emptyList())
        }

        shouldThrow<IllegalArgumentException> {
            action.createPlaylist("  \t\n\r", emptyList())
        }
    }

    @Test
    fun `When creating a playlist with tracks then record them to PlaylistDao`() = test.run {
        val dao = InMemoryPlaylistDao()
        val action = ManagePlaylistAction(dao)

        action.createPlaylist(
            name = NEW_PLAYLIST_NAME,
            members = listOf(
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 16L),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 42L)
            )
        )

        val playlists = dao.savedPlaylists
        playlists shouldHaveSize 1
        val newPlaylist = playlists[0]
        newPlaylist.title shouldBe NEW_PLAYLIST_NAME
        newPlaylist.created shouldBe TEST_TIME

        val tracks = dao.savedTracks
        tracks shouldHaveSize 2
        tracks.forAll { it.playlistId shouldBe newPlaylist.id }
        extracting(tracks) { trackId }.shouldContainExactlyInAnyOrder(16L, 42L)
    }

    @Test
    fun `When creating a playlist with non-track members them fail with IAE`() = test.run {
        val action = ManagePlaylistAction(InMemoryPlaylistDao())

        for (mediaId in invalidTrackIds()) {
            shouldThrow<IllegalArgumentException> {
                action.createPlaylist(
                    name = NEW_PLAYLIST_NAME,
                    members = listOf(mediaId)
                )
            }
        }
    }

    @Test
    fun `When appending members then add tracks to that playlist`() = test.run {
        val dao = InMemoryPlaylistDao(initialPlaylists = listOf(SAMPLE_PLAYLIST))
        val action = ManagePlaylistAction(dao)

        action.appendMembers(
            targetPlaylist = MediaId(TYPE_PLAYLISTS, SAMPLE_PLAYLIST.id.toString()),
            members = listOf(
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 16L),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 42L)
            )
        )

        val tracks = dao.savedTracks
        tracks shouldHaveSize 2
        tracks.forAll { it.playlistId shouldBe SAMPLE_PLAYLIST.id }
        extracting(tracks) { trackId }.shouldContainExactlyInAnyOrder(16L, 42L)
    }

    @Test
    fun `Given invalid target media id, when appending members then fail with IAE`() = test.run {
        val dao = InMemoryPlaylistDao(initialPlaylists = listOf(SAMPLE_PLAYLIST))
        val action = ManagePlaylistAction(dao)

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
    }

    @Test
    fun `When deleting a playlist then remove corresponding record from PlaylistDao`() = test.run {
        val dao = InMemoryPlaylistDao(
            initialPlaylists = listOf(SAMPLE_PLAYLIST),
            initialMembers = listOf(PlaylistTrack(SAMPLE_PLAYLIST.id, 16L))
        )
        val action = ManagePlaylistAction(dao)

        action.deletePlaylist(MediaId(TYPE_PLAYLISTS, SAMPLE_PLAYLIST.id.toString()))

        dao.savedPlaylists shouldNotContain SAMPLE_PLAYLIST
        dao.savedTracks.forNone { it.playlistId shouldBe SAMPLE_PLAYLIST.id }
    }

    @Test
    fun `Given invalid playlist id, when deleting a playlist then fail with IAE`() = test.run {
        val dao = InMemoryPlaylistDao(initialPlaylists = listOf(SAMPLE_PLAYLIST))
        val action = ManagePlaylistAction(dao)

        for (mediaId in invalidPlaylistIds()) {
            shouldThrow<IllegalArgumentException> {
                action.deletePlaylist(mediaId)
            }
        }
    }

    @Test
    fun `When deleting a playlist then delete its associated icon`() = test.run {
        val dao = InMemoryPlaylistDao(initialPlaylists = listOf(SAMPLE_PLAYLIST))
        val existingIconFile = iconDir.newFile("zen.png")
        val action = ManagePlaylistAction(dao)

        action.deletePlaylist(MediaId(TYPE_PLAYLISTS, SAMPLE_PLAYLIST.id.toString()))

        existingIconFile.shouldNotExist()
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

    private fun ManagePlaylistAction(dao: PlaylistDao) = ManagePlaylistAction(
        playlistDao = dao,
        iconDir = { iconDir.root },
        baseIconUri = BASE_ICON_URI.toUri(),
        clock = clock,
        dispatchers = dispatchers
    )
}