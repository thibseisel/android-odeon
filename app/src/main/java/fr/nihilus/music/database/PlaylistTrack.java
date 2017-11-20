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

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;

/**
 * An association between a music track and a {@link Playlist}.
 * If the containing playlist is deleted so is this playlist track.
 * <p>
 * This object can be associated only to one playlist.
 * To feature the same track in multiple playlists you have to define multiple instances.
 */
@Entity(tableName = "playlist_track", primaryKeys = {"music_id", "playlist_id"})
@ForeignKey(entity = Playlist.class, onDelete = ForeignKey.CASCADE,
        childColumns = "playlist_id", parentColumns = "id")
public class PlaylistTrack {

    @ColumnInfo(name = "music_id")
    final private long musicId;

    @ColumnInfo(name = "playlist_id")
    private final long playlistId;

    @ColumnInfo(name = "position")
    private int position;

    /**
     * Create an association between a music track and a playlist.
     * Note that this class performs no check on the music id and the playlist id.
     * @param playlistId id of the playlist this track belongs to
     * @param musicId id of the music track this object represents
     */
    public PlaylistTrack(long playlistId, long musicId) {
        this.playlistId = playlistId;
        this.musicId = musicId;
    }

    /**
     * @return id of the music track this object represents
     */
    public long getMusicId() {
        return musicId;
    }

    /**
     * @return position in the playlist used for ordering
     */
    public int getPosition() {
        return position;
    }

    /**
     * Reorders this track in its containing playlist.
     * @param position New position in the playlist
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * @return id of the playlist this track belongs to
     */
    public long getPlaylistId() {
        return playlistId;
    }
}
