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

package fr.nihilus.music.media.usage

internal val SAMPLE_TRACK_SCORE = listOf(
    TrackScore(75L, 82),
    TrackScore(464L, 66),
    TrackScore(294L, 43),
    TrackScore(477L, 25),
    TrackScore(48L, 20),
    TrackScore(481L, 12)
)

internal class TestUsageDao(
    initialScores: List<TrackScore> = emptyList()
) : MediaUsageDao {

    private val scorePerTrack = mutableMapOf<Long, Int>().apply {
        initialScores.forEach { (trackId, score) ->
            put(trackId, score)
        }
    }

    override suspend fun recordEvent(usageEvent: MediaUsageEvent) {
        val currentScore = scorePerTrack.getOrElse(usageEvent.trackId, { 0 })
        scorePerTrack[usageEvent.trackId] = currentScore + 1
    }

    override suspend fun getTracksUsage(): List<TrackUsage> = TODO()

    override suspend fun getMostRatedTracks(limit: Int): List<TrackScore> {
        return scorePerTrack.map { (trackId, score) -> TrackScore(trackId, score) }
            .sortedByDescending { it.score }
            .take(limit)
    }

    override suspend fun deleteEventsForTracks(trackIds: LongArray) {
        trackIds.forEach { scorePerTrack.remove(it) }
    }
}