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

import android.Manifest
import fr.nihilus.music.common.os.PermissionDeniedException
import fr.nihilus.music.common.test.stub
import fr.nihilus.music.database.playlists.Playlist
import fr.nihilus.music.database.playlists.PlaylistDao
import fr.nihilus.music.database.playlists.PlaylistTrack
import fr.nihilus.music.media.playlists.*
import fr.nihilus.music.media.provider.*
import fr.nihilus.music.database.usage.MediaUsageEvent
import fr.nihilus.music.database.usage.TrackScore
import fr.nihilus.music.database.usage.TrackUsage
import fr.nihilus.music.database.usage.UsageDao
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldContain
import io.kotlintest.matchers.collections.shouldContainAll
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotThrowAny
import io.kotlintest.shouldThrow
import io.reactivex.Flowable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Test

/**
 * Tests that describe and validate the behavior of
 * [MediaRepository][fr.nihilus.music.media.repo.MediaRepository] implementations
 * and especially [MediaRepositoryImpl].
 */
class MediaRepositoryTest {

    private val dispatcher = TestCoroutineDispatcher()

    @After
    fun cleanup() {
        dispatcher.cleanupTestCoroutines()
    }

    /**
     * Convenience function to execute the test [body] with [runBlockingTest].
     */
    private fun test(body: suspend TestCoroutineScope.() -> Unit) = dispatcher.runBlockingTest(body)

    /**
     * Execute the given [block] in the context of a child coroutine scope, then cancel that scope.
     * This simulates the whole lifespan of a coroutine scope, cancelling it when [block] ends.
     */
    private fun TestCoroutineScope.runInScope(block: suspend CoroutineScope.() -> Unit) {
        val job = launch(block = block)
        advanceUntilIdle()
        job.cancel()
    }

    @Test
    fun `When requesting all tracks, then load tracks from Dao`() = test {
        val sampleTracks = TestTrackDao(SAMPLE_TRACKS)

        runInScope {
            val repository = MediaRepository(this, mediaDao = sampleTracks)
            val tracks = repository.getTracks()

            tracks shouldContainExactly SAMPLE_TRACKS
        }
    }

    @Test
    fun `When requesting tracks, then always return the latest track list`() = test {
        val initialTracks = listOf(SAMPLE_TRACKS[0])
        val updatedTracks = listOf(SAMPLE_TRACKS[1])
        val trackDao = TestTrackDao(initialTracks)

        runInScope {
            val repository = MediaRepository(this, mediaDao = trackDao)

            val initialLoad = repository.getTracks()
            initialLoad shouldBe initialTracks
            val secondLoad = repository.getTracks()
            secondLoad shouldBe initialTracks

            trackDao.update(updatedTracks)

            val loadAfterAfter = repository.getTracks()
            loadAfterAfter shouldBe updatedTracks
            val secondLoadAfterUpdate = repository.getTracks()
            secondLoadAfterUpdate shouldBe updatedTracks
        }
    }

