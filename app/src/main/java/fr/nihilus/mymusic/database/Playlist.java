package fr.nihilus.mymusic.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.net.Uri;

import java.util.Date;

/**
 *
 */
@Entity(tableName = "playlist",
        indices = {@Index(value = "name", unique = true)})
public class Playlist {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private Long id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "date_created")
    private Date created;

    @ColumnInfo(name = "date_last_played")
    private Date lastPlayed;

    @ColumnInfo(name = "art_uri")
    private Uri artUri;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getLastPlayed() {
        return lastPlayed;
    }

    public void setLastPlayed(Date lastPlayed) {
        this.lastPlayed = lastPlayed;
    }

    public Uri getArtUri() {
        return artUri;
    }

    public void setArtUri(Uri artUri) {
        this.artUri = artUri;
    }
}
