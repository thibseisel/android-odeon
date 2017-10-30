package fr.nihilus.music.database

import android.arch.persistence.room.*
import android.support.annotation.WorkerThread
import io.reactivex.Flowable

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlist ORDER BY date_last_played DESC")
    fun playlists(): Flowable<List<Playlist>>

    @Query("SELECT * FROM playlist ORDER BY date_last_played DESC")
    fun playlistsWithTracks(): Flowable<List<PlaylistWithTracks>>

    @Query("SELECT * FROM playlist_track WHERE playlist_id = :id ORDER BY position ASC")
    fun getPlaylistTracks(id: Long): Flowable<List<PlaylistTrack>>

    @WorkerThread
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun savePlaylist(playlist: Playlist): Long?

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
