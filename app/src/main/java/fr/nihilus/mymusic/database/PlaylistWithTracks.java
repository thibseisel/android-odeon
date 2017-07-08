package fr.nihilus.mymusic.database;

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
