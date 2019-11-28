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

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import fr.nihilus.music.core.context.AppDispatchers
import fr.nihilus.music.core.media.CustomActions
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_PLAYLISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.core.media.MediaId.Builder.encode
import fr.nihilus.music.core.os.FileSystem
import fr.nihilus.music.core.test.rules.CoroutineTestRule
import fr.nihilus.music.media.repo.MediaRepository
import io.kotlintest.shouldThrow
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val NEW_PLAYLIST_TITLE = "New playlist"

@RunWith(AndroidJUnit4::class)
internal class ManagePlaylistActionTest {

    @get:Rule
    val test = CoroutineTestRule()

    private val dispatchers = AppDispatchers(test.dispatcher)

    @MockK
    private lateinit var repository: MediaRepository

    @Before
    fun setupMocks() = MockKAnnotations.init(this)

    @Test
    fun `Action name should be the constant ActionManagePlaylist`() {
        val action = ManagePlaylistAction(repository, NoopFileSystem, dispatchers)
        assertThat(action.name).isEqualTo(CustomActions.ACTION_MANAGE_PLAYLIST)
    }

    @Test
    fun `Given no params, when executing then fail with missing parameters`() = test.run {
        val action = ManagePlaylistAction(repository, NoopFileSystem, dispatchers)
        val failure = shouldThrow<ActionFailure> {
            action.execute(Bundle.EMPTY)
        }

        assertThat(failure.errorCode).isEqualTo(CustomActions.ERROR_CODE_PARAMETER)
    }

    @Test
    fun `Given no playlist id but a title, when executing then create a new empty playlist`() = test.run {
        coEvery { repository.createPlaylist(any(), any()) } just Runs

        val action = ManagePlaylistAction(repository, NoopFileSystem, dispatchers)
        action.execute(Bundle(1).apply {
            putString(CustomActions.EXTRA_TITLE, NEW_PLAYLIST_TITLE)
        })

        coVerify(exactly = 1) {
            repository.createPlaylist(
                newPlaylist = match { it.title == NEW_PLAYLIST_TITLE },
                trackIds = LongArray(0)
            )
        }
    }

    @Test
    fun `Given track media ids, when executing without title param then fail with missing title`() = test.run {
        val trackMediaIds = arrayOf(
            encode(TYPE_TRACKS, CATEGORY_ALL, 42L)
        )

        val action = ManagePlaylistAction(repository, NoopFileSystem, dispatchers)
        val failure = shouldThrow<ActionFailure> {
            action.execute(Bundle(1).apply {
                putStringArray(CustomActions.EXTRA_MEDIA_IDS, trackMediaIds)
            })
        }

        assertThat(failure.errorCode).isEqualTo(CustomActions.ERROR_CODE_PARAMETER)
        assertThat(failure.errorMessage).contains(CustomActions.EXTRA_TITLE)
    }

    @Test
    fun `Given title and non track media ids, when executing then fail with invalid media id`() = test.run {
        val mediaIds = arrayOf(encode(TYPE_TRACKS, CATEGORY_ALL))

        val action = ManagePlaylistAction(repository, NoopFileSystem, dispatchers)
        val failure = shouldThrow<ActionFailure> {
            action.execute(Bundle(2).apply {
                putString(CustomActions.EXTRA_TITLE, NEW_PLAYLIST_TITLE)
                putStringArray(CustomActions.EXTRA_MEDIA_IDS, mediaIds)
            })
        }

        assertThat(failure.errorCode).isEqualTo(CustomActions.ERROR_CODE_UNSUPPORTED_MEDIA_ID)
    }

    @Test
    fun `Given title and track media ids, when executing then create a new playlist with those tracks`() = test.run {
        val trackMediaIds = arrayOf(
            encode(TYPE_TRACKS, CATEGORY_ALL, 16L),
            encode(TYPE_TRACKS, CATEGORY_ALL, 42L)
        )

        coEvery { repository.createPlaylist(any(), any()) } just Runs

        val action = ManagePlaylistAction(repository, NoopFileSystem, dispatchers)
        action.execute(Bundle(2).apply {
            putString(CustomActions.EXTRA_TITLE, NEW_PLAYLIST_TITLE)
            putStringArray(CustomActions.EXTRA_MEDIA_IDS, trackMediaIds)
        })

        // Then a new playlist has been created with those tracks.
        coVerify {
            repository.createPlaylist(
                newPlaylist = match { it.title == NEW_PLAYLIST_TITLE },
                trackIds = longArrayOf(16L, 42L)
            )
        }
    }

    @Test
    fun `Given playlist id, when executing without track media ids then fail with missing parameter`() = test.run {
        val action = ManagePlaylistAction(repository, NoopFileSystem, dispatchers)
        val failure = shouldThrow<ActionFailure> {
            action.execute(Bundle(1).apply {
                putString(CustomActions.EXTRA_PLAYLIST_ID, encode(TYPE_PLAYLISTS, "2"))
            })
        }

        assertThat(failure.errorCode).isEqualTo(CustomActions.ERROR_CODE_PARAMETER)
    }

    @Test
    fun `Given non playlist media id, when executing then fail with invalid parameter`() = test.run {
        val nonPlaylistMediaId = encode(TYPE_TRACKS, "2")
        val trackMediaIds = arrayOf(
            encode(TYPE_TRACKS, CATEGORY_ALL, 16L),
            encode(TYPE_TRACKS, CATEGORY_ALL, 42L)
        )

        val action = ManagePlaylistAction(repository, NoopFileSystem, dispatchers)
        val failure = shouldThrow<ActionFailure> {
            action.execute(Bundle(2).apply {
                putString(CustomActions.EXTRA_PLAYLIST_ID, nonPlaylistMediaId)
                putStringArray(CustomActions.EXTRA_MEDIA_IDS, trackMediaIds)
            })
        }

        assertThat(failure.errorCode).isEqualTo(CustomActions.ERROR_CODE_UNSUPPORTED_MEDIA_ID)
    }

    @Test
    fun `Given playlist id and track media ids, when executing then add tracks to that playlist`() = test.run {
        val trackMediaIds = arrayOf(
            encode(TYPE_TRACKS, CATEGORY_ALL, 16L),
            encode(TYPE_TRACKS, CATEGORY_ALL, 42L)
        )

        coEvery { repository.addTracksToPlaylist(any(), any()) } just Runs

        val action = ManagePlaylistAction(repository, NoopFileSystem, dispatchers)
        action.execute(Bundle(2).apply {
            putString(CustomActions.EXTRA_PLAYLIST_ID, encode(TYPE_PLAYLISTS, "2"))
            putStringArray(CustomActions.EXTRA_MEDIA_IDS, trackMediaIds)
        })

        // Then add those 2 tracks to the playlist.
        coVerify {
            repository.addTracksToPlaylist(
                playlistId = 2L,
                trackIds = longArrayOf(16L, 42L)
            )
        }
    }
}

private object NoopFileSystem : FileSystem {
    override fun makeSharedContentUri(filePath: String): Uri? = null
    override fun writeBitmapToInternalStorage(filepath: String, bitmap: Bitmap): Uri? = null
    override fun deleteFile(filepath: String): Boolean = true
}