/*
 * Copyright 2019 Thibault Seisel
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

package fr.nihilus.music.media.repo

import fr.nihilus.music.media.MediaId
import fr.nihilus.music.media.playlists.Playlist
import fr.nihilus.music.media.provider.Album
import fr.nihilus.music.media.provider.Artist
import fr.nihilus.music.media.provider.Track
import io.reactivex.Flowable

internal interface MediaRepository {
    val changeNotifications: Flowable<ChangeNotification>
    suspend fun getAllTracks(): List<Track>
    suspend fun getAllAlbums(): List<Album>
    suspend fun getAllArtists(): List<Artist>
    suspend fun getAllPlaylists(): List<Playlist>
    suspend fun getPlaylistTracks(playlistId: Long): List<Track>?
    suspend fun getMostRatedTracks(): List<Track>
}

internal suspend fun MediaRepository.getAlbumTracks(albumId: Long): List<Track>? =
    getAllTracks()
        .filter { it.albumId == albumId }
        .takeUnless { it.isEmpty() }

internal suspend fun MediaRepository.getArtistTracks(artistId: Long): List<Track>? =
    getAllTracks()
        .filter { it.artistId == artistId }
        .takeUnless { it.isEmpty() }

internal suspend fun MediaRepository.getArtistAlbums(artistId: Long): List<Album>? =
    getAllAlbums()
        .filter { it.artistId == artistId }
        .takeUnless { it.isEmpty() }

internal sealed class ChangeNotification(private val name: String) {
    override fun toString(): String = "NotificationChange ($name)"

    object AllTracks : ChangeNotification("All tracks")
    object AllAlbums : ChangeNotification("All albums")
    object AllArtists : ChangeNotification("All artists")
    object AllPlaylists : ChangeNotification("All playlists")
    data class Album(val albumId: Long) : ChangeNotification("Album with id $albumId")
    data class Artist(val artistId: Long): ChangeNotification("Artist with id $artistId")
    data class Playlist(val playlistId: Long) : ChangeNotification("Playlist with id $playlistId")
}

internal val ChangeNotification.mediaId: MediaId
    get() = when(this) {
        is ChangeNotification.AllTracks -> MediaId.fromParts(MediaId.TYPE_TRACKS, MediaId.CATEGORY_ALL)
        is ChangeNotification.AllAlbums -> MediaId.fromParts(MediaId.TYPE_ALBUMS)
        is ChangeNotification.AllArtists -> MediaId.fromParts(MediaId.TYPE_ARTISTS)
        is ChangeNotification.AllPlaylists -> MediaId.fromParts(MediaId.TYPE_PLAYLISTS)
        is ChangeNotification.Album -> MediaId.fromParts(MediaId.TYPE_ALBUMS, albumId.toString())
        is ChangeNotification.Artist -> MediaId.fromParts(MediaId.TYPE_ARTISTS, artistId.toString())
        is ChangeNotification.Playlist -> MediaId.fromParts(MediaId.TYPE_PLAYLISTS, playlistId.toString())
    }
