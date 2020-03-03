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

import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.net.toUri
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ALBUMS
import fr.nihilus.music.media.provider.Album
import fr.nihilus.music.media.provider.Track
import fr.nihilus.music.media.repo.MediaRepository

internal class AlbumChildrenProvider(
    private val repository: MediaRepository
) : ChildrenProvider() {

    private val albumTrackOrdering = Comparator<Track> { a, b ->
        val discNumberDiff = a.discNumber - b.discNumber
        if (discNumberDiff != 0) discNumberDiff else (a.trackNumber - b.trackNumber)
    }

    override suspend fun findChildren(
        parentId: MediaId,
        fromIndex: Int,
        count: Int
    ): List<MediaItem>? {
        check(parentId.type == TYPE_ALBUMS)

        val albumId = parentId.category?.toLongOrNull()
        return when {
            albumId != null -> getAlbumTracks(albumId, fromIndex, count)
            else -> getAlbums(fromIndex, count)
        }
    }

    private suspend fun getAlbums(fromIndex: Int, count: Int): List<MediaItem>? {
        val builder = MediaDescriptionCompat.Builder()

        return repository.getAlbums().asSequence()
            .drop(fromIndex)
            .take(count)
            .map { it.toMediaItem(builder) }
            .toList()
    }

    private suspend fun getAlbumTracks(
        albumId: Long,
        fromIndex: Int,
        count: Int
    ): List<MediaItem>? {
        val builder = MediaDescriptionCompat.Builder()

        return repository.getTracks().asSequence()
            .filter { it.albumId == albumId }
            .sortedWith(albumTrackOrdering)
            .drop(fromIndex)
            .take(count)
            .map { it.toMediaItem(builder) }
            .toList()
            .takeUnless { it.isEmpty() }
    }

    private fun Album.toMediaItem(builder: MediaDescriptionCompat.Builder): MediaItem {
        return browsable(
            builder,
            id = MediaId.encode(TYPE_ALBUMS, id.toString()),
            title = title,
            subtitle = artist,
            trackCount = trackCount,
            iconUri = albumArtUri?.toUri()
        )
    }

    private fun Track.toMediaItem(builder: MediaDescriptionCompat.Builder): MediaItem {
        return playable(
            builder,
            id = MediaId.encode(TYPE_ALBUMS, albumId.toString(), id),
            title = title,
            subtitle = artist,
            mediaUri = mediaUri.toUri(),
            iconUri = albumArtUri?.toUri(),
            duration = duration,
            disc = discNumber,
            number = trackNumber
        )
    }
}
