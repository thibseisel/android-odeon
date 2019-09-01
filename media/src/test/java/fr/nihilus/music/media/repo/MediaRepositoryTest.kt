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

package fr.nihilus.music.media.repo

import com.google.common.truth.Truth.assertThat
import fr.nihilus.music.media.AppDispatchers
import fr.nihilus.music.media.assertThrows
import fr.nihilus.music.media.permissions.PermissionDeniedException
import fr.nihilus.music.media.playlists.*
import fr.nihilus.music.media.provider.*
import fr.nihilus.music.media.stub
import fr.nihilus.music.media.usage.MediaUsageDao
import fr.nihilus.music.media.usage.MediaUsageEvent
import fr.nihilus.music.media.usage.TrackScore
import fr.nihilus.music.media.usage.TrackUsage
import fr.nihilus.music.media.usingScope
import io.kotlintest.matchers.collections.shouldContainExactly
import io.reactivex.Completable
import io.reactivex.Flowable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Test

class MediaRepositoryTest {

    private val dispatcher = TestCoroutineDispatcher()
    private val scope = TestCoroutineScope(dispatcher)
    private val dispatchers: AppDispatchers
        get() = AppDispatchers(dispatcher)

    @After
    fun cleanup() {
        dispatcher.cleanupTestCoroutines()
    }

    private fun test(body: suspend TestCoroutineScope.() -> Unit) = dispatcher.runBlockingTest(body)

    private fun TestCoroutineScope.runInScope(block: suspend CoroutineScope.() -> Unit) {
        val job = launch(block = block)
        advanceUntilIdle()
        job.cancel()
    }

    @Test
    fun `When querying all tracks, then return tracks from Dao`() = test {
        println("Hello!")
        val dao = TestTrackDao(SAMPLE_TRACKS)

        runInScope {
            val repository =
                MediaRepositoryImpl(this, dao, DummyPlaylistDao, DummyUsageDao, dispatchers)
            val tracks = repository.getAllTracks()

            tracks shouldContainExactly SAMPLE_TRACKS
            println("Test finished normally.")
        }
    }

    @Test
    fun whenLoadingAllTracks_thenReturnTracksFromDao() {
        val mediaDao = TestTrackDao(SAMPLE_TRACKS)
        assertInitialLoadFromDao(mediaDao, SAMPLE_TRACKS, MediaRepository::getAllTracks)
    }

    @Test
    fun whenLoadingAllTracks_thenReturnLatestTrackList() {
        val initialTracks = listOf(SAMPLE_TRACKS[0])
        val updatedTracks = listOf(SAMPLE_TRACKS[1])
        val mediaDao = TestTrackDao(initialTracks)

        assertAlwaysLoadLatestMediaList(mediaDao, initialTracks, updatedTracks, MediaRepository::getAllTracks)
    }

    @Test
    fun givenFiniteTrackStream_whenLoadingAllTracks_thenStillReturnTheLatestTrackList() {
        val initialTracks = listOf(SAMPLE_TRACKS[0])
        val mediaDao = TestTrackDao(initialTracks)

        assertStillLoadsLatestMediaListAfterStreamCompleted(mediaDao, initialTracks, MediaRepository::getAllTracks)
    }

    @Test
    fun whenLoadingAllAlbums_thenReturnAlbumsFromDao() = dispatcher.runBlockingTest {
        val mediaDao = TestAlbumDao(SAMPLE_ALBUMS)
        assertInitialLoadFromDao(mediaDao, SAMPLE_ALBUMS, MediaRepository::getAllAlbums)
    }

    @Test
    fun whenLoadingAllAlbums_thenReturnLatestAlbumList() {
        val initialAlbums = listOf(SAMPLE_ALBUMS[0])
        val updatedAlbums = listOf(SAMPLE_ALBUMS[1])
        val mediaDao = TestAlbumDao(initialAlbums)

        assertAlwaysLoadLatestMediaList(mediaDao, initialAlbums, updatedAlbums, MediaRepository::getAllAlbums)
    }

