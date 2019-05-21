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

package fr.nihilus.music.media.actions

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import fr.nihilus.music.media.AppDispatchers
import fr.nihilus.music.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.media.MediaId.Builder.TYPE_PLAYLISTS
import fr.nihilus.music.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.media.MediaId.Builder.encode
import fr.nihilus.music.media.assertThrows
import fr.nihilus.music.media.fail
import fr.nihilus.music.media.os.FileSystem
import fr.nihilus.music.media.playlists.Playlist
import fr.nihilus.music.media.playlists.PlaylistDao
import fr.nihilus.music.media.playlists.PlaylistTrack
import fr.nihilus.music.media.playlists.TestPlaylistDao
import fr.nihilus.music.media.stub
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith

private const val NEW_PLAYLIST_TITLE = "New playlist"

private val INITIAL_PLAYLIST = Playlist(2L, NEW_PLAYLIST_TITLE, 0L, null)

@RunWith(AndroidJUnit4::class)
class ManagePlaylistActionTest {

    private val dispatcher = TestCoroutineDispatcher()

    @Test
    fun whenReadingName_thenReturnActionManagePlaylistConstant() {
        val action = ManagePlaylistAction(StubPlaylistDao, NoopFileSystem, AppDispatchers(dispatcher))
        assertThat(action.name).isEqualTo(CustomActions.ACTION_MANAGE_PLAYLIST)
    }

    @Test
    fun givenNoParams_whenExecuting_thenFailWithMissingParameter() = dispatcher.runBlockingTest {
        val action = ManagePlaylistAction(StubPlaylistDao, NoopFileSystem, AppDispatchers(dispatcher))
        val failure = assertThrows<ActionFailure> {
            action.execute(Bundle.EMPTY)
        }

        assertThat(failure.errorCode).isEqualTo(CustomActions.ERROR_CODE_PARAMETER)
    }

    @Test
    fun givenNoPlaylistId_whenExecutingWithTitleParam_thenCreateNewEmptyPlaylist() = dispatcher.runBlockingTest {
        val playlistDao = TestPlaylistDao()
        val action = ManagePlaylistAction(playlistDao, NoopFileSystem, AppDispatchers(dispatcher))
        action.execute(Bundle(1).apply {
            putString(CustomActions.EXTRA_TITLE, NEW_PLAYLIST_TITLE)
        })

        val addedPlaylist = playlistDao.playlists.firstOrNull() ?: fail("Expected a playlist to be created.")
        assertThat(addedPlaylist.title).isEqualTo(NEW_PLAYLIST_TITLE)
    }

    @Test
    fun givenTrackMediaIds_whenExecutingWithoutTitleParam_thenFailWithMissingTitleParameter() = dispatcher.runBlockingTest {
        val trackMediaIds = arrayOf(
            encode(TYPE_TRACKS, CATEGORY_ALL, 42L)
        )

        val action = ManagePlaylistAction(StubPlaylistDao, NoopFileSystem, AppDispatchers(dispatcher))
        val failure = assertThrows<ActionFailure> {
            action.execute(Bundle(1).apply {
                putStringArray(CustomActions.EXTRA_MEDIA_IDS, trackMediaIds)
            })
        }

        assertThat(failure.errorCode).isEqualTo(CustomActions.ERROR_CODE_PARAMETER)
        assertThat(failure.errorMessage).contains(CustomActions.EXTRA_TITLE)
    }

    @Test
    fun givenTitleAndNonTrackMediaIds_whenExecuting_thenFailWithInvalidMediaId() = dispatcher.runBlockingTest {
        val mediaIds = arrayOf(encode(TYPE_TRACKS, CATEGORY_ALL))

        val action = ManagePlaylistAction(StubPlaylistDao, NoopFileSystem, AppDispatchers(dispatcher))
        val failure = assertThrows<ActionFailure> {
            action.execute(Bundle(2).apply {
                putString(CustomActions.EXTRA_TITLE, NEW_PLAYLIST_TITLE)
                putStringArray(CustomActions.EXTRA_MEDIA_IDS, mediaIds)
            })
        }

        assertThat(failure.errorCode).isEqualTo(CustomActions.ERROR_CODE_UNSUPPORTED_MEDIA_ID)
    }