    @Test
    fun `Given denied permission, when requesting tracks then throw PermissionDeniedException`() = test {
        val failingTrackDao = TestTrackDao()
        failingTrackDao.failWith(PermissionDeniedException(Manifest.permission.READ_EXTERNAL_STORAGE))

        runInScope {
            val repository = MediaRepository(this, mediaDao = failingTrackDao)
            val permissionException = shouldThrow<PermissionDeniedException> {
                repository.getTracks()
            }

            permissionException.permission shouldBe Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    @Test
    fun `Given denied permission, when requesting tracks after being granted then load the current track list`() = test {
        val grantingPermissionDao = PermissionMediaDao()
        grantingPermissionDao.hasPermissions = false

        runInScope {
            val repository = MediaRepository(this, mediaDao = grantingPermissionDao)
            runCatching { repository.getTracks() }

            grantingPermissionDao.hasPermissions = true
            shouldNotThrowAny {
                repository.getTracks()
            }
        }
    }

    @Test
    fun `When requesting albums, then load albums from Dao`() = test {
        val albumDao = TestAlbumDao(SAMPLE_ALBUMS)

        runInScope {
            val repository = MediaRepository(this, mediaDao = albumDao)
            val albums = repository.getAlbums()

            albums shouldContainExactly SAMPLE_ALBUMS
        }
    }

    @Test
    fun `When requesting albums, then always return the latest album list`() = test {
        val initialAlbums = listOf(SAMPLE_ALBUMS[0])
        val updatedAlbums = listOf(SAMPLE_ALBUMS[1])
        val albumDao = TestAlbumDao(initialAlbums)

        runInScope {
            val repository = MediaRepository(this, mediaDao = albumDao)

            val initialLoad = repository.getAlbums()
            initialLoad shouldBe initialAlbums
            val secondLoad = repository.getAlbums()
            secondLoad shouldBe initialAlbums

            albumDao.update(updatedAlbums)

            val loadAfterAfter = repository.getAlbums()
            loadAfterAfter shouldBe updatedAlbums
            val secondLoadAfterUpdate = repository.getAlbums()
            secondLoadAfterUpdate shouldBe updatedAlbums
        }
    }

    @Test
    fun `Given denied permission, when requesting albums then throw PermissionDeniedException`() = test {
        val failingAlbumDao = TestAlbumDao()
        failingAlbumDao.failWith(PermissionDeniedException(Manifest.permission.READ_EXTERNAL_STORAGE))

        runInScope {
            val repository = MediaRepository(this, mediaDao = failingAlbumDao)
            val permissionException = shouldThrow<PermissionDeniedException> {
                repository.getAlbums()
            }

            permissionException.permission shouldBe Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    @Test
    fun `Given denied permission, when requesting albums after being granted then load the current album list`() = test {
        val grantingPermissionDao = PermissionMediaDao()
        grantingPermissionDao.hasPermissions = false

        runInScope {
            val repository = MediaRepository(this, mediaDao = grantingPermissionDao)
            runCatching { repository.getAlbums() }

            grantingPermissionDao.hasPermissions = true
            shouldNotThrowAny {
                repository.getAlbums()
            }
        }
    }

    @Test
    fun `When requesting artists, then load artists from Dao`() = test {
        val artistDao = TestArtistDao(SAMPLE_ARTISTS)

        runInScope {
            val repository = MediaRepository(this, mediaDao = artistDao)
            val artists = repository.getAlbums()

            artists shouldContainExactly SAMPLE_ARTISTS
        }
    }

    @Test
    fun `When requesting artists, then always return the latest artist list`() = test {
        val initialArtists = listOf(SAMPLE_ARTISTS[0])
        val updatedArtists = listOf(SAMPLE_ARTISTS[1])
        val artistDao = TestArtistDao(initialArtists)

        runInScope {
            val repository = MediaRepository(this, mediaDao = artistDao)

            val initialLoad = repository.getArtists()
            initialLoad shouldBe initialArtists
            val secondLoad = repository.getArtists()
            secondLoad shouldBe initialArtists

            artistDao.update(updatedArtists)

            val loadAfterAfter = repository.getArtists()
            loadAfterAfter shouldBe updatedArtists
            val secondLoadAfterUpdate = repository.getArtists()
            secondLoadAfterUpdate shouldBe updatedArtists
        }
    }

    @Test
    fun `Given denied permission, when requesting artists then throw PermissionDeniedException`() = test {
        val failingArtistDao = TestArtistDao()
        failingArtistDao.failWith(PermissionDeniedException(Manifest.permission.READ_EXTERNAL_STORAGE))

        runInScope {
            val repository = MediaRepository(this, mediaDao = failingArtistDao)
            val permissionException = shouldThrow<PermissionDeniedException> {
                repository.getArtists()
            }

            permissionException.permission shouldBe Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    @Test
    fun `Given denied permission, when requesting artists after being granted then load the current artist list`() = test {
        val grantingPermissionDao = PermissionMediaDao()
        grantingPermissionDao.hasPermissions = false

        runInScope {
            val repository = MediaRepository(this, mediaDao = grantingPermissionDao)
            runCatching { repository.getArtists() }

            grantingPermissionDao.hasPermissions = true
            shouldNotThrowAny {
                repository.getArtists()
            }
        }
    }

    @Test
    fun `When requesting playlists, then load playlist from Dao`() = test {
        val dao = TestPlaylistDao(SAMPLE_PLAYLISTS, SAMPLE_PLAYLIST_TRACKS)

        runInScope {
            val repository = MediaRepository(this, playlistDao = dao)
            val playlists = repository.getPlaylists()

            playlists shouldContainExactly SAMPLE_PLAYLISTS
        }
    }

    @Test
    fun `When loading playlists, then always return the latest playlist set`() = test {
        val original = listOf(SAMPLE_PLAYLISTS[0])
        val updated = listOf(SAMPLE_PLAYLISTS[1])
        val dao = TestPlaylistDao(original, emptyList())

        runInScope {
            val repository = MediaRepository(this, playlistDao = dao)

            val initialLoad = repository.getPlaylists()
            initialLoad shouldBe original
            val secondLoad = repository.getPlaylists()
            secondLoad shouldBe original

            dao.update(updated)

            val loadAfterUpdated = repository.getPlaylists()
            loadAfterUpdated shouldBe updated
            val secondLoadAfterUpdated = repository.getPlaylists()
            secondLoadAfterUpdated shouldBe updated
        }
    }

    @Test
    fun `Given the id of a playlist that does not exists, when requesting its tracks then return null`() = test {
        val mediaDao = TestTrackDao(SAMPLE_TRACKS)
        val playlistDao = TestPlaylistDao(SAMPLE_PLAYLISTS, SAMPLE_PLAYLIST_TRACKS)

        runInScope {
            val repository = MediaRepository(this, mediaDao, playlistDao)
            val playlistTracks = repository.getPlaylistTracks(42L)

            playlistTracks.shouldBeNull()
        }
    }

    @Test
    fun `Given an empty existing playlist, when requesting its tracks then return an empty list`() = test {
        val mediaDao = TestTrackDao(SAMPLE_TRACKS)
        val playlistDao = TestPlaylistDao(SAMPLE_PLAYLISTS, emptyList())

        runInScope {
            val repository = MediaRepository(this, mediaDao, playlistDao)
            val playlistTracks = repository.getPlaylistTracks(1L)

            playlistTracks.shouldNotBeNull()
            playlistTracks.shouldBeEmpty()
        }
    }

    @Test
    fun `Given an existing playlist, when requesting its tracks then load them from both Dao`() = test {
        val mediaDao = TestTrackDao(SAMPLE_TRACKS)
        val playlistDao = TestPlaylistDao(SAMPLE_PLAYLISTS, SAMPLE_PLAYLIST_TRACKS)

        runInScope {
            val repository = MediaRepository(this, mediaDao, playlistDao)
            val playlistTracks = repository.getPlaylistTracks(1L)

            playlistTracks.shouldNotBeNull()
            playlistTracks.shouldContainExactly(SAMPLE_TRACKS[1])
        }
    }

    @Test
    fun `When track list changed for the first time, then do not dispatch change notifications`() = test {
        val mediaDao = TestTrackDao(initialTrackList = listOf(SAMPLE_TRACKS[0], SAMPLE_TRACKS[1]))

        runInScope {
            val repository = MediaRepository(this, mediaDao)
            val subscriber = repository.changeNotifications.test()

            repository.getTracks()
            subscriber.assertNoValues()
        }
    }

    @Test
    fun `When receiving a track list update, then notify a change of all tracks`() = test {
        val initial = listOf(SAMPLE_TRACKS[0], SAMPLE_TRACKS[1])
        val updated = listOf(SAMPLE_TRACKS[1], SAMPLE_TRACKS[2])

        // Given a dao with the specified initial tracks...
        val mediaDao = TestTrackDao(initialTrackList = initial)

        runInScope {
            // and a repository that started caching tracks...
            val repository = MediaRepository(this, mediaDao)
            repository.getTracks()

            // and we are listening to media change notifications...
            val subscriber = repository.changeNotifications.test()

            // when receiving a new track list
            mediaDao.update(updated)

            // then receive the expected notifications.
            subscriber.values().shouldContain(ChangeNotification.AllTracks)
        }
    }

    @Test
    fun `When receiving a track list update, then notify for the album of each modified track`() = test {
        val initial = listOf(SAMPLE_TRACKS[0], SAMPLE_TRACKS[1])
        val updated = listOf(SAMPLE_TRACKS[1], SAMPLE_TRACKS[2])

        // Given a dao with the specified initial tracks...
        val mediaDao = TestTrackDao(initialTrackList = initial)

        runInScope {
            // and a repository that started caching tracks...
            val repository = MediaRepository(this, mediaDao)
            repository.getTracks()

            // and we are listening to media change notifications...
            val subscriber = repository.changeNotifications.test()

            // when receiving a new track list
            mediaDao.update(updated)

            // Then receive the expected notifications.
            subscriber.values().shouldContainAll(
                ChangeNotification.Album(65),
                ChangeNotification.Album(102)
            )
        }
    }

    @Test
    fun `When receiving a track list update, then notify for the artist of each modified track`() = test {
        val initial = listOf(SAMPLE_TRACKS[0], SAMPLE_TRACKS[1])
        val updated = listOf(SAMPLE_TRACKS[1], SAMPLE_TRACKS[2])

        // Given a dao with the specified initial tracks...
        val mediaDao = TestTrackDao(initialTrackList = initial)

        runInScope {
            // and a repository that started caching tracks...
            val repository = MediaRepository(this, mediaDao)
            repository.getTracks()

            // and we are listening to media change notifications...
            val subscriber = repository.changeNotifications.test()

            // when receiving a new track list
            mediaDao.update(updated)

            // Then receive the expected notifications.
            subscriber.values().shouldContainAll(
                ChangeNotification.Artist(26),
                ChangeNotification.Artist(13)
            )
        }
    }

    @Test
    fun `When receiving an album list update, then notify a change of all albums`() = test {
        val initial = listOf(SAMPLE_ALBUMS[0], SAMPLE_ALBUMS[1])
        val updated = listOf(SAMPLE_ALBUMS[1], SAMPLE_ALBUMS[2])

        // Given a dao with the specified initial albums...
        val mediaDao = TestAlbumDao(initialAlbumList = initial)

        runInScope {
            // and a repository that started caching albums...
            val repository = MediaRepository(this, mediaDao)
            repository.getAlbums()

            // and we are listening to media change notifications...
            val subscriber = repository.changeNotifications.test()

            // when receiving a new album list
            mediaDao.update(updated)

            // Then receive the expected notifications.
            subscriber.values() shouldContain ChangeNotification.AllAlbums
        }
    }

    @Test
    fun `When receiving an album list update, then notify for the artist of each modified album`() = test {
        val initial = listOf(SAMPLE_ALBUMS[0], SAMPLE_ALBUMS[1])
        val updated = listOf(SAMPLE_ALBUMS[1], SAMPLE_ALBUMS[2])

        // Given a dao with the specified initial albums...
        val mediaDao = TestAlbumDao(initialAlbumList = initial)

        runInScope {
            // and a repository that started caching albums...
            val repository = MediaRepository(this, mediaDao)
            repository.getAlbums()

            // and we are listening to media change notifications...
            val subscriber = repository.changeNotifications.test()

            // when receiving a new album list
            mediaDao.update(updated)

            // Then receive the expected notifications.
            subscriber.values().shouldContainAll(
                ChangeNotification.Artist(18),
                ChangeNotification.Artist(13)
            )
        }
    }

    @Test
    fun `When receiving playlists update, then notify a change of all playlists`() = test {
        val initial = listOf(SAMPLE_PLAYLISTS[0], SAMPLE_PLAYLISTS[1])
        val updated = listOf(SAMPLE_PLAYLISTS[1], SAMPLE_PLAYLISTS[2])
        val playlistDao = TestPlaylistDao(initialPlaylists = initial)

        runInScope {
            val repository = MediaRepository(this, playlistDao = playlistDao)
            repository.getPlaylists()
            val subscriber = repository.changeNotifications.test()

            playlistDao.update(updated)

            subscriber.values() shouldContain ChangeNotification.AllPlaylists
        }
    }

    @Test
    fun `When receiving an artists update, then notify a change of all artists`() = test {
        val initial = listOf(SAMPLE_ARTISTS[0], SAMPLE_ARTISTS[1])
        val updated = listOf(SAMPLE_ARTISTS[1], SAMPLE_ARTISTS[2])
        val mediaDao = TestArtistDao(initialArtistList = initial)

        runInScope {
            val repository = MediaRepository(this, mediaDao)
            repository.getArtists()
            val subscriber = repository.changeNotifications.test()

            mediaDao.update(updated)

            subscriber.values() shouldContain ChangeNotification.AllArtists
        }
    }

    /**
     * Convenience function for creating the [MediaRepository] under test.
     *
     * @param scope The scope in which coroutines run by this repository will be executed.
     * @param mediaDao The source of tracks, artists and albums. Defaults to a dummy implementation.
     * @param playlistDao The source of playlists. Defaults to a dummy implementation.
     * @param usageDao The source of usage statistics. Defaults to a dummy implementation.
     */
    @Suppress("TestFunctionName")
    private fun MediaRepository(
        scope: CoroutineScope,
        mediaDao: MediaDao = DummyMediaDao,
        playlistDao: PlaylistDao = DummyPlaylistDao,
        usageDao: UsageDao = DummyUsageDao
    ) = MediaRepositoryImpl(scope, mediaDao, playlistDao, usageDao)

    private object DummyPlaylistDao : PlaylistDao() {
        override val playlists: Flow<List<Playlist>> get() = emptyFlow()
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
        override suspend fun deleteTracks(trackIds: LongArray) = 0
    }

    private object DummyUsageDao : UsageDao {
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

        override suspend fun deleteTracks(trackIds: LongArray) = stub()

        private fun <T> mediaUpdates(): Flowable<List<T>> =
            Flowable.concat(Flowable.just(emptyList()), Flowable.never())

        private fun <T> permissionFailure() =
            Flowable.error<T>(PermissionDeniedException("android.permission"))
    }
}