    @Test
    fun givenFiniteAlbumStream_whenLoadingAllAlbums_thenStillReturnTheLatestAlbumList() {
        val initialAlbums = listOf(SAMPLE_ALBUMS[0])
        val mediaDao = TestAlbumDao(initialAlbums)

        assertStillLoadsLatestMediaListAfterStreamCompleted(mediaDao, initialAlbums, MediaRepository::getAllAlbums)
    }

    @Test
    fun whenLoadingAllArtists_thenReturnArtistsFromDao() {
        val mediaDao = TestArtistDao(SAMPLE_ARTISTS)
        assertInitialLoadFromDao(mediaDao, SAMPLE_ARTISTS, MediaRepository::getAllArtists)
    }

    @Test
    fun whenLoadingAllArtists_thenReturnLatestArtistList() {
        val initialArtists = listOf(SAMPLE_ARTISTS[0])
        val updatedArtists = listOf(SAMPLE_ARTISTS[1])
        val mediaDao = TestArtistDao(initialArtists)

        assertAlwaysLoadLatestMediaList(mediaDao, initialArtists, updatedArtists, MediaRepository::getAllArtists)
    }

    @Test
    fun givenFiniteArtistStream_whenLoadingAllArtists_thenStillReturnTheLatestArtistList() {
        val artists = listOf(SAMPLE_ARTISTS[0])
        val mediaDao = TestArtistDao(artists)

        assertStillLoadsLatestMediaListAfterStreamCompleted(mediaDao, artists, MediaRepository::getAllArtists)
    }

    @Test
    fun whenLoadingAllPlaylists_thenReturnPlaylistsFromDao() = dispatcher.runBlockingTest {
        val dao = TestPlaylistDao(
            SAMPLE_PLAYLISTS,
            SAMPLE_PLAYLIST_TRACKS
        )
        
        usingScope {
            val repository = MediaRepositoryImpl(it, DummyMediaDao, dao, DummyUsageDao, dispatchers)
            val playlists = repository.getAllPlaylists()
            assertThat(playlists).containsExactlyElementsIn(SAMPLE_PLAYLISTS).inOrder()
        }
    }

    @Test
    fun whenLoadingAllPlaylists_thenReturnLatestPlaylists() = dispatcher.runBlockingTest {
        val original = listOf(SAMPLE_PLAYLISTS[0])
        val updated = listOf(SAMPLE_PLAYLISTS[1])
        val dao = TestPlaylistDao(original, emptyList())

        usingScope {
            val repository = MediaRepositoryImpl(it, DummyMediaDao, dao, DummyUsageDao, dispatchers)
            repeat(2) {
                val currentPlaylists = repository.getAllPlaylists()
                assertThat(currentPlaylists).containsExactlyElementsIn(original).inOrder()
            }

            dao.update(updated)

            repeat(2) {
                val currentPlaylists = repository.getAllPlaylists()
                assertThat(currentPlaylists).containsExactlyElementsIn(updated).inOrder()
            }
        }
    }

    @Test
    fun givenNonExistingPlaylist_whenLoadingItsTracks_thenReturnNull() =
        dispatcher.runBlockingTest {
        val mediaDao = TestTrackDao(SAMPLE_TRACKS)
        val playlistDao = TestPlaylistDao(
            SAMPLE_PLAYLISTS,
            SAMPLE_PLAYLIST_TRACKS
        )

        usingScope {
            val repository = MediaRepositoryImpl(it, mediaDao, playlistDao, DummyUsageDao, dispatchers)
            val playlistTracks = repository.getPlaylistTracks(42L)

            assertThat(playlistTracks).named("Tracks of unknown playlist 42").isNull()
        }
    }

