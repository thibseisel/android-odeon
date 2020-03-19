/*
 * Copyright 2020 Thibault Seisel
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

package fr.nihilus.music.media.usage

import fr.nihilus.music.media.provider.Track
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

/**
 * Manages reading and writing media playback statistics.
 */
interface UsageManager {

    /**
     * Retrieve the most rated tracks from the music library.
     * Tracks are sorted by descending score.
     */
    fun getMostRatedTracks(): Flow<List<Track>>

    /**
     * Retrieve tracks that have been played the most in the last specified time period.
     * Tracks are sorted by descending score.
     *
     * @param period The duration of the time period to retrieve.
     * For example, to get the most popular tracks in the 2 latest weeks
     * you should call `getPopularTracksSince(14, TimeUnit.DAYS)`.
     * @param unit The unit of the [period].
     */
    fun getPopularTracksSince(period: Long, unit: TimeUnit): Flow<List<Track>>

    /**
     * Load tracks that could be deleted by the user to free-up the device's storage.
     * The returned tracks are sorted, so that tracks that would most benefit from being deleted are listed first.
     *
     * A track is likely to be selected if at least one of the following are true:
     * - its associated file has a large size,
     * - it has never been played,
     * - it has not been played for a long time,
     * - it scores low.
     *
     * @return A list of tracks that could be deleted.
     */
    fun getDisposableTracks(): Flow<List<DisposableTrack>>

    /**
     * Report that the track with the given [trackId] has been played until the end.
     *
     * @param trackId The unique identifier of the track that has been played.
     */
    suspend fun reportCompletion(trackId: Long)
}