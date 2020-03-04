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

package fr.nihilus.music.service.actions

import android.Manifest
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import fr.nihilus.music.core.database.playlists.PlaylistDao
import fr.nihilus.music.core.media.CustomActions
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_MOST_RATED
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_RECENTLY_ADDED
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ALBUMS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ARTISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_PLAYLISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ROOT
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.core.media.MediaId.Builder.encode
import fr.nihilus.music.core.os.PermissionDeniedException
import fr.nihilus.music.media.provider.MediaDao
import io.kotlintest.shouldThrow
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.truth.os.BundleSubject.assertThat as assertThatBundle

@RunWith(AndroidJUnit4::class)
internal class DeleteActionTest {

    @MockK
    private lateinit var mediaDao: MediaDao

    @MockK
    private lateinit var playlistDao: PlaylistDao

    @Before
    fun setUp() = MockKAnnotations.init(this)

    @Test
    fun whenReadingName_thenReturnActionDeleteMediaConstant() {
        val action = DeleteAction(mediaDao, playlistDao)
        assertThat(action.name).isEqualTo(CustomActions.ACTION_DELETE_MEDIA)
    }

    @Test
    fun givenNoParameters_whenExecuting_thenFailWithMissingParameter() = runBlockingTest {
        val action = DeleteAction(mediaDao, playlistDao)
        val failure = shouldThrow<ActionFailure> {
            action.execute(Bundle.EMPTY)
        }

        assertThat(failure.errorCode).isEqualTo(CustomActions.ERROR_CODE_PARAMETER)
        assertThat(failure.errorMessage).contains(CustomActions.EXTRA_MEDIA_IDS)
    }

    private suspend fun assertUnsupported(mediaId: MediaId) {
        val action = DeleteAction(mediaDao, playlistDao)
        val failure = shouldThrow<ActionFailure> {
            action.execute(Bundle(1).apply {
                putStringArray(CustomActions.EXTRA_MEDIA_IDS, arrayOf(mediaId.encoded))
            })
        }

        assertThat(failure.errorCode).isEqualTo(CustomActions.ERROR_CODE_UNSUPPORTED_MEDIA_ID)
    }

    @Test
    fun givenDeniedPermissionAndTracks_whenExecuting_thenFailWithDeniedPermission() = runBlockingTest {
        coEvery { mediaDao.deleteTracks(any()) } throws PermissionDeniedException(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        val action = DeleteAction(mediaDao, playlistDao)
        val failure = shouldThrow<ActionFailure> {
            action.execute(Bundle(1).apply {
                putStringArray(CustomActions.EXTRA_MEDIA_IDS, arrayOf(
                    encode(TYPE_TRACKS, CATEGORY_ALL, 42)
                ))
            })
        }

        assertThat(failure.errorCode).isEqualTo(CustomActions.ERROR_CODE_PERMISSION_DENIED)
        assertThat(failure.errorMessage).isEqualTo(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    @Test
    fun givenInvalidMediaIds_whenExecuting_thenFailWithUnsupportedParameter() = runBlockingTest {
        assertUnsupported(MediaId(TYPE_ROOT))
        assertUnsupported(MediaId(TYPE_TRACKS))
        assertUnsupported(MediaId(TYPE_ALBUMS))
        assertUnsupported(MediaId(TYPE_ARTISTS))
        assertUnsupported(MediaId(TYPE_PLAYLISTS))
        assertUnsupported(MediaId(TYPE_TRACKS, CATEGORY_ALL))
        assertUnsupported(MediaId(TYPE_ALBUMS, "54"))
        assertUnsupported(MediaId(TYPE_ARTISTS, "13"))
        assertUnsupported(MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED))
        assertUnsupported(MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED))
    }

    @Test
    fun givenExistingTrackMediaIds_whenExecuting_thenDeleteThoseTracks() = runBlockingTest {
        val deletedMediaIds = arrayOf(
            encode(TYPE_TRACKS, CATEGORY_ALL, 16L),
            encode(TYPE_TRACKS, CATEGORY_ALL, 42L)
        )

        coEvery { mediaDao.deleteTracks(any()) } returns 2

        val action = DeleteAction(mediaDao, playlistDao)
        action.execute(Bundle(1).apply {
            putStringArray(CustomActions.EXTRA_MEDIA_IDS, deletedMediaIds)
        })

        coVerify { mediaDao.deleteTracks(longArrayOf(16L, 42L)) }
    }

    @Test
    fun givenExistingPlaylistIds_whenExecuting_thenDeleteThosePlaylists() = runBlockingTest {
        coEvery { playlistDao.deletePlaylist(any()) } just Runs

        val deletedMediaIds = arrayOf(
            encode(TYPE_PLAYLISTS, "1"),
            encode(TYPE_PLAYLISTS, "2")
        )

        val action = DeleteAction(mediaDao, playlistDao)
        action.execute(Bundle(1).apply {
            putStringArray(CustomActions.EXTRA_MEDIA_IDS, deletedMediaIds)
        })

        coVerifyAll {
            playlistDao.deletePlaylist(1L)
            playlistDao.deletePlaylist(2L)
        }
    }

    @Test
    fun givenExistingTrackIds_whenExecuting_thenReturnTheNumberOfDeletedTracks() = runBlockingTest {
        val deletedMediaIds = arrayOf(
            encode(TYPE_TRACKS, CATEGORY_ALL, 16L),
            encode(TYPE_TRACKS, CATEGORY_ALL, 42L)
        )

        coEvery { mediaDao.deleteTracks(any()) } returns 2

        val action = DeleteAction(mediaDao, playlistDao)
        val result = action.execute(Bundle(1).apply {
            putStringArray(CustomActions.EXTRA_MEDIA_IDS, deletedMediaIds)
        })

        assertThat(result).isNotNull()
        assertThatBundle(result).integer(CustomActions.RESULT_TRACK_COUNT).isEqualTo(2)
    }
}