    @Test
    fun givenAnExistingPlaylist_whenLoadingItsTracks_thenReturnTracksFromThatPlaylist() =
        dispatcher.runBlockingTest {
        val mediaDao = TestTrackDao(SAMPLE_TRACKS)
        val playlistDao = TestPlaylistDao(
            SAMPLE_PLAYLISTS,
            SAMPLE_PLAYLIST_TRACKS
        )

        usingScope {
            val repository = MediaRepositoryImpl(it, mediaDao, playlistDao, DummyUsageDao, dispatchers)
            val playlistTracks = repository.getPlaylistTracks(1L)

            assertThat(playlistTracks).containsExactly(SAMPLE_TRACKS[1])
        }
    }

    @Test
    fun whenLoadingAllTracksAndErrorOccurs_thenThrowThatError() = dispatcher.runBlockingTest {
        val mediaDao = PermissionMediaDao()
        mediaDao.hasPermissions = false

        usingScope {
            val repository = MediaRepositoryImpl(it, mediaDao, DummyPlaylistDao, DummyUsageDao, dispatchers)
            assertThrows<PermissionDeniedException> {
                repository.getAllTracks()
            }

            mediaDao.hasPermissions = true
            val allTracks = repository.getAllTracks()
            assertThat(allTracks).isEmpty()
        }
    }

    @Test
    fun whenTrackListChangedForTheFirstTime_thenDontDispatchChangeNotifications() =
        dispatcher.runBlockingTest {
        val mediaDao = TestTrackDao(initialTrackList = listOf(SAMPLE_TRACKS[0], SAMPLE_TRACKS[1]))

        usingScope {
            val repository = MediaRepositoryImpl(it, mediaDao, DummyPlaylistDao, DummyUsageDao, dispatchers)
            val subscriber = repository.changeNotifications.test()

            repository.getAllTracks()
            subscriber.assertNoValues()
        }
    }

    @Test
    fun whenReceivingTrackListUpdate_thenNotifyForAllTracks() {
        val initial = listOf(SAMPLE_TRACKS[0], SAMPLE_TRACKS[1])
        val updated = listOf(SAMPLE_TRACKS[1], SAMPLE_TRACKS[2])

        assertNotifyAfterTrackListUpdate(initial, updated, arrayOf(ChangeNotification.AllTracks))
    }

    @Test
    fun whenReceivingTrackListUpdate_thenNotifyAlbumsOfEachModifiedTrack() {
        val initial = listOf(SAMPLE_TRACKS[0], SAMPLE_TRACKS[1])
        val updated = listOf(SAMPLE_TRACKS[1], SAMPLE_TRACKS[2])

        assertNotifyAfterTrackListUpdate(initial, updated, arrayOf(
            ChangeNotification.Album(65),
            ChangeNotification.Album(102)
        ))
    }

    @Test
    fun whenReceivingTrackListUpdate_thenNotifyArtistOfEachModifiedTrack() {
        val initial = listOf(SAMPLE_TRACKS[0], SAMPLE_TRACKS[1])
        val updated = listOf(SAMPLE_TRACKS[1], SAMPLE_TRACKS[2])

        assertNotifyAfterTrackListUpdate(initial, updated, arrayOf(
            ChangeNotification.Artist(26),
            ChangeNotification.Artist(13)
        ))
    }

    @Test
    fun whenReceivingAlbumListUpdate_thenNotifyForAllAlbums() {
        val initial = listOf(SAMPLE_ALBUMS[0], SAMPLE_ALBUMS[1])
        val updated = listOf(SAMPLE_ALBUMS[1], SAMPLE_ALBUMS[2])

        assertNotifyAfterAlbumListUpdate(initial, updated, arrayOf(ChangeNotification.AllAlbums))
    }

    @Test
    fun whenReceivingAlbumListUpdate_thenNotifyArtistOfEachModifiedAlbum() {
        val initial = listOf(SAMPLE_ALBUMS[0], SAMPLE_ALBUMS[1])
        val updated = listOf(SAMPLE_ALBUMS[1], SAMPLE_ALBUMS[2])

        assertNotifyAfterAlbumListUpdate(initial, updated, arrayOf(
            ChangeNotification.Artist(18),
            ChangeNotification.Artist(13)
        ))
    }

