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

import fr.nihilus.music.media.di.ServiceScoped
import fr.nihilus.music.media.os.Clock
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
internal interface MediaUsageManager {

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
    private val usageDao: MediaUsageDao,
    private val clock: Clock
) : MediaUsageManager {

    override suspend fun getMostRatedTracks(): List<Track> {
        val tracksById = repository.getAllTracks().associateBy { it.id }
        val trackScores = usageDao.getMostRatedTracks(25)

        return trackScores.mapNotNull { tracksById[it.trackId] }
    }

    override suspend fun getDisposableTracks(): List<DisposableTrack> = coroutineScope {
        val allTracks = async { repository.getAllTracks() }
        val usageByTrack = usageDao.getTracksUsage().groupBy { it.trackId }

        allTracks.await().asSequence()
            .filterNot { usageByTrack.containsKey(it.id) }
            .map { DisposableTrack(it.id, it.title, it.fileSize, null) }
            .sortedByDescending { it.fileSizeBytes }
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
 * Information on a track that could be deleted from the device's storage to free-up space.
 *
 * @param trackId Unique identifier of the related track.
 * @param title The display title of the related track.
 * @param fileSizeBytes The size of the file stored on the device's storage in bytes.
 * @param lastPlayedTime The epoch time at which that track has been played for the last time,
 * or `null` if it has never been played.
 */
internal class DisposableTrack(
    val trackId: Long,
    val title: String,
    val fileSizeBytes: Long,
    val lastPlayedTime: Long?
)