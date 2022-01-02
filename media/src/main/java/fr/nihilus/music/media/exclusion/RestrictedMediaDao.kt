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

package fr.nihilus.music.media.exclusion

import dagger.Reusable
import fr.nihilus.music.core.context.AppCoroutineScope
import fr.nihilus.music.core.database.exclusion.TrackExclusion
import fr.nihilus.music.core.database.exclusion.TrackExclusionDao
import fr.nihilus.music.core.flow.dematerialize
import fr.nihilus.music.core.flow.materialize
import fr.nihilus.music.media.dagger.SourceDao
import fr.nihilus.music.media.provider.Album
import fr.nihilus.music.media.provider.Artist
import fr.nihilus.music.media.provider.MediaDao
import fr.nihilus.music.media.provider.Track
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.plus
import javax.inject.Inject

/**
 * Wraps a [source of music metadata][MediaDao] to hide tracks that are marked as excluded from the
 * music library by application users.
 * If an album or artist only has excluded tracks then it is also hidden.
 */
@Reusable
internal class RestrictedMediaDao @Inject constructor(
    @AppCoroutineScope private val scope: CoroutineScope,
    @SourceDao private val sourceDao: MediaDao,
    exclusionList: TrackExclusionDao
) : MediaDao by sourceDao {

    override val tracks: Flow<List<Track>> =
        combine(sourceDao.tracks, exclusionList.trackExclusions) { tracks, exclusions ->
            val excludedTrackIds = exclusions.mapTo(mutableSetOf(), TrackExclusion::trackId)
            tracks.filterNot { it.id in excludedTrackIds }
        }
            .materialize()
            .shareIn(scope + CoroutineName("TracksShare"), SharingStarted.WhileSubscribed(), 1)
            .dematerialize()

    override val albums: Flow<List<Album>>
        get() = combine(tracks, sourceDao.albums) { tracks, albums ->
            val trackCountPerAlbum = tracks.groupingBy { it.albumId }.eachCount()
            albums.mapNotNull { album ->
                when (val trackCount = trackCountPerAlbum[album.id]) {
                    null -> null
                    album.trackCount -> album
                    else -> album.copy(trackCount = trackCount)
                }
            }
        }

    override val artists: Flow<List<Artist>>
        get() = combine(tracks, sourceDao.artists) { tracks, artists ->
            val tracksPerArtist = tracks.groupBy { it.artistId }
            artists.mapNotNull { artist ->
                val artistTracks = tracksPerArtist[artist.id].orEmpty()
                val trackCount = artistTracks.size

                when {
                    trackCount == 0 -> null
                    artist.trackCount == trackCount -> artist
                    else -> {
                        val albumCount = artistTracks.mapTo(mutableSetOf(), Track::albumId).size
                        artist.copy(trackCount = trackCount, albumCount = albumCount)
                    }
                }
            }
        }
}
