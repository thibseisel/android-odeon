/*
 * Copyright 2017 Thibault Seisel
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

package fr.nihilus.music.database

import android.arch.persistence.room.*
import android.support.annotation.WorkerThread
import io.reactivex.Single

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlist ORDER BY date_last_played DESC")
    fun getPlaylists(): Single<List<Playlist>>

    @Query("SELECT * FROM playlist_track WHERE playlist_id = :id ORDER BY position ASC")
    fun getPlaylistTracks(id: Long): Single<List<PlaylistTrack>>

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
    @Delete
    fun deleteTracks(vararg tracks: PlaylistTrack)
}
