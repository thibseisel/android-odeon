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

package fr.nihilus.music.media.playlists

import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import fr.nihilus.music.core.context.AppDispatchers
import fr.nihilus.music.core.database.playlists.Playlist
import fr.nihilus.music.core.database.playlists.PlaylistDao
import fr.nihilus.music.core.database.playlists.PlaylistTrack
import fr.nihilus.music.core.test.coroutines.CoroutineTestRule
import fr.nihilus.music.core.test.coroutines.flow.infiniteFlowOf
import fr.nihilus.music.core.test.os.TestClock
import fr.nihilus.music.media.tracks.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.file.shouldContainFile
import io.kotest.matchers.file.shouldNotBeEmpty
import io.kotest.matchers.file.shouldNotExist
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test

private val BASE_ICON_URI = "content://fr.nihilus.music.test.provider/icons".toUri()

@RunWith(AndroidJUnit4::class)
internal class PlaylistRepositoryTest {
    @get:Rule val test = CoroutineTestRule()
    @get:Rule val iconDir = TemporaryFolder()

    @MockK private lateinit var mockPlaylists: PlaylistDao
    @MockK private lateinit var mockTracks: TrackRepository

    private lateinit var fakeClock: TestClock
    private lateinit var repository: PlaylistRepository

    @BeforeTest
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        fakeClock = TestClock(0)
        repository = PlaylistRepository(
            playlistDao = mockPlaylists,
            trackRepository = mockTracks,
            iconDir = { iconDir.root },
            baseIconUri = BASE_ICON_URI,
            clock = fakeClock,
            dispatchers = AppDispatchers(test.dispatcher)
        )
    }

    @Test
    fun `playlists - lists all playlists by ascending creation date`() = test {
        every { mockPlaylists.playlists } returns infiniteFlowOf(
            listOf(PLAYLIST_FAVORITES, PLAYLIST_ZEN, PLAYLIST_SPORT)
        )

        val allPlaylists = repository.playlists.first()

        allPlaylists.shouldContainExactly(
            PLAYLIST_FAVORITES,
            PLAYLIST_ZEN,
            PLAYLIST_SPORT,
        )
    }

    @Test
    fun `playlists - emits whenever playlists change`() = test {
        val livePlaylists = MutableStateFlow(
            listOf(PLAYLIST_FAVORITES, PLAYLIST_ZEN)
        )
        every { mockPlaylists.playlists } returns livePlaylists

        repository.playlists.drop(1).test {
            livePlaylists.value = listOf(PLAYLIST_ZEN, PLAYLIST_SPORT)
            awaitItem()

            livePlaylists.value = listOf(PLAYLIST_ZEN)
            awaitItem()
            expectNoEvents()
        }
    }

    @Test
    fun `getPlaylistTracks - lists tracks from that playlist sorted by position`() = test {
        every { mockTracks.tracks } returns infiniteFlowOf(
            listOf(ALGORITHM, DIRTY_WATER, KNIGHTS_OF_CYDONIA, NIGHTMARE)
        )
        every { mockPlaylists.playlists } returns infiniteFlowOf(
            listOf(PLAYLIST_FAVORITES)
        )
        every { mockPlaylists.getPlaylistTracks(eq(PLAYLIST_FAVORITES.id)) } returns infiniteFlowOf(
            listOf(
                PlaylistTrack(PLAYLIST_FAVORITES.id, KNIGHTS_OF_CYDONIA.id, position = 0),
                PlaylistTrack(PLAYLIST_FAVORITES.id, ALGORITHM.id, position = 1),
                PlaylistTrack(PLAYLIST_FAVORITES.id, DIRTY_WATER.id, position = 2),
            )
        )

        val playlistTracks = repository.getPlaylistTracks(PLAYLIST_FAVORITES.id).first()

        playlistTracks.shouldContainExactly(
            KNIGHTS_OF_CYDONIA,
            ALGORITHM,
            DIRTY_WATER,
        )
    }

    @Test
    fun `getPlaylistTracks - emit whenever tracks or members change`() = test {
        val liveTracks = MutableStateFlow(
            listOf(ALGORITHM, KNIGHTS_OF_CYDONIA)
        )
        val liveMembers = MutableStateFlow(
            listOf(
                PlaylistTrack(PLAYLIST_FAVORITES.id, ALGORITHM.id, 0),
                PlaylistTrack(PLAYLIST_FAVORITES.id, KNIGHTS_OF_CYDONIA.id, 1),
            )
        )
        every { mockTracks.tracks } returns liveTracks
        every { mockPlaylists.getPlaylistTracks(eq(PLAYLIST_FAVORITES.id)) } returns liveMembers

        repository.getPlaylistTracks(PLAYLIST_FAVORITES.id).drop(1).test {
            liveTracks.value = listOf(ALGORITHM, KNIGHTS_OF_CYDONIA, NIGHTMARE)
            awaitItem()

            liveMembers.value += PlaylistTrack(PLAYLIST_FAVORITES.id, NIGHTMARE.id, 2)
            awaitItem()

            expectNoEvents()
        }
    }

    @Test
    fun `getPlaylistTracks - omits tracks with no match in playlist members`() = test {
        every { mockTracks.tracks } returns infiniteFlowOf(
            listOf(ALGORITHM, KNIGHTS_OF_CYDONIA)
        )
        every { mockPlaylists.getPlaylistTracks(eq(PLAYLIST_FAVORITES.id)) } returns infiniteFlowOf(
            listOf(
                PlaylistTrack(PLAYLIST_FAVORITES.id, KNIGHTS_OF_CYDONIA.id, 0),
                PlaylistTrack(PLAYLIST_FAVORITES.id, ALGORITHM.id, 1),
                PlaylistTrack(PLAYLIST_FAVORITES.id, NIGHTMARE.id, 2),
                PlaylistTrack(PLAYLIST_FAVORITES.id, DIRTY_WATER.id, 3)
            )
        )

        val playlistTracks = repository.getPlaylistTracks(PLAYLIST_FAVORITES.id).first()

        playlistTracks.shouldContainExactly(
            KNIGHTS_OF_CYDONIA,
            ALGORITHM,
        )
    }

    @Test
    fun `getPlaylistTracks - returns empty list for unknown playlist`() = test {
        every { mockTracks.tracks } returns infiniteFlowOf(
            listOf(ALGORITHM, DIRTY_WATER, KNIGHTS_OF_CYDONIA, NIGHTMARE)
        )
        every { mockPlaylists.playlists } returns infiniteFlowOf(emptyList())
        every { mockPlaylists.getPlaylistTracks(any()) } returns infiniteFlowOf(emptyList())

        val playlistTracks = repository.getPlaylistTracks(42).first()
        playlistTracks.shouldBeEmpty()
    }

    @Test
    fun `createUserPlaylist - adds a new playlist`() = test {
        coEvery { mockPlaylists.savePlaylist(any()) } returns 42L

        val createdPlaylist = repository.createUserPlaylist("My playlist")
        createdPlaylist shouldBe Playlist(
            id = 42L,
            title = "My playlist",
            created = fakeClock.currentEpochTime,
            iconUri = "content://fr.nihilus.music.test.provider/icons/My_playlist.png".toUri()
        )

        coVerifyAll {
            mockPlaylists.savePlaylist(
                Playlist(
                    id = 0,
                    title = "My playlist",
                    created = fakeClock.currentEpochTime,
                    iconUri = "content://fr.nihilus.music.test.provider/icons/My_playlist.png".toUri()
                )
            )
        }
    }

    @Test
    fun `createUserPlaylist - generates a playlist icon on device storage`() = test {
        coEvery { mockPlaylists.savePlaylist(any()) } returns 42L

        repository.createUserPlaylist("My playlist")

        iconDir.root shouldContainFile "My_playlist.png"
        val iconFile = File(iconDir.root, "My_playlist.png")
        iconFile.shouldBeAFile()
        iconFile.shouldNotBeEmpty()
    }

    @Test
    fun `createUserPlaylist - throws IAE when name is blank`() = test {
        shouldThrow<IllegalArgumentException> {
            repository.createUserPlaylist(" ")
        }
    }

    @Test
    fun `addTracksToPlaylist - appends tracks to the end of the playlist`() = test {
        every { mockPlaylists.playlists } returns infiniteFlowOf(
            listOf(PLAYLIST_FAVORITES)
        )
        every { mockTracks.tracks } returns infiniteFlowOf(
            listOf(ALGORITHM, DIRTY_WATER, KNIGHTS_OF_CYDONIA, NIGHTMARE)
        )

        repository.addTracksToPlaylist(
            playlistId = PLAYLIST_FAVORITES.id,
            trackIds = longArrayOf(KNIGHTS_OF_CYDONIA.id, DIRTY_WATER.id, NIGHTMARE.id)
        )

        coVerify {
            mockPlaylists.addTracks(
                listOf(
                    PlaylistTrack(
                        playlistId = PLAYLIST_FAVORITES.id,
                        trackId = KNIGHTS_OF_CYDONIA.id,
                        position = 0
                    ),
                    PlaylistTrack(
                        playlistId = PLAYLIST_FAVORITES.id,
                        trackId = DIRTY_WATER.id,
                        position = 0
                    ),
                    PlaylistTrack(
                        playlistId = PLAYLIST_FAVORITES.id,
                        trackId = NIGHTMARE.id,
                        position = 0
                    ),
                )
            )
        }
    }

    @Test
    fun `addTracksToPlaylist - throws IAE when no playlist matches id`() = test {
        every { mockPlaylists.playlists } returns infiniteFlowOf(emptyList())
        every { mockTracks.tracks } returns infiniteFlowOf(
            listOf(ALGORITHM, DIRTY_WATER, KNIGHTS_OF_CYDONIA, NIGHTMARE)
        )

        shouldThrow<IllegalArgumentException> {
            repository.addTracksToPlaylist(
                playlistId = 42,
                trackIds = longArrayOf(ALGORITHM.id)
            )
        }

        coVerify(inverse = true) {
            mockPlaylists.createPlaylist(any(), any())
            mockPlaylists.addTracks(any())
        }
    }

    @Test
    fun `addTracksToPlaylist - throws IAE when track ids are invalid`() = test {
        every { mockPlaylists.playlists } returns infiniteFlowOf(
            listOf(PLAYLIST_FAVORITES)
        )
        every { mockTracks.tracks } returns infiniteFlowOf(
            listOf(ALGORITHM, DIRTY_WATER, KNIGHTS_OF_CYDONIA, NIGHTMARE)
        )

        shouldThrow<IllegalArgumentException> {
            repository.addTracksToPlaylist(
                playlistId = PLAYLIST_FAVORITES.id,
                trackIds = longArrayOf(ISOLATED_SYSTEM.id, DIRTY_WATER.id)
            )
        }

        coVerify(inverse = true) {
            mockPlaylists.createPlaylist(any(), any())
            mockPlaylists.addTracks(any())
        }
    }

    @Test
    fun `deletePlaylist - deletes playlist matching id`() = test {
        every { mockPlaylists.playlists } returns infiniteFlowOf(
            listOf(PLAYLIST_FAVORITES)
        )

        repository.deletePlaylist(PLAYLIST_FAVORITES.id)

        coVerify {
            mockPlaylists.deletePlaylist(PLAYLIST_FAVORITES.id)
        }
    }

    @Test
    fun `deletePlaylist - do nothing when playlist does not exist`() = test {
        every { mockPlaylists.playlists } returns infiniteFlowOf(
            listOf(PLAYLIST_FAVORITES)
        )

        repository.deletePlaylist(playlistId = PLAYLIST_ZEN.id)

        coVerify(exactly = 0) {
            mockPlaylists.deletePlaylist(any())
        }
    }

    @Test
    fun `deletePlaylist - deletes playlist icon from device storage`() = test {
        @Suppress("BlockingMethodInNonBlockingContext")
        val existingIconFile = iconDir.newFile("My_favorites.png")
        every { mockPlaylists.playlists } returns infiniteFlowOf(
            listOf(PLAYLIST_FAVORITES)
        )

        repository.deletePlaylist(playlistId = PLAYLIST_FAVORITES.id)

        existingIconFile.shouldNotExist()
    }
}
