package fr.nihilus.mymusic.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "music_info")
public class MusicInfo {

    /**
     * Unique identifier of the song described by those informations.
     */
    // Primary key should not be automatically assigned by the database
    @PrimaryKey
    @ColumnInfo(name = "music_id")
    private long musicId;

    @ColumnInfo(name = "read_count")
    private int readCount;

    @ColumnInfo(name = "skip_count")
    private int skipCount;

    @ColumnInfo(name = "tempo")
    private int tempo;

    @ColumnInfo(name = "mean_energy")
    private int energy;

    public long getMusicId() {
        return musicId;
    }

    public void setMusicId(long musicId) {
        this.musicId = musicId;
    }

    public int getReadCount() {
        return readCount;
    }

    public void setReadCount(int readCount) {
        this.readCount = readCount;
    }

    public int getSkipCount() {
        return skipCount;
    }

    public void setSkipCount(int skipCount) {
        this.skipCount = skipCount;
    }

    public int getTempo() {
        return tempo;
    }

    public void setTempo(int tempo) {
        this.tempo = tempo;
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
    }
}
