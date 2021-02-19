/*
 * Copyright 2021 Thibault Seisel
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

package fr.nihilus.music.core.database.exclusion

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Store a list of tracks that should be excluded from the music library.
 */
@Dao
interface TrackExclusionDao {

    /**
     * Reactive list of tracks that should be excluded from the music library.
     * This flow emits once with the current list of excluded tracks,
     * then emits a new list whenever its content changes.
     *
     * Tracks are sorted by descending exclusion date.
     */
    @get:Query("SELECT * FROM track_exclusion ORDER BY exclude_date DESC")
    val trackExclusions: Flow<List<TrackExclusion>>

    /**
     * Mark a track as excluded from the music library.
     * @param track Reference to the track that should be excluded.
     */
    @Insert
    suspend fun exclude(track: TrackExclusion)

    /**
     * Remove a track from the exclusion list.
     * If that track was not excluded then nothing happens.
     *
     * @param trackId Identifier of the track that should be allowed again.
     */
    @Query("DELETE FROM track_exclusion WHERE track_id = :trackId")
    suspend fun allow(trackId: Long)
}