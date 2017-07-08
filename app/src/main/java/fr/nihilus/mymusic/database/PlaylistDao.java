package fr.nihilus.mymusic.database;

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
    public Flowable<List<Playlist>> getPlaylists();

    @Query("SELECT * FROM playlist ORDER BY date_last_played DESC")
    public Flowable<List<PlaylistWithTracks>> getPlaylistsWithTracks();

    @WorkerThread
    @Insert(onConflict = OnConflictStrategy.FAIL)
    public Long savePlaylist(Playlist playlist);

    @WorkerThread
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public void addTracks(Iterable<PlaylistTrack> tracks);

    @WorkerThread
    @Delete
    public void deletePlaylists(Playlist... playlists);

    @WorkerThread
    @Delete
    public void deleteTracks(PlaylistTrack... tracks);
}
