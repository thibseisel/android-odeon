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
import fr.nihilus.music.media.provider.MediaDao
import fr.nihilus.music.media.provider.Track
import fr.nihilus.music.service.AudioTrack
import fr.nihilus.music.service.MediaCategory
import fr.nihilus.music.service.MediaContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class AlbumChildrenProvider(
    private val mediaDao: MediaDao
) : ChildrenProvider() {

    private val albumTrackOrdering = Comparator<Track> { a, b ->
        val discNumberDiff = a.discNumber - b.discNumber
        if (discNumberDiff != 0) discNumberDiff else (a.trackNumber - b.trackNumber)
    }

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

    private fun getAlbums(): Flow<List<MediaCategory>> = mediaDao.albums.map { albums ->
        albums.map {
            MediaCategory(
                id = MediaId(TYPE_ALBUMS, it.id.toString()),
                title = it.title,
                subtitle = it.artist,
                iconUri = it.albumArtUri?.toUri(),
                trackCount = it.trackCount
            )
        }
    }

    private fun getAlbumTracks(albumId: Long): Flow<List<AudioTrack>> =
        mediaDao.tracks.map { tracks ->
            tracks.asSequence()
                .filter { it.albumId == albumId }
                .sortedWith(albumTrackOrdering)
                .map { it.asContent() }
                .toList()
                .takeUnless { it.isEmpty() }
                ?: throw NoSuchElementException("No album with id = $albumId")
        }

    private fun Track.asContent(): AudioTrack {
        return AudioTrack(
            id = MediaId(TYPE_ALBUMS, albumId.toString(), id),
            title = title,
            subtitle = artist,
            album = album,
            artist = artist,
            duration = duration,
            discNumber = discNumber,
            trackNumber = trackNumber,
            mediaUri = mediaUri.toUri(),
            iconUri = albumArtUri?.toUri()
        )
    }
}
