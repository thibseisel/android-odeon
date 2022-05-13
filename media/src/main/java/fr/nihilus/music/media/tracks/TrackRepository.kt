/*
 * Copyright 2022 Thibault Seisel
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

package fr.nihilus.music.media.tracks

import fr.nihilus.music.core.context.AppCoroutineScope
import fr.nihilus.music.core.database.exclusion.TrackExclusion
import fr.nihilus.music.core.database.exclusion.TrackExclusionDao
import fr.nihilus.music.core.os.Clock
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.plus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackRepository @Inject internal constructor(
    @AppCoroutineScope private val appScope: CoroutineScope,
    private val clock: Clock,
    private val sourceDao: TrackLocalSource,
    private val exclusionDao: TrackExclusionDao,
) {
    /**
     * Live list of all tracks from the music library.
     */
    val tracks: Flow<List<Track>> by lazy {
        getIncludedTracks()
            .shareIn(
                scope = appScope + CoroutineName("TracksShare"),
                started = SharingStarted.WhileSubscribed(),
                replay = 1
            )
    }

    /**
     * Excludes a track from the music library.
     * Unlike deleted tracks, excluded tracks are not removed from the device's storage.
     *
     * @param trackId Unique identifier of the track to exclude from the music library.
     */
    suspend fun excludeTrack(trackId: Long) {
        exclusionDao.exclude(TrackExclusion(trackId, clock.currentEpochTime))
    }

    /**
     * Re-includes a previously excluded track into the music library.
     *
     * @param excludedTrackId Unique identifier of a track that have previously been excluded.
     */
    suspend fun allowTrack(excludedTrackId: Long) {
        exclusionDao.allow(excludedTrackId)
    }

    /**
     * Removes multiple tracks from the music library, deleting them from the device's storage.
     * Deleting tracks is a sensitive operation that may require explicit consent from the user.
     *
     * @param trackIds Identifiers of tracks to be deleted.
     * @return Result of the delete operation.
     */
    suspend fun deleteTracks(trackIds: LongArray): DeleteTracksResult =
        sourceDao.deleteTracks(trackIds)

    private fun getIncludedTracks() =
        combine(sourceDao.tracks, exclusionDao.trackExclusions) { tracks, exclusions ->
            val exclusionTrackIds = exclusions.mapTo(mutableSetOf(), TrackExclusion::trackId)
            tracks.filterNot { it.id in exclusionTrackIds }
        }
}

fun TrackRepository.getAlbumTracks(albumId: Long): Flow<List<Track>> =
    tracks.map { allTracks ->
        allTracks
            .filter { it.albumId == albumId }
            .sortedWith(compareBy(Track::discNumber, Track::trackNumber))
    }

fun TrackRepository.getArtistTracks(artistId: Long): Flow<List<Track>> =
    tracks.map { allTracks ->
        allTracks.filter { it.artistId == artistId }
    }
