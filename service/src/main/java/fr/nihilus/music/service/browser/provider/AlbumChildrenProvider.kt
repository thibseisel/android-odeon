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
import fr.nihilus.music.media.provider.MediaDao
import fr.nihilus.music.media.provider.Track
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
    ): Flow<List<MediaItem>> {
        check(parentId.type == TYPE_ALBUMS)

        val albumId = parentId.category?.toLongOrNull()
        return when {
            albumId != null -> getAlbumTracks(albumId)
            else -> getAlbums()
        }
    }

    private fun getAlbums(): Flow<List<MediaItem>> = mediaDao.albums.map { albums ->
        val builder = MediaDescriptionCompat.Builder()
        albums.map { it.toMediaItem(builder) }
    }

    private fun getAlbumTracks(
        albumId: Long
    ): Flow<List<MediaItem>> = mediaDao.tracks.map { tracks ->
        val builder = MediaDescriptionCompat.Builder()

        tracks.asSequence()
            .filter { it.albumId == albumId }
            .sortedWith(albumTrackOrdering)
            .map { it.toMediaItem(builder) }
            .toList()
            .takeUnless { it.isEmpty() }
            ?: throw NoSuchElementException("No album with id = $albumId")
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
