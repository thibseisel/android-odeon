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

package fr.nihilus.music.media.artists

import fr.nihilus.music.media.tracks.Track
import fr.nihilus.music.media.tracks.TrackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ArtistRepository @Inject internal constructor(
    private val artistSource: ArtistLocalSource,
    private val trackRepository: TrackRepository,
) {
    /**
     * Live list of all artists that contributed to tracks in the music library.
     * The list of artists is sorted alphabetically by [their name][Artist.name].
     */
    val artists: Flow<List<Artist>>
        get() = combine(artistSource.artists, trackRepository.tracks) { allArtists, tracks ->
            val tracksPerArtist = tracks.groupBy(Track::artistId)
            allArtists.mapNotNull { artist ->
                val artistTracks = tracksPerArtist[artist.id].orEmpty()

                when (val trackCount = artistTracks.size) {
                    0 -> null
                    artist.trackCount -> artist
                    else -> {
                        val albumCount = artistTracks.distinctBy(Track::albumId).size
                        artist.copy(trackCount = trackCount, albumCount = albumCount)
                    }
                }
            }
        }
}
