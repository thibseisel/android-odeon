/*
 * Copyright 2020 Thibault Seisel
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

package fr.nihilus.music.core.database.playlists

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.core.net.toUri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import fr.nihilus.music.core.database.AppDatabase
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Validate behavior of the real implementation of [PlaylistDao].
 * This indirectly checks:
 * 1. that the database schema for playlists is adequate,
 * 2. the correctness of the SQL statements specified in [PlaylistDao].
 *
 * Tests are performed on an in-memory version of the SQLite database
 * and its content is reset between tests.
 */
@MediumTest
internal class PlaylistDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: PlaylistDao

    @BeforeTest
    fun setupInMemoryDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.playlistDao
    }

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun whenCreatingPlaylistWithoutTracks_thenPersistIt() = runBlocking<Unit> {
        dao.createPlaylist(UNSAVED_PLAYLIST, LongArray(0))
        assertPlaylistPersistedFrom(UNSAVED_PLAYLIST)
    }

    @Test
    fun whenCreatingPlaylistWithTracks_thenGiveThemSamePlaylistId() = runBlocking {
        dao.createPlaylist(UNSAVED_PLAYLIST, longArrayOf(16L, 42L, 39L, 10L))
        val playlistId = assertPlaylistPersistedFrom(UNSAVED_PLAYLIST)

        // Verify that playlist tracks are mapped to the newly inserted playlist.
        database.query("SELECT playlist_id, music_id FROM playlist_track", null).use {
            assertEquals(4, it.count)

            while (it.moveToNext()) {
                assertEquals(
                    expected = playlistId,
                    actual = it.getLong(0),
                    message = "Expected track ${it.getLong(1)} to be bound to playlist $playlistId, but was ${
                        it.getLong(
                            0
                        )
                    }"
                )
            }
        }
    }

    @Test
    fun whenAddingExistingTrackToPlaylist_thenReplaceIt() = runBlocking {
        val existingTrack = PlaylistTrack(PERSISTED_PLAYLIST.id, 42L)
        dao.createPlaylist(PERSISTED_PLAYLIST, longArrayOf(existingTrack.trackId))

        dao.addTracks(listOf(existingTrack))

        database.query("SELECT playlist_id, music_id FROM playlist_track", null).use {
            assertEquals(1, it.count)

            it.moveToFirst()
            assertEquals(PERSISTED_PLAYLIST.id, it.getLong(0))
            assertEquals(42L, it.getLong(1))
        }
    }

    @Test
    fun whenAddingTracksToUnknownPlaylist_thenFailWithSqlConstraint() = runBlocking<Unit> {
        val orphanTrack = PlaylistTrack(12L, 42L)

        assertFailsWith<SQLiteConstraintException> {
            dao.addTracks(listOf(orphanTrack))
        }
    }

    @Test
    fun whenDeletingPlaylist_thenAlsoDeleteItsTracks() = runBlocking {
        dao.createPlaylist(PERSISTED_PLAYLIST, longArrayOf(16L, 42L))

        dao.deletePlaylist(PERSISTED_PLAYLIST.id)

        database.query("SELECT * FROM playlist_track WHERE playlist_id = 39", null).use {
            assertEquals(0, it.count)
        }
    }

    private fun assertPlaylistPersistedFrom(newPlaylist: Playlist): Long {
        database.query("SELECT id, title, date_created, icon_uri FROM playlist", null).use {
            assertTrue(it.moveToFirst())
            assertEquals(newPlaylist.title, it.getString(1))
            assertEquals(newPlaylist.created, it.getLong(2))
            assertEquals(newPlaylist.iconUri.toString(), it.getString(3))

            val playlistId = it.getLong(0)
            assertNotEquals(0L, playlistId)

            return playlistId
        }
    }
}

private val UNSAVED_PLAYLIST =
    Playlist(0L, "Zen", 1585836890L, "content://fr.nihilus.music.provider/icons/zen.png".toUri())
private val PERSISTED_PLAYLIST = Playlist(
    39L,
    "Rock'n'Roll",
    1585903650L,
    "content://fr.nihilus.music.provider/icons/rocknroll.png".toUri()
)
