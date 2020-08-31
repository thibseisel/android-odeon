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
import androidx.core.net.toUri
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ARTISTS
import fr.nihilus.music.media.provider.Album
import fr.nihilus.music.media.provider.Artist
import fr.nihilus.music.media.provider.MediaDao
import fr.nihilus.music.media.provider.Track
import fr.nihilus.music.service.MediaCategory
import fr.nihilus.music.service.MediaContent
import fr.nihilus.music.service.AudioTrack
import fr.nihilus.music.service.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

internal class ArtistChildrenProvider(
    private val context: Context,
    private val mediaDao: MediaDao
) : ChildrenProvider() {

    override fun findChildren(parentId: MediaId): Flow<List<MediaContent>> {
        check(parentId.type == TYPE_ARTISTS)

        val artistId = parentId.category?.toLongOrNull()
        return when {
            artistId != null -> getArtistChildren(artistId)
            else -> getArtists()
        }
    }

    private fun getArtists(): Flow<List<MediaCategory>> = mediaDao.artists.map { artists ->
        artists.map { it.toCategory() }
    }

    private fun getArtistChildren(
        artistId: Long
    ): Flow<List<MediaContent>> = combine(mediaDao.albums, mediaDao.tracks) { albums, tracks ->
        val artistAlbums = albums.asSequence()
            .filter { it.artistId == artistId }
            .sortedByDescending { it.releaseYear }
            .map { album -> album.toCategory() }

        val artistTracks = tracks.asSequence()
            .filter { it.artistId == artistId }
            .map { track -> track.toPlayableMedia() }

        (artistAlbums + artistTracks)
            .toList()
            .takeUnless { it.isEmpty() }
            ?: throw NoSuchElementException("No artist with id = $artistId")
    }

    private fun Artist.toCategory() = MediaCategory(
        id = MediaId(TYPE_ARTISTS, id.toString()),
        title = name,
        subtitle = context.getString(R.string.svc_artist_subtitle, albumCount, trackCount),
        iconUri = iconUri?.toUri(),
        count = trackCount
    )

    private fun Album.toCategory(): MediaCategory = MediaCategory(
        id = MediaId(MediaId.TYPE_ALBUMS, id.toString()),
        title = title,
        subtitle = artist,
        iconUri = albumArtUri?.toUri(),
        count = trackCount
    )

    private fun Track.toPlayableMedia() = AudioTrack(
        id = MediaId(TYPE_ARTISTS, artistId.toString(), id),
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