    @Test
    fun whenReceivingUpdatedPlaylists_thenNotifyForAllPlaylists() = dispatcher.runBlockingTest {
        val initial = listOf(SAMPLE_PLAYLISTS[0], SAMPLE_PLAYLISTS[1])
        val updated = listOf(SAMPLE_PLAYLISTS[1], SAMPLE_PLAYLISTS[2])
        val playlistDao = TestPlaylistDao(initialPlaylists = initial)

        usingScope {
            val repository = MediaRepositoryImpl(it, DummyMediaDao, playlistDao, DummyUsageDao, dispatchers)
            repository.getAllPlaylists()
            val subscriber = repository.changeNotifications.test()

            playlistDao.update(updated)

            assertThat(subscriber.values()).contains(ChangeNotification.AllPlaylists)
        }
    }

    @Test
    fun whenReceivingArtistListUpdate_thenNotifyForAllArtists() = dispatcher.runBlockingTest {
        val initial = listOf(SAMPLE_ARTISTS[0], SAMPLE_ARTISTS[1])
        val updated = listOf(SAMPLE_ARTISTS[1], SAMPLE_ARTISTS[2])
        val mediaDao = TestArtistDao(initialArtistList = initial)

        usingScope {
            val repository = MediaRepositoryImpl(it, mediaDao, DummyPlaylistDao, DummyUsageDao, dispatchers)
            repository.getAllArtists()
            val subscriber = repository.changeNotifications.test()

            mediaDao.update(updated)

            assertThat(subscriber.values()).contains(ChangeNotification.AllArtists)
        }
    }

    private fun <M> assertInitialLoadFromDao(
        dao: TestMediaDao<M>,
        expectedMediaList: List<M>,
        getAllMedia: suspend MediaRepository.() -> List<M>
    ): Unit = dispatcher.runBlockingTest {
        usingScope {
            val repository = MediaRepositoryImpl(it, dao, DummyPlaylistDao, DummyUsageDao, dispatchers)
            val mediaList = repository.getAllMedia()

            assertThat(mediaList).containsExactlyElementsIn(expectedMediaList).inOrder()
        }
    }

    private fun <M> assertAlwaysLoadLatestMediaList(
        dao: TestMediaDao<M>,
        expectedInitial: List<M>,
        expectedUpdated: List<M>,
        getAllMedia: suspend MediaRepository.() -> List<M>
    ): Unit = dispatcher.runBlockingTest {
        usingScope {
            val repository = MediaRepositoryImpl(it, dao, DummyPlaylistDao, DummyUsageDao, dispatchers)
            repeat(2) {
                val currentMediaList = repository.getAllMedia()
                assertThat(currentMediaList).containsExactlyElementsIn(expectedInitial)
            }

            dao.update(expectedUpdated)
            repeat(2) {
                val currentMediaList = repository.getAllMedia()
                assertThat(currentMediaList).containsExactlyElementsIn(expectedUpdated).inOrder()
            }
        }
    }

    private fun <M> assertStillLoadsLatestMediaListAfterStreamCompleted(
        dao: TestMediaDao<M>,
        expectedInitial: List<M>,
        getAllMedia: suspend MediaRepository.() -> List<M>
    ): Unit = dispatcher.runBlockingTest {
        usingScope {
            // Fill cache by requesting media for the first time.
            val repository = MediaRepositoryImpl(it, dao, DummyPlaylistDao, DummyUsageDao, dispatchers)
            repository.getAllMedia()

            // When media stream completes
            dao.complete()

            // Then repository returns the latest received media list.
            val cachedMediaList = repository.getAllMedia()
            assertThat(cachedMediaList).containsExactlyElementsIn(expectedInitial).inOrder()
        }
    }

