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

package fr.nihilus.music.media.browser.provider

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ARTISTS
import fr.nihilus.music.media.AudioTrack
import fr.nihilus.music.media.MediaCategory
import fr.nihilus.music.media.MediaContent
import fr.nihilus.music.media.R
import fr.nihilus.music.media.albums.Album
import fr.nihilus.music.media.albums.AlbumRepository
import fr.nihilus.music.media.albums.getArtistAlbums
import fr.nihilus.music.media.artists.Artist
import fr.nihilus.music.media.artists.ArtistRepository
import fr.nihilus.music.media.tracks.Track
import fr.nihilus.music.media.tracks.TrackRepository
import fr.nihilus.music.media.tracks.getArtistTracks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class ArtistChildrenProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val artistRepository: ArtistRepository,
    private val albumRepository: AlbumRepository,
    private val trackRepository: TrackRepository,
) : ChildrenProvider() {

    override fun findChildren(parentId: MediaId): Flow<List<MediaContent>> {
        check(parentId.type == TYPE_ARTISTS)

        val artistId = parentId.category?.toLongOrNull()
        return when {
            artistId != null -> getArtistChildren(artistId)
            else -> getArtists()
        }
    }

    private fun getArtists(): Flow<List<MediaCategory>> = artistRepository.artists.map { artists ->
        artists.map { it.toCategory() }
    }

    private fun getArtistChildren(
        artistId: Long
    ): Flow<List<MediaContent>> = combine(
        albumRepository.getArtistAlbums(artistId),
        trackRepository.getArtistTracks(artistId)
    ) { albums, tracks ->
        val artistAlbums = albums.map { album -> album.toCategory() }
        val artistTracks = tracks.map { track -> track.toPlayableMedia() }

        (artistAlbums + artistTracks).takeUnless { it.isEmpty() }
            ?: throw NoSuchElementException("No artist with id = $artistId")
    }

    private fun Artist.toCategory() = MediaCategory(
        id = MediaId(TYPE_ARTISTS, id.toString()),
        title = name,
        subtitle = context.getString(R.string.artist_subtitle, albumCount, trackCount),
        iconUri = iconUri?.toUri(),
        count = trackCount
    )

    private fun Album.toCategory(): MediaCategory = MediaCategory(
        id = MediaId(MediaId.TYPE_ALBUMS, id.toString()),
        title = title,
        subtitle = artist,
        iconUri = albumArtUri?.toUri(),
        playable = true,
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
