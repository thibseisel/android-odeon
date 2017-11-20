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

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Relation;

import java.util.Collections;
import java.util.List;

/**
 * A user-defined group of music tracks that are intended to be played together.
 * To add tracks to a playlist, see {@link PlaylistTrack}.
 */
public class PlaylistWithTracks {

    @Embedded
    private Playlist playlistInfo;

    @Relation(parentColumn = "id", entityColumn = "playlist_id", entity = PlaylistTrack.class)
    private List<PlaylistTrack> tracks;

    public Playlist getPlaylistInfo() {
        return playlistInfo;
    }

    void setPlaylistInfo(Playlist playlistInfo) {
        this.playlistInfo = playlistInfo;
    }

    public List<PlaylistTrack> getTracks() {
        return tracks == null ? Collections.<PlaylistTrack>emptyList() : tracks;
    }

    void setTracks(List<PlaylistTrack> tracks) {
        this.tracks = tracks;
    }
}
