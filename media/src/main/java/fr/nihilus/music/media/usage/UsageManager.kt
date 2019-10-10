/*
 * Copyright 2018 Thibault Seisel
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

import fr.nihilus.music.common.os.Clock
import fr.nihilus.music.database.usage.MediaUsageEvent
import fr.nihilus.music.database.usage.UsageDao
import fr.nihilus.music.media.dagger.ServiceScoped
import fr.nihilus.music.media.provider.Track
import fr.nihilus.music.media.repo.MediaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Manages reading and writing media usage statistics.
 */
interface UsageManager {

    /**
     * Retrieve the most rated tracks from the music library.
     * Tracks are sorted by descending score.
     */
    suspend fun getMostRatedTracks(): List<Track>

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
    suspend fun getDisposableTracks(): List<DisposableTrack>

    /**
     * Report that the track with the given [trackId] has been played until the end.
     *
     * @param trackId The unique identifier of the track that has been played.
     */
    fun reportCompletion(trackId: Long)
}

/**
 * Default implementation of the usage manager.
 *
 * @param scope The scope coroutines should be executed into.
 * @param repository The repository for media files.
 * @param usageDao The DAO that controls storage of usage statistics.
 */
@ServiceScoped
internal class UsageManagerImpl
@Inject constructor(
    private val scope: CoroutineScope,
    private val repository: MediaRepository,
    private val usageDao: UsageDao,
    private val clock: Clock
) : UsageManager {

    override suspend fun getMostRatedTracks(): List<Track> {
        val tracksById = repository.getTracks().associateBy { it.id }
        val trackScores = usageDao.getMostRatedTracks(25)

        return trackScores.mapNotNull { tracksById[it.trackId] }
    }

    override suspend fun getDisposableTracks(): List<DisposableTrack> = coroutineScope {
        val allTracksAsync = async { repository.getTracks() }

        // Retrieve usage records for each track.
        val usageByTrack = usageDao.getTracksUsage().associateBy { it.trackId }

        // Find all tracks that have never been played.
        val allTracks = allTracksAsync.await()
        val neverPlayedTracks = allTracks.asSequence()
            .filterNot { it.id in usageByTrack }
            .map { DisposableTrack(it.id, it.title, it.artist, it.fileSize, null) }

        // Of all tracks that have been played at least once,
        // select those that have not been played for more than a month.
        val lastMonth = clock.currentEpochTime - ONE_MONTH_MILLIS
        val tracksById = allTracks.associateBy { it.id }

        val notPlayedForMonthsTracks = usageByTrack.entries.asSequence()
            .filter { (_, usage) -> usage.lastEventTime < lastMonth }
            .mapNotNull { (trackId, usage) ->
                tracksById[trackId]?.let {
                    DisposableTrack(trackId, it.title, it.artist, it.fileSize, usage.lastEventTime)
                }
            }

        // Merge selected tracks and sort them by descending file size.
        (neverPlayedTracks + notPlayedForMonthsTracks)
            .sortedByDescending { it.fileSizeBytes }
            .take(25)
            .toList()
    }

    override fun reportCompletion(trackId: Long) {
        scope.launch {
            val newEvent = MediaUsageEvent(0, trackId, clock.currentEpochTime)
            usageDao.recordEvent(newEvent)
        }
    }
}

/**
 * The duration of one month in milliseconds.
 */
private const val ONE_MONTH_MILLIS = 3600 * 24 * 30

/**
 * Information on a track that could be deleted from the device's storage to free-up space.
 *
 * @param trackId Unique identifier of the related track.
 * @param title The display title of the related track.
 * @param fileSizeBytes The size of the file stored on the device's storage in bytes.
 * @param lastPlayedTime The epoch time at which that track has been played for the last time,
 * or `null` if it has never been played.
 */
class DisposableTrack(
    val trackId: Long,
    val title: String,
    val subtitle: String?,
    val fileSizeBytes: Long,
    val lastPlayedTime: Long?
)