    private fun assertNotifyAfterTrackListUpdate(
        original: List<Track>,
        revised: List<Track>,
        expectedNotifications: Array<out ChangeNotification>
    ): Unit = dispatcher.runBlockingTest {
        // Given a dao with the specified initial tracks...
        val mediaDao = TestTrackDao(initialTrackList = original)

        usingScope {
            // and a repository that started caching tracks...
            val repository = MediaRepositoryImpl(it, mediaDao, DummyPlaylistDao, DummyUsageDao, dispatchers)
            repository.getAllTracks()

            // and we are listening to media change notifications...
            val subscriber = repository.changeNotifications.test()

            // when receiving a new track list
            mediaDao.update(revised)

            // Then receive the expected notifications.
            assertThat(subscriber.values()).containsAtLeastElementsIn(expectedNotifications)
        }
    }

    private fun assertNotifyAfterAlbumListUpdate(
        original: List<Album>,
        revised: List<Album>,
        expectedNotifications: Array<out ChangeNotification>
    ): Unit = dispatcher.runBlockingTest {
        // Given a dao with the specified initial albums...
        val mediaDao = TestAlbumDao(initialAlbumList = original)

        usingScope {
            // and a repository that started caching albums...
            val repository = MediaRepositoryImpl(it, mediaDao, DummyPlaylistDao, DummyUsageDao, dispatchers)
            repository.getAllAlbums()

            // and we are listening to media change notifications...
            val subscriber = repository.changeNotifications.test()

            // when receiving a new album list
            mediaDao.update(revised)

            // Then receive the expected notifications.
            assertThat(subscriber.values()).containsAtLeastElementsIn(expectedNotifications)
        }
    }

    private object DummyPlaylistDao : PlaylistDao {
        override val playlists: Flowable<List<Playlist>> get() = Flowable.empty()
        override suspend fun getPlaylistTracks(playlistId: Long): List<PlaylistTrack> = emptyList()
        override suspend fun getPlaylistsHavingTracks(trackIds: LongArray): LongArray = LongArray(0)
        override suspend fun savePlaylist(playlist: Playlist): Long = 0L
        override suspend fun addTracks(tracks: List<PlaylistTrack>) = Unit
        override suspend fun deletePlaylist(playlistId: Long) = Unit
        override suspend fun deletePlaylistTracks(trackIds: LongArray) = Unit
    }

    private object DummyMediaDao : MediaDao {
        override val tracks: Flowable<List<Track>> get() = Flowable.empty()
        override val albums: Flowable<List<Album>> get() = Flowable.empty()
        override val artists: Flowable<List<Artist>> get() = Flowable.empty()
        override fun deleteTracks(trackIds: LongArray): Completable = Completable.complete()
    }

    private object DummyUsageDao : MediaUsageDao {
        override suspend fun recordEvent(usageEvent: MediaUsageEvent) = Unit
        override suspend fun getMostRatedTracks(limit: Int): List<TrackScore> = emptyList()
        override suspend fun getTracksUsage(): List<TrackUsage> = emptyList()
        override suspend fun deleteEventsForTracks(trackIds: LongArray) = Unit
    }

    /**
     * A [MediaDao] that returns different results when permission is granted or denied.
     * When permission is granted, all flows emit an empty list an never completes.
     * When permission is denied, all flows emit a [PermissionDeniedException].
     */
    private class PermissionMediaDao : MediaDao {
        /**
         * Whether permission should be granted.
         */
        var hasPermissions = false

        override val tracks: Flowable<List<Track>>
            get() = if (hasPermissions) mediaUpdates() else permissionFailure()

        override val albums: Flowable<List<Album>>
            get() = if (hasPermissions) mediaUpdates() else permissionFailure()

        override val artists: Flowable<List<Artist>>
            get() = if (hasPermissions) mediaUpdates() else permissionFailure()

        override fun deleteTracks(trackIds: LongArray): Completable = stub()

        private fun <T> mediaUpdates(): Flowable<List<T>> =
            Flowable.concat(Flowable.just(emptyList()), Flowable.never())

        private fun <T> permissionFailure() =
            Flowable.error<T>(PermissionDeniedException("android.permission"))
    }
}