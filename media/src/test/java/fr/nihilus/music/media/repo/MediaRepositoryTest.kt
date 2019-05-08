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
import fr.nihilus.music.media.playlists.*
import fr.nihilus.music.media.provider.*
import fr.nihilus.music.media.usage.*
import fr.nihilus.music.media.usingScope
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.debug.junit4.CoroutinesTimeout
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Rule
import org.junit.Test

class MediaRepositoryTest {

    private object DummyPlaylistDao : PlaylistDao {
        override val playlistsFlow: Flowable<List<Playlist>> get() = Flowable.empty()
        override fun getPlaylists(): Single<List<Playlist>> = Single.just(emptyList())
        override fun getPlaylistTracks(playlistId: Long): Single<List<PlaylistTrack>> = Single.just(emptyList())
        override fun getPlaylistsHavingTracks(trackIds: LongArray): Single<LongArray> = Single.just(LongArray(0))
        override fun savePlaylist(playlist: Playlist): Long = 0L
        override fun addTracks(tracks: List<PlaylistTrack>) = Unit
        override fun deletePlaylist(playlistId: Long) = Unit
        override fun deletePlaylistTracks(trackIds: LongArray) = Unit
    }

    private object DummyMediaDao : RxMediaDao {
        override val tracks: Flowable<List<Track>> get() = Flowable.empty()
        override val albums: Flowable<List<Album>> get() = Flowable.empty()
        override val artists: Flowable<List<Artist>> get() = Flowable.empty()
        override fun deleteTracks(trackIds: LongArray): Completable = Completable.complete()
    }

    private object DummyUsageDao : MediaUsageDao {
        override fun getMostRatedTracks(limit: Int): List<TrackScore> = emptyList()
        override fun recordUsageEvents(events: Iterable<MediaUsageEvent>) = Unit
        override fun deleteAllEventsBefore(timeThreshold: Long) = Unit
        override fun deleteEventsForTracks(trackIds: LongArray) = Unit
    }

