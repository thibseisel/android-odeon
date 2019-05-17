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

import android.os.Bundle
import androidx.test.runner.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import fr.nihilus.music.media.AppDispatchers
import fr.nihilus.music.media.MediaId
import fr.nihilus.music.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.media.MediaId.Builder.CATEGORY_MOST_RATED
import fr.nihilus.music.media.MediaId.Builder.CATEGORY_RECENTLY_ADDED
import fr.nihilus.music.media.MediaId.Builder.TYPE_ALBUMS
import fr.nihilus.music.media.MediaId.Builder.TYPE_ARTISTS
import fr.nihilus.music.media.MediaId.Builder.TYPE_PLAYLISTS
import fr.nihilus.music.media.MediaId.Builder.TYPE_ROOT
import fr.nihilus.music.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.media.MediaId.Builder.encode
import fr.nihilus.music.media.assertThrows
import fr.nihilus.music.media.playlists.PlaylistDao
import fr.nihilus.music.media.provider.Album
import fr.nihilus.music.media.provider.Artist
import fr.nihilus.music.media.provider.MediaProvider
import fr.nihilus.music.media.provider.Track
import fr.nihilus.music.media.stub
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import fr.nihilus.music.media.MediaId.Builder.fromParts as mediaId
import androidx.test.ext.truth.os.BundleSubject.assertThat as assertThatBundle

@RunWith(AndroidJUnit4::class)
class DeleteActionTest {

    @[JvmField Rule]
    val mockitoRule: MockitoRule = MockitoJUnit.rule()

    private val dispatcher = TestCoroutineDispatcher()

    @Mock
    private lateinit var provider: MediaProvider
    @Mock
    private lateinit var playlistDao: PlaylistDao

    @Test
    fun whenReadingName_thenReturnActionDeleteMediaConstant() {
        val action = DeleteAction(provider, playlistDao, AppDispatchers(dispatcher))
        assertThat(action.name).isEqualTo(CustomActions.ACTION_DELETE_MEDIA)
    }

    @Test
    fun givenNoParameters_whenExecuting_thenFailWithMissingParameter() = dispatcher.runBlockingTest {
        val action = DeleteAction(provider, playlistDao, AppDispatchers(dispatcher))
        val failure = assertThrows<ActionFailure> {
            action.execute(Bundle.EMPTY)
        }

        assertThat(failure.errorCode).isEqualTo(CustomActions.ERROR_CODE_PARAMETER)
        assertThat(failure.errorMessage).contains(CustomActions.EXTRA_MEDIA_IDS)
    }

    private suspend fun assertUnsupported(mediaId: MediaId) {
        val action = DeleteAction(provider, playlistDao, AppDispatchers(dispatcher))
        val failure = assertThrows<ActionFailure> {
            action.execute(Bundle(1).apply {
                putStringArray(CustomActions.EXTRA_MEDIA_IDS, arrayOf(mediaId.encoded))
            })
        }

        assertThat(failure.errorCode).isEqualTo(CustomActions.ERROR_CODE_UNSUPPORTED_MEDIA_ID)
    }

    @Test
    fun givenInvalidMediaIds_whenExecuting_thenFailWithUnsupportedParameter() = dispatcher.runBlockingTest {
        assertUnsupported(mediaId(TYPE_ROOT))
        assertUnsupported(mediaId(TYPE_TRACKS))
        assertUnsupported(mediaId(TYPE_ALBUMS))
        assertUnsupported(mediaId(TYPE_ARTISTS))
        assertUnsupported(mediaId(TYPE_PLAYLISTS))
        assertUnsupported(mediaId(TYPE_TRACKS, CATEGORY_ALL))
        assertUnsupported(mediaId(TYPE_ALBUMS, "54"))
        assertUnsupported(mediaId(TYPE_ARTISTS, "13"))
        assertUnsupported(mediaId(TYPE_TRACKS, CATEGORY_MOST_RATED))
        assertUnsupported(mediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED))
    }

    @Test
    fun givenExistingTrackMediaIds_whenExecuting_thenDeleteThoseTracks() = dispatcher.runBlockingTest {
        val deletedMediaIds = arrayOf(
            encode(TYPE_TRACKS, CATEGORY_ALL, 16L),
            encode(TYPE_TRACKS, CATEGORY_ALL, 42L)
        )

        given(provider.deleteTracks(any())).willReturn(2)

        val action = DeleteAction(provider, playlistDao, AppDispatchers(dispatcher))
        action.execute(Bundle(1).apply {
            putStringArray(CustomActions.EXTRA_MEDIA_IDS, deletedMediaIds)
        })

        then(provider).should().deleteTracks(longArrayOf(16L, 42L))
    }

    @Test
    fun givenExistingPlaylistIds_whenExecuting_thenDeleteThosePlaylists() = dispatcher.runBlockingTest {
        val deletedMediaIds = arrayOf(
            encode(TYPE_PLAYLISTS, "1"),
            encode(TYPE_PLAYLISTS, "2")
        )

        val action = DeleteAction(provider, playlistDao, AppDispatchers(dispatcher))
        action.execute(Bundle(1).apply {
            putStringArray(CustomActions.EXTRA_MEDIA_IDS, deletedMediaIds)
        })

        then(playlistDao).should().deletePlaylist(eq(1L))
        then(playlistDao).should().deletePlaylist(eq(2L))
    }

    @Test
    fun givenExistingTrackIds_whenExecuting_thenReturnTheNumberOfDeletedTracks() = dispatcher.runBlockingTest {
        val deletedMediaIds = arrayOf(
            encode(TYPE_TRACKS, CATEGORY_ALL, 16L),
            encode(TYPE_TRACKS, CATEGORY_ALL, 42L)
        )

        val provider = DeletingMediaProvider(16L, 42L)

        val action = DeleteAction(provider, playlistDao, AppDispatchers(dispatcher))
        val result = action.execute(Bundle(1).apply {
            putStringArray(CustomActions.EXTRA_MEDIA_IDS, deletedMediaIds)
        })

        assertThat(result).isNotNull()
        assertThatBundle(result).integer(CustomActions.RESULT_TRACK_COUNT).isEqualTo(2)
    }
}

private class DeletingMediaProvider(
    vararg existingTrackIds: Long
) : MediaProvider {

    private val existingTracks = existingTrackIds.toMutableSet()

    override fun deleteTracks(trackIds: LongArray): Int {
        var deletedTracks = 0
        for (trackId in trackIds) {
            if (existingTracks.remove(trackId)) {
                deletedTracks++
            }
        }

        return deletedTracks
    }

    override fun queryTracks(): List<Track> = stub()
    override fun queryAlbums(): List<Album> = stub()
    override fun queryArtists(): List<Artist> = stub()
    override fun registerObserver(observer: MediaProvider.Observer): Unit = stub()
    override fun unregisterObserver(observer: MediaProvider.Observer): Unit = stub()
}