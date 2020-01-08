/*
 * Copyright 2019 Thibault Seisel
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

package fr.nihilus.music.core.database.spotify

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Define operations for persisting metadata from the Spotify API locally.
 */
@Dao
interface SpotifyDao {

    /**
     * Create or update audio features of a track.
     * If a link already exists for a track locally stored on the device
     * then the existing link is removed and replaced by the new one.
     *
     * @param link The association between a local track and the same track from the Spotify API.
     * @param feature The audio features of that track.
     * Its [TrackFeature.id] should be the id of the track on the Spotify API.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTrackFeature(link: SpotifyLink, feature: TrackFeature)

    /**
     * Returns all associations between tracks stored locally and tracks from the Spotify API.
     */
    @Query("SELECT * FROM remote_link")
    suspend fun getLinks(): List<SpotifyLink>

    /**
     * Returns the audio features of all tracks that have an existing link.
     */
    @Query(
        """
        SELECT remote_link.local_id, track_feature.*
        FROM remote_link 
        INNER JOIN track_feature ON remote_link.remote_id = track_feature.id
    """
    )
    suspend fun getLocalizedFeatures(): List<LocalizedTrackFeature>
}