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

package fr.nihilus.music.media.playlists

import androidx.annotation.WorkerThread
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
internal interface PlaylistDao {

    @get:Query("SELECT * FROM playlist ORDER BY date_created ASC")
    val playlistsFlow: Flowable<List<Playlist>>

    @Deprecated("New APIs should observe playlist updates as a Flowable")
    @Query("SELECT * FROM playlist ORDER BY date_created ASC")
    fun getPlaylists(): Single<List<Playlist>>

    @Query("SELECT * FROM playlist_track WHERE playlist_id = :playlistId ORDER BY position ASC")
    fun getPlaylistTracks(playlistId: Long): Single<List<PlaylistTrack>>

    @Query("SELECT playlist_id FROM playlist_track WHERE music_id IN (:trackIds)")
    fun getPlaylistsHavingTracks(trackIds: LongArray): Single<LongArray>

    @WorkerThread
    @Insert(onConflict = OnConflictStrategy.FAIL)
    fun savePlaylist(playlist: Playlist): Long

    @WorkerThread
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addTracks(tracks: Iterable<PlaylistTrack>)

    @WorkerThread
    @Query("DELETE FROM playlist WHERE id = :playlistId")
    fun deletePlaylist(playlistId: Long)

    @WorkerThread
    @Query("DELETE FROM playlist_track WHERE music_id IN (:trackIds)")
    fun deletePlaylistTracks(trackIds: LongArray)
}
