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

import android.content.Context
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.net.toUri
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ARTISTS
import fr.nihilus.music.core.media.MediaId.Builder.encode
import fr.nihilus.music.media.provider.Album
import fr.nihilus.music.media.provider.Artist
import fr.nihilus.music.media.provider.MediaDao
import fr.nihilus.music.media.provider.Track
import fr.nihilus.music.service.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

internal class ArtistChildrenProvider(
    private val context: Context,
    private val mediaDao: MediaDao
) : ChildrenProvider() {

    override fun findChildren(
        parentId: MediaId,
        fromIndex: Int,
        count: Int
    ): Flow<List<MediaItem>> {
        check(parentId.type == TYPE_ARTISTS)

        val artistId = parentId.category?.toLongOrNull()
        return when {
            artistId != null -> getArtistChildren(artistId, fromIndex, count)
            else -> getArtists(fromIndex, count)
        }
    }

    private fun getArtists(
        fromIndex: Int,
        count: Int
    ): Flow<List<MediaItem>> = mediaDao.artists.map { artists ->
        val builder = MediaDescriptionCompat.Builder()

        artists.asSequence()
            .drop(fromIndex)
            .take(count)
            .map { it.toMediaItem(builder) }
            .toList()
    }

    private fun getArtistChildren(
        artistId: Long,
        fromIndex: Int,
        count: Int
    ): Flow<List<MediaItem>> = combine(mediaDao.albums, mediaDao.tracks) { albums, tracks ->
        val builder = MediaDescriptionCompat.Builder()

        val artistAlbums = albums.asSequence()
            .filter { it.artistId == artistId }
            .sortedByDescending { it.releaseYear }
            .map { album -> album.toMediaItem(builder) }

        val artistTracks = tracks.asSequence()
            .filter { it.artistId == artistId }
            .map { track -> track.toMediaItem(builder) }

        (artistAlbums + artistTracks)
            .drop(fromIndex)
            .take(count)
            .toList()
            .takeUnless { it.isEmpty() }
            ?: throw NoSuchElementException("No artist with id = $artistId")
    }

    private fun Artist.toMediaItem(builder: MediaDescriptionCompat.Builder): MediaItem = browsable(
        builder,
        id = encode(TYPE_ARTISTS, id.toString()),
        title = name,
        subtitle = context.getString(R.string.svc_artist_subtitle, albumCount, trackCount),
        trackCount = trackCount,
        iconUri = iconUri?.toUri()
    )

    private fun Album.toMediaItem(builder: MediaDescriptionCompat.Builder): MediaItem = browsable(
        builder,
        id = encode(MediaId.TYPE_ALBUMS, id.toString()),
        title = title,
        subtitle = artist,
        trackCount = trackCount,
        iconUri = albumArtUri?.toUri()
    )

    private fun Track.toMediaItem(builder: MediaDescriptionCompat.Builder): MediaItem = playable(
        builder,
        id = encode(TYPE_ARTISTS, artistId.toString(), id),
        title = title,
        subtitle = artist,
        mediaUri = mediaUri.toUri(),
        iconUri = albumArtUri?.toUri(),
        duration = duration,
        disc = discNumber,
        number = trackNumber
    )
}