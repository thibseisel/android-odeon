package fr.nihilus.music.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.net.Uri;
import android.support.annotation.Nullable;

import java.util.Date;

/**
 * A class that groups informations associated with a playlist.
 * Each playlist has an unique identifier that its {@link PlaylistTrack} children must reference
 * to be included.
 */
@Entity(tableName = "playlist",
        indices = {@Index(value = "title", unique = true)})
public class Playlist {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private Long id;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "date_created")
    private Date created;

    @ColumnInfo(name = "date_last_played")
    private Date lastPlayed;

    @ColumnInfo(name = "art_uri")
    private Uri artUri;

    public static Playlist create(CharSequence title) {
        Playlist newPlaylist = new Playlist();
        newPlaylist.setTitle(title.toString());
        newPlaylist.setCreated(new Date());
        return newPlaylist;
    }

    /**
     * Returns the unique identifier of this playlist.
     * If this playlist has not been saved, this identifier will be {@code null}.
     * @return unique identifier or {@code null}
     */
    @Nullable
    public Long getId() {
        return id;
    }

    void setId(Long id) {
        this.id = id;
    }

    /**
     * @return The title given by the user to this playlist
     */
    public String getTitle() {
        return title;
    }

    /**
     * Rename this playlist.
     * @param title The new title of this playlist
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the date at which this playlist has been created
     */
    public Date getCreated() {
        return created;
    }

    void setCreated(Date created) {
        this.created = created;
    }

    /**
     * @return the date at which this playlist has been last played
     */
    public Date getLastPlayed() {
        return lastPlayed;
    }

    /**
     * Update the date a which this playlist has been played for the last time.
     * @param lastPlayed date of last play
     */
    public void setLastPlayed(Date lastPlayed) {
        this.lastPlayed = lastPlayed;
    }

    /**
     * @return an URI pointing to a Bitmap featuring this playlist
     */
    public Uri getArtUri() {
        return artUri;
    }

    /**
     * Sets an URI pointing to a Bitmap that features this playlist.
     * @param artUri URI pointing to a Bitmap resource
     */
    public void setArtUri(Uri artUri) {
        this.artUri = artUri;
    }
}
