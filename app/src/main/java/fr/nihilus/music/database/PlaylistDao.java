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

package fr.nihilus.music.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.support.annotation.WorkerThread;

import java.util.List;

import io.reactivex.Flowable;

@Dao
public interface PlaylistDao {

    @Query("SELECT * FROM playlist ORDER BY date_last_played DESC")
    Flowable<List<Playlist>> getPlaylists();

    @Query("SELECT * FROM playlist_track WHERE playlist_id = :id ORDER BY position ASC")
    Flowable<List<PlaylistTrack>> getPlaylistTracks(long id);

    @WorkerThread
    @Insert(onConflict = OnConflictStrategy.FAIL)
    Long savePlaylist(Playlist playlist);

    @WorkerThread
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void addTracks(Iterable<PlaylistTrack> tracks);

    @WorkerThread
    @Query("DELETE FROM playlist WHERE id = :playlistId")
    void deletePlaylist(long playlistId);

    @WorkerThread
    @Delete
    void deleteTracks(PlaylistTrack... tracks);
}