    @Test
    fun givenTitleAndTrackMediaIds_whenExecuting_thenCreateNewPlaylistWithGivenTracks() = dispatcher.runBlockingTest {
        val trackMediaIds = arrayOf(
            encode(TYPE_TRACKS, CATEGORY_ALL, 16L),
            encode(TYPE_TRACKS, CATEGORY_ALL, 42L)
        )

        val playlistDao = TestPlaylistDao()
        val action = ManagePlaylistAction(playlistDao, NoopFileSystem, AppDispatchers(dispatcher))
        action.execute(Bundle(2).apply {
            putString(CustomActions.EXTRA_TITLE, NEW_PLAYLIST_TITLE)
            putStringArray(CustomActions.EXTRA_MEDIA_IDS, trackMediaIds)
        })

        // Then a new playlist has been created.
        val addedPlaylist = playlistDao.playlists.firstOrNull() ?: fail("Expected a playlist to be created.")
        assertThat(addedPlaylist.id).isNotEqualTo(0L)
        assertThat(addedPlaylist.title).isEqualTo(NEW_PLAYLIST_TITLE)

        // Then both tracks are added to that playlist in order.
        val addedTracks = playlistDao.playlistTracks
        assertThat(addedTracks).hasSize(2)
        val (firstTrack, secondTrack) = addedTracks

        assertThat(firstTrack.playlistId).isEqualTo(addedPlaylist.id)
        assertThat(firstTrack.trackId).isEqualTo(16L)

        assertThat(secondTrack.playlistId).isEqualTo(addedPlaylist.id)
        assertThat(secondTrack.trackId).isEqualTo(42L)
    }

    @Test
    fun givenPlaylistId_whenExecutingWithoutTrackMediaIds_thenFailWithMissingParameter() = dispatcher.runBlockingTest {
        val action = ManagePlaylistAction(StubPlaylistDao, NoopFileSystem, AppDispatchers(dispatcher))
        val failure = assertThrows<ActionFailure> {
            action.execute(Bundle(1).apply {
                putString(CustomActions.EXTRA_PLAYLIST_ID, encode(TYPE_PLAYLISTS, "2"))
            })
        }

        assertThat(failure.errorCode).isEqualTo(CustomActions.ERROR_CODE_PARAMETER)
    }

    @Test
    fun givenNonPlaylistMediaId_whenExecuting_thenFailWithInvalidMediaId() = dispatcher.runBlockingTest {
        val nonPlaylistMediaId = encode(TYPE_TRACKS, "2")
        val trackMediaIds = arrayOf(
            encode(TYPE_TRACKS, CATEGORY_ALL, 16L),
            encode(TYPE_TRACKS, CATEGORY_ALL, 42L)
        )

        val action = ManagePlaylistAction(StubPlaylistDao, NoopFileSystem, AppDispatchers(dispatcher))
        val failure = assertThrows<ActionFailure> {
            action.execute(Bundle(2).apply {
                putString(CustomActions.EXTRA_PLAYLIST_ID, nonPlaylistMediaId)
                putStringArray(CustomActions.EXTRA_MEDIA_IDS, trackMediaIds)
            })
        }

        assertThat(failure.errorCode).isEqualTo(CustomActions.ERROR_CODE_UNSUPPORTED_MEDIA_ID)
    }

    @Test
    fun givenPlaylistIdAndTrackMediaIds_whenExecuting_thenAddTracksToThatPlaylist() = dispatcher.runBlockingTest {
        val playlistDao = TestPlaylistDao(initialPlaylists = listOf(INITIAL_PLAYLIST))
        val trackMediaIds = arrayOf(
            encode(TYPE_TRACKS, CATEGORY_ALL, 16L),
            encode(TYPE_TRACKS, CATEGORY_ALL, 42L)
        )

        val action = ManagePlaylistAction(playlistDao, NoopFileSystem, AppDispatchers(dispatcher))
        action.execute(Bundle(2).apply {
            putString(CustomActions.EXTRA_PLAYLIST_ID, encode(TYPE_PLAYLISTS, "2"))
            putStringArray(CustomActions.EXTRA_MEDIA_IDS, trackMediaIds)
        })

        // Then add those 2 tracks to the playlist
        val addedTracks = playlistDao.playlistTracks
        assertThat(addedTracks).hasSize(2)

        val (firstTrack, secondTrack) = addedTracks
        assertThat(firstTrack.playlistId).isEqualTo(2L)
        assertThat(firstTrack.trackId).isEqualTo(16L)

        assertThat(secondTrack.playlistId).isEqualTo(2L)
        assertThat(secondTrack.trackId).isEqualTo(42L)
    }
}

private object StubPlaylistDao : PlaylistDao {
    override val playlistsFlow: Flowable<List<Playlist>> get() = stub()
    override fun getPlaylistTracks(playlistId: Long): Single<List<PlaylistTrack>> = stub()
    override fun getPlaylistsHavingTracks(trackIds: LongArray): Single<LongArray> = stub()
    override fun savePlaylist(playlist: Playlist): Long = stub()
    override fun addTracks(tracks: List<PlaylistTrack>): Unit = stub()
    override fun deletePlaylist(playlistId: Long): Unit = stub()
    override fun deletePlaylistTracks(trackIds: LongArray): Unit = stub()
}

private object NoopFileSystem : FileSystem {
    override fun writeBitmapToInternalStorage(filepath: String, bitmap: Bitmap): Uri? = null
    override fun deleteFile(filepath: String): Boolean = true
}