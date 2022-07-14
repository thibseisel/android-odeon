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

package fr.nihilus.music.service.browser.provider

import androidx.core.net.toUri
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ALBUMS
import fr.nihilus.music.media.albums.Album
import fr.nihilus.music.media.albums.AlbumRepository
import fr.nihilus.music.media.tracks.Track
import fr.nihilus.music.media.tracks.TrackRepository
import fr.nihilus.music.media.tracks.getAlbumTracks
import fr.nihilus.music.service.AudioTrack
import fr.nihilus.music.service.MediaCategory
import fr.nihilus.music.service.MediaContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class AlbumChildrenProvider @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val trackRepository: TrackRepository,
) : ChildrenProvider() {

    override fun findChildren(
        parentId: MediaId
    ): Flow<List<MediaContent>> {
        check(parentId.type == TYPE_ALBUMS)

        val albumId = parentId.category?.toLongOrNull()
        return when {
            albumId != null -> getAlbumTracks(albumId)
            else -> getAlbums()
        }
    }

    private fun getAlbums(): Flow<List<MediaCategory>> = albumRepository.albums.map { albums ->
        albums.map { it.toCategory() }
    }

    private fun getAlbumTracks(
        albumId: Long
    ): Flow<List<AudioTrack>> = trackRepository.getAlbumTracks(albumId).map { tracks ->
        tracks
            .map { it.toPlayableMedia() }
            .takeUnless { it.isEmpty() }
            ?: throw NoSuchElementException("No album with id = $albumId")
    }

    private fun Album.toCategory(): MediaCategory {
        return MediaCategory(
            id = MediaId(TYPE_ALBUMS, id.toString()),
            title = title,
            subtitle = artist,
            iconUri = albumArtUri?.toUri(),
            playable = true,
            count = trackCount
        )
    }

    private fun Track.toPlayableMedia(): AudioTrack {
        return AudioTrack(
            id = MediaId(TYPE_ALBUMS, albumId.toString(), id),
            title = title,
            artist = artist,
            album = album,
            mediaUri = mediaUri.toUri(),
            iconUri = albumArtUri?.toUri(),
            duration = duration,
            disc = discNumber,
            number = trackNumber
        )
    }
}
