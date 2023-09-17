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

import dagger.Reusable
import fr.nihilus.music.core.database.usage.MediaUsageEvent
import fr.nihilus.music.core.database.usage.UsageDao
import fr.nihilus.music.core.files.bytes
import fr.nihilus.music.core.os.Clock
import fr.nihilus.music.media.tracks.Track
import fr.nihilus.music.media.tracks.TrackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.sqrt

/**
 * The default implementation of [UsageManager] that filters and sorts tracks stored on the device
 * based on the user's usage events.
 * Depending on the requesting collection of tracks, each track is given a compliance score.
 * Tracks with the highest score are listed first, while tracks that score too loo are ignored.
 *
 * @param tracks The source for media files metadata.
 * @param usageDao The DAO that controls storage of playback statistics.
 */
@Reusable
internal class UsageManagerImpl @Inject constructor(
    private val tracks: TrackRepository,
    private val usageDao: UsageDao,
    private val clock: Clock
) : UsageManager {

    private val biggerScoreAndSizeFirst = Comparator<DisposableTrackScore> { a, b ->
        when (val scoreDiff = a.score - b.score) {
            0 -> -1 * (a.track.fileSize - b.track.fileSize).toInt()
            else -> -1 * scoreDiff
        }
    }

    override fun getMostRatedTracks(): Flow<List<Track>> = tracks.tracks.map { tracks ->
        val tracksById = tracks.associateBy { it.id }
        val trackScores = usageDao.getTracksUsage(0L)

        trackScores.asSequence()
            .mapNotNull { tracksById[it.trackId] }
            .take(25)
            .toCollection(ArrayList(25))
    }

    override fun getPopularTracksSince(period: Long, unit: TimeUnit): Flow<List<Track>> {
        require(period >= 0)

        return tracks.tracks.map { tracks ->
            val tracksById = tracks.associateBy { it.id }
            val tracksUsage =
                usageDao.getTracksUsage(clock.currentEpochTime - unit.toSeconds(period))

            if (tracksUsage.isEmpty()) emptyList() else {
                val bestScore = tracksUsage.first().score

                // We only consider the top 3 quartiles to ignore tracks that were only played punctually.
                val threshold = bestScore / 4

                tracksUsage.asSequence()
                    .filter { it.score > threshold }
                    .mapNotNull { tracksById[it.trackId] }
                    .toList()
            }
        }
    }

    override fun getDisposableTracks(): Flow<List<DisposableTrack>> = tracks.tracks.map { tracks ->
        val usageByTrack = usageDao.getTracksUsage(0L).associateBy { it.trackId }

        val currentEpochTime = clock.currentEpochTime
        tracks.mapTo(ArrayList(tracks.size)) { track ->
            val usage = usageByTrack[track.id]
            val playCount = usage?.score ?: 0
            val lastPlayedTime = usage?.lastEventTime ?: 0L

            val score =
                computeCleanupScore(track.fileSize, playCount, lastPlayedTime, currentEpochTime)
            DisposableTrackScore(track, usage?.lastEventTime, score)
        }
            .also { it.sortWith(biggerScoreAndSizeFirst) }
            .take(25)
            .map { (track, playTime, _) ->
                DisposableTrack(
                    trackId = track.id,
                    title = track.title,
                    fileSize = track.fileSize.bytes,
                    lastPlayedTime = playTime
                )
            }
    }

    override suspend fun reportCompletion(trackId: Long) {
        val newEvent = MediaUsageEvent(0, trackId, clock.currentEpochTime)
        usageDao.recordEvent(newEvent)
    }

    /**
     * Compute the cleanup score of a track.
     * The higher the score, the more likely the track should benefit from being deleted.
     *
     * @param fileSize The track file size in bytes.
     * @param playCount The number of times this track has been played.
     * Should be `0` if never played.
     * @param lastPlayTime The epoch time at which this track has been last played.
     * Should be `0` if never played.
     * @param currentEpochTime The current epoch time.
     */
    private fun computeCleanupScore(
        fileSize: Long,
        playCount: Int,
        lastPlayTime: Long,
        currentEpochTime: Long
    ): Int {
        var score = sizeScore(fileSize)

        if (playCount == 0) {
            // Apply a penalty to tracks that have never been played.
            // A hard penalty ensures that those tracks are listed first.
            score += NEVER_PLAYED_PENALTY
        } else {
            // Increase the score as days pass since last played,
            // but lower it based on the number of times it has been played.
            val daysSinceLastPlayed = (currentEpochTime - lastPlayTime) / ONE_DAY_IN_SECONDS
            score += lastPlayedScore(daysSinceLastPlayed.toInt())
            score -= playCountBonus(playCount)
        }

        return score
    }

    /**
     * Compute the portion of the score that depends on the size of the track file.
     * The current implementation is linear, which means that the score grows proportionally
     * with the file size.
     *
     * @param fileSize The size of the track file in bytes.
     */
    private fun sizeScore(fileSize: Long): Int = (fileSize / SIZE_FACTOR).toInt()

    /**
     * Compute the portion of the score that depends on the last time the track has been played.
     * The current implementation is based on a square root, which means that the score grows
     * with the number of days since last played, but slower.
     *
     * The curve has the following equation:
     *
     * `S(t) = K * sqrt(t)`
     *
     * where `S(t)` is the computed score, `t` is the number of days since last played
     * and `K` is a constant so the following is true:
     *
     * `S(F) = P`, with `F` = [LAST_PLAYED_FACTOR] and `P` = [NEVER_PLAYED_PENALTY].
     *
     * To satisfy those requirements, we found the following equation:
     *
     * `S(t) = P/F sqrt(Ft)`.
     */
    private fun lastPlayedScore(numberOfDays: Int): Int {
        var score = sqrt(numberOfDays.toDouble() * LAST_PLAYED_FACTOR)
        score *= (NEVER_PLAYED_PENALTY.toDouble() / LAST_PLAYED_FACTOR)
        return score.toInt()
    }

    /**
     * Compute points that should be subtracted from the score based on the number of times
     * the track has been played.
     * Since we don't want to list a track that has been played multiple time,
     * we apply the square function to make the score decrease faster as the play count grows.
     *
     * The equation is the following:
     *
     * `B(n) = Fn²` where `B(n)` is the amount of the bonus, `n` the play [count]
     * and `F` the [PLAY_COUNT_FACTOR].
     */
    private fun playCountBonus(count: Int): Int = count * count * PLAY_COUNT_FACTOR

    /**
     * Correlate track metadata with additional usage information.
     */
    private data class DisposableTrackScore(
        val track: Track,
        val lastPlayedTime: Long?,
        val score: Int
    )
}

/**
 * The number of seconds in a day.
 */
private const val ONE_DAY_IN_SECONDS = 24 * 3600

/**
 * Define the importance of the file size in selecting disposable tracks.
 * The lower the factor, the more a large track file is likely to be suggested for removal.
 *
 * The score is incremented by 1 for each group of [SIZE_FACTOR] bytes in the whole track file.
 */
private const val SIZE_FACTOR = 1024 * 256 // 1/4 megabyte

/**
 * Define the penalty applied to tracks that have never been played.
 * The higher the penalty, the more tracks that has been never played are likely to be suggested
 * for removal.
 */
private const val NEVER_PLAYED_PENALTY = 50

/**
 * Define the importance of the time elapsed since the track has been last played.
 * The lower the factor, the more likely a track that has not been played for some time
 * is to be suggested for removal.
 *
 * This is the number of days since a track has been last played, so that it has the same score
 * as if it had never been played.
 */
private const val LAST_PLAYED_FACTOR = 6 * 30 // 6 months

/**
 * Define the importance of the number of times a track has been played.
 * The higher the factor, the less a track that has been played a lot is likely to be suggested
 * for removal.
 */
private const val PLAY_COUNT_FACTOR = 1
