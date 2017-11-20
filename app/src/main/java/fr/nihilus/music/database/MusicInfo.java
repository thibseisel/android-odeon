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
import android.arch.persistence.room.PrimaryKey;

/**
 * Additional informations associated with a music track that can be used for classification.
 * Each instance is bound to a music track by its id.
 */
@Entity(tableName = "music_info")
public class MusicInfo {
    // Primary key should not be automatically assigned by the database
    @PrimaryKey
    @ColumnInfo(name = "music_id")
    private final long musicId;

    @ColumnInfo(name = "read_count")
    private int readCount;

    @ColumnInfo(name = "skip_count")
    private int skipCount;

    @ColumnInfo(name = "tempo")
    private int tempo;

    @ColumnInfo(name = "mean_energy")
    private int energy;

    /**
     * Create a new set of informations associated with a particular music track.
     * Note that this class performs no check on the passed music id.
     * @param musicId the id of the music track associated with those informations
     */
    public MusicInfo(long musicId) {
        this.musicId = musicId;
    }

    /**
     * @return Unique identifier of the track described by those informations.
     */
    public long getMusicId() {
        return musicId;
    }

    /**
     * @return The number of times the associated track has been played until the end.
     */
    public int getReadCount() {
        return readCount;
    }

    public void setReadCount(int readCount) {
        this.readCount = readCount;
    }

    /**
     * @return The number of times the user skipped this track before it has been played for 5 seconds.
     */
    public int getSkipCount() {
        return skipCount;
    }

    public void setSkipCount(int skipCount) {
        this.skipCount = skipCount;
    }

    /**
     * An approximation of the mean tempo.
     * This may represent the speed of the rhythm of the associated track.
     * @return Approximated tempo in beats per minute.
     */
    public int getTempo() {
        return tempo;
    }

    public void setTempo(int tempo) {
        this.tempo = tempo;
    }

    /**
     * An approximation of the mean sound energy.
     * This arbitrary number represents how loud the sound is.
     * @return Approximated sound energy
     */
    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
    }
}