    @[JvmField Rule]
    val timeoutRule = CoroutinesTimeout.seconds(5)

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
    fun whenLoadingAllAlbums_thenReturnAlbumsFromDao() : Unit = runBlocking {
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
    fun whenLoadingAllPlaylists_thenReturnPlaylistsFromDao() : Unit = runBlocking {
        val dao = TestPlaylistDao(
            SAMPLE_PLAYLISTS,
            SAMPLE_PLAYLIST_TRACKS
        )
        
        usingScope {
            val repository = MediaRepositoryImpl(it, DummyMediaDao, dao, DummyUsageDao)
            val playlists = repository.getAllPlaylists()
            assertThat(playlists).containsExactlyElementsIn(SAMPLE_PLAYLISTS).inOrder()
        }
    }

    @Test
    fun whenLoadingAllPlaylists_thenReturnLatestPlaylists(): Unit = runBlocking {
        val original = listOf(SAMPLE_PLAYLISTS[0])
        val updated = listOf(SAMPLE_PLAYLISTS[1])
        val dao = TestPlaylistDao(original, emptyList())

        usingScope {
            val repository = MediaRepositoryImpl(it, DummyMediaDao, dao, DummyUsageDao)
            repeat(2) {
                val currentPlaylists = repository.getAllPlaylists()
                assertThat(currentPlaylists).containsExactlyElementsIn(original).inOrder()
            }

            dao.update(updated)
            yield()

            repeat(2) {
                val currentPlaylists = repository.getAllPlaylists()
                assertThat(currentPlaylists).containsExactlyElementsIn(updated).inOrder()
            }
        }
    }

    @Test
    fun givenNonExistingPlaylist_whenLoadingItsTracks_thenReturnNull(): Unit = runBlocking {
        val mediaDao = TestTrackDao(SAMPLE_TRACKS)
        val playlistDao = TestPlaylistDao(
            SAMPLE_PLAYLISTS,
            SAMPLE_PLAYLIST_TRACKS
        )

        usingScope {
            val repository = MediaRepositoryImpl(it, mediaDao, playlistDao, DummyUsageDao)
            val playlistTracks = repository.getPlaylistTracks(42L)

            assertThat(playlistTracks).named("Tracks of unknown playlist 42").isNull()
        }
    }

    @Test
    fun givenAnExistingPlaylist_whenLoadingItsTracks_thenReturnTracksFromThatPlaylist(): Unit = runBlocking {
        val mediaDao = TestTrackDao(SAMPLE_TRACKS)
        val playlistDao = TestPlaylistDao(
            SAMPLE_PLAYLISTS,
            SAMPLE_PLAYLIST_TRACKS
        )

        usingScope {
            val repository = MediaRepositoryImpl(it, mediaDao, playlistDao, DummyUsageDao)
            val playlistTracks = repository.getPlaylistTracks(1L)

            assertThat(playlistTracks).containsExactly(SAMPLE_TRACKS[1])
        }
    }

    @Test
    fun whenLoadingMostRatedTracks_thenReturnTracksByDescendingScore(): Unit = runBlocking {
        val mediaDao = TestTrackDao(SAMPLE_TRACKS)
        val usageDao = TestUsageDao(SAMPLE_TRACK_SCORE)

        usingScope {
            val repository = MediaRepositoryImpl(it, mediaDao, DummyPlaylistDao, usageDao)
            val mostRatedTracks = repository.getMostRatedTracks()

            assertThat(mostRatedTracks).containsExactly(
                SAMPLE_TRACKS[7],
                SAMPLE_TRACKS[8],
                SAMPLE_TRACKS[5],
                SAMPLE_TRACKS[9],
                SAMPLE_TRACKS[3],
                SAMPLE_TRACKS[2]
            )
        }
    }

    @Test
    fun whenTrackListChangedForTheFirstTime_thenDontDispatchChangeNotifications(): Unit = runBlocking {
        val mediaDao = TestTrackDao(initialTrackList = listOf(SAMPLE_TRACKS[0], SAMPLE_TRACKS[1]))

        usingScope {
            val repository = MediaRepositoryImpl(it, mediaDao, DummyPlaylistDao, DummyUsageDao)
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
    fun whenReceivingUpdatedPlaylists_thenNotifyForAllPlaylists() : Unit = runBlocking {
        val initial = listOf(SAMPLE_PLAYLISTS[0], SAMPLE_PLAYLISTS[1])
        val updated = listOf(SAMPLE_PLAYLISTS[1], SAMPLE_PLAYLISTS[2])
        val playlistDao = TestPlaylistDao(initialPlaylists = initial)

        usingScope {
            val repository = MediaRepositoryImpl(it, DummyMediaDao, playlistDao, DummyUsageDao)
            repository.getAllPlaylists()
            val subscriber = repository.changeNotifications.test()

            playlistDao.update(updated)
            yield()

            assertThat(subscriber.values()).contains(ChangeNotification.AllPlaylists)
        }
    }

    @Test
    fun whenReceivingArtistListUpdate_thenNotifyForAllArtists(): Unit = runBlocking {
        val initial = listOf(SAMPLE_ARTISTS[0], SAMPLE_ARTISTS[1])
        val updated = listOf(SAMPLE_ARTISTS[1], SAMPLE_ARTISTS[2])
        val mediaDao = TestArtistDao(initialArtistList = initial)

        usingScope {
            val repository = MediaRepositoryImpl(it, mediaDao, DummyPlaylistDao, DummyUsageDao)
            repository.getAllArtists()
            val subscriber = repository.changeNotifications.test()

            mediaDao.update(updated)
            yield()

            assertThat(subscriber.values()).contains(ChangeNotification.AllArtists)
        }
    }

    private fun <M> assertInitialLoadFromDao(
        dao: TestMediaDao<M>,
        expectedMediaList: List<M>,
        getAllMedia: suspend MediaRepository.() -> List<M>
    ): Unit = runBlocking {
        usingScope {
            val repository = MediaRepositoryImpl(it, dao, DummyPlaylistDao, DummyUsageDao)
            val mediaList = repository.getAllMedia()

            assertThat(mediaList).containsExactlyElementsIn(expectedMediaList).inOrder()
        }
    }

    private fun <M> assertAlwaysLoadLatestMediaList(
        dao: TestMediaDao<M>,
        expectedInitial: List<M>,
        expectedUpdated: List<M>,
        getAllMedia: suspend MediaRepository.() -> List<M>
    ): Unit = runBlocking {
        usingScope {
            val repository = MediaRepositoryImpl(it, dao, DummyPlaylistDao, DummyUsageDao)
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
    ): Unit = runBlocking {
        usingScope {
            // Fill cache by requesting media for the first time.
            val repository = MediaRepositoryImpl(it, dao, DummyPlaylistDao, DummyUsageDao)
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
    ) : Unit = runBlocking {
        // Given a dao with the specified initial tracks...
        val mediaDao = TestTrackDao(initialTrackList = original)

        usingScope {
            // and a repository that started caching tracks...
            val repository = MediaRepositoryImpl(it, mediaDao, DummyPlaylistDao, DummyUsageDao)
            repository.getAllTracks()

            // and we are listening to media change notifications...
            val subscriber = repository.changeNotifications.test()

            // when receiving a new track list
            mediaDao.update(revised)
            yield()

            // Then receive the expected notifications.
            assertThat(subscriber.values()).containsAtLeastElementsIn(expectedNotifications)
        }
    }

    private fun assertNotifyAfterAlbumListUpdate(
        original: List<Album>,
        revised: List<Album>,
        expectedNotifications: Array<out ChangeNotification>
    ) : Unit = runBlocking {
        // Given a dao with the specified initial albums...
        val mediaDao = TestAlbumDao(initialAlbumList = original)

        usingScope {
            // and a repository that started caching albums...
            val repository = MediaRepositoryImpl(it, mediaDao, DummyPlaylistDao, DummyUsageDao)
            repository.getAllAlbums()

            // and we are listening to media change notifications...
            val subscriber = repository.changeNotifications.test()

            // when receiving a new album list
            mediaDao.update(revised)
            yield()

            // Then receive the expected notifications.
            assertThat(subscriber.values()).containsAtLeastElementsIn(expectedNotifications)
        }
    }
}