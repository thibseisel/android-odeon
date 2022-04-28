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

package fr.nihilus.music.media

import fr.nihilus.music.media.provider.Album
import fr.nihilus.music.media.provider.AlbumLocalSource
import fr.nihilus.music.media.provider.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AlbumRepository @Inject internal constructor(
    private val albumSource: AlbumLocalSource,
    private val trackRepository: TrackRepository,
) {
    val albums: Flow<List<Album>>
        get() = combine(albumSource.albums, trackRepository.tracks) { allAlbums, allTracks ->
            val trackCountPerAlbum = allTracks.groupingBy { it.albumId }.eachCount()
            allAlbums.mapNotNull { album ->
                when (val trackCount = trackCountPerAlbum[album.id]) {
                    null -> null
                    album.trackCount -> album
                    else -> album.copy(trackCount = trackCount)
                }
            }
        }
}

fun AlbumRepository.getArtistAlbums(artistId: Long): Flow<List<Album>> =
    albums.map { allAlbums ->
        allAlbums
            .filter { it.artistId == artistId }
            .sortedByDescending { it.releaseYear }
    }
