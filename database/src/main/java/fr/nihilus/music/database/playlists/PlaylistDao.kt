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

package fr.nihilus.music.database.playlists

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Flowable

@Dao
interface PlaylistDao {

    @get:Query("SELECT * FROM playlist ORDER BY date_created ASC")
    val playlists: Flowable<List<Playlist>>

    @Query("SELECT * FROM playlist_track WHERE playlist_id = :playlistId ORDER BY position ASC")
    suspend fun getPlaylistTracks(playlistId: Long): List<PlaylistTrack>

    @Query("SELECT playlist_id FROM playlist_track WHERE music_id IN (:trackIds)")
    suspend fun getPlaylistsHavingTracks(trackIds: LongArray): LongArray

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun savePlaylist(playlist: Playlist): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTracks(tracks: List<PlaylistTrack>)

    @Query("DELETE FROM playlist WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM playlist_track WHERE music_id IN (:trackIds)")
    suspend fun deletePlaylistTracks(trackIds: LongArray)
}
