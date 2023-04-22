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

import fr.nihilus.music.core.collections.associateByLong
import fr.nihilus.music.core.context.AppCoroutineScope
import fr.nihilus.music.core.database.exclusion.TrackExclusion
import fr.nihilus.music.core.database.exclusion.TrackExclusionDao
import fr.nihilus.music.core.os.Clock
import fr.nihilus.music.media.tracks.local.TrackLocalSource
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
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
    private val allTracks: Flow<List<Track>> by lazy {
        combineTrackSources().shareIn(
            scope = appScope + CoroutineName("TracksCache"),
            started = SharingStarted.WhileSubscribed(),
            replay = 1,
        )
    }

    /**
     * Live list of all tracks from the music library.
     */
    val tracks: Flow<List<Track>>
        get() = allTracks.map { allTracks ->
            allTracks.filter { it.exclusionTime == null }
        }

    /**
     * Live list of tracks that have been excluded from the music library.
     */
    val excludedTracks: Flow<List<Track>>
        get() = allTracks.map { allTracks ->
            allTracks.filter { it.exclusionTime != null }
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

    private fun combineTrackSources(): Flow<List<Track>> =
        combine(sourceDao.tracks, exclusionDao.trackExclusions) { tracks, exclusions ->
            val exclusionsByTrackId = exclusions.associateByLong(TrackExclusion::trackId)
            tracks.map {
                Track(
                    id = it.id,
                    title = it.title,
                    artistId = it.artistId,
                    artist = it.artist,
                    albumId = it.albumId,
                    album = it.album,
                    duration = it.duration,
                    discNumber = it.discNumber,
                    trackNumber = it.trackNumber,
                    mediaUri = it.mediaUri,
                    albumArtUri = it.albumArtUri,
                    availabilityDate = it.availabilityDate,
                    fileSize = it.fileSize,
                    exclusionTime = exclusionsByTrackId[it.id]?.excludeDate
                )
            }
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
