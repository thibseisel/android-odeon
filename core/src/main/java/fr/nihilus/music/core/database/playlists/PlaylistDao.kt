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

package fr.nihilus.music.core.database.playlists

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Define interactions with the database storing user-defined playlists.
 */
@Dao
abstract class PlaylistDao {

    /**
     * An asynchronous stream of playlists currently stored in the database.
     * A new list is emitted whenever playlists are added, modified or deleted.
     */
    @get:Query("SELECT * FROM playlist ORDER BY date_created ASC")
    abstract val playlists: Flow<List<Playlist>>

    /**
     * Observe tracks that are part of the specified playlist.
     * A new list is emitted whenever tracks are added, moved or removed from that playlist.
     *
     * @param playlistId The [unique identifier][Playlist.id] of the requested playlist.
     */
    @Query("SELECT * FROM playlist_track WHERE playlist_id = :playlistId ORDER BY position ASC")
    abstract fun getPlaylistTracks(playlistId: Long): Flow<List<PlaylistTrack>>

    /**
     * Insert a new playlist record into the database.
     *
     * @param playlist The playlist to persisted to the database.
     * @return The identifier of the newly created playlist assigned by the database.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun savePlaylist(playlist: Playlist): Long

    /**
     * Append tracks to existing playlists.
     * The target playlist is defined by the [PlaylistTrack.playlistId] property.
     *
     * Tracks that are already part of a playlist are ignored.
     * If the given list of tracks is empty, then nothing happens.
     *
     * @param tracks List of tracks to be added to one or multiple playlists.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addTracks(tracks: List<PlaylistTrack>)

    /**
     * Create a new playlist having the specified tracks.
     * Tracks are added to the playlist in the order they are specified.
     *
     * @param newPlaylist The new playlist to be persisted to the database.
     * @param trackIds Unique identifiers of tracks to be added to the new playlist.
     * Passing an empty set of ids will result in creating a playlist with no tracks.
     */
    @Transaction
    open suspend fun createPlaylist(newPlaylist: Playlist, trackIds: LongArray) {
        val playlistId = savePlaylist(newPlaylist)
        if (trackIds.isNotEmpty()) {
            val tracks = trackIds.map {
                PlaylistTrack(playlistId, it)
            }
            addTracks(tracks)
        }
    }

    /**
     * Delete a playlist from the database given its [Playlist.id].
     * Tracks that were part of it will no longer be associated to that playlist.
     * If no such playlist exists then nothing happens.
     *
     * @param playlistId Unique identifier of the playlist to be deleted.
     */
    @Query("DELETE FROM playlist WHERE id = :playlistId")
    abstract suspend fun deletePlaylist(playlistId: Long)

    /**
     * Remove multiple tracks from all existing playlists.
     * Note: this should not be used if the intention is to remove one track from a specific playlist.
     *
     * @param trackIds Unique identifiers of tracks to be removed from playlists.
     * Nothing happens if there are no ids or if one id is invalid.
     */
    @Query("DELETE FROM playlist_track WHERE music_id IN (:trackIds)")
    abstract suspend fun deletePlaylistTracks(trackIds: LongArray)
}
