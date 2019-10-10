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

import fr.nihilus.music.common.database.playlists.Playlist
import fr.nihilus.music.media.provider.Album
import fr.nihilus.music.media.provider.Artist
import fr.nihilus.music.media.provider.Track
import io.reactivex.Flowable

interface MediaRepository {
    val changeNotifications: Flowable<ChangeNotification>
    suspend fun getTracks(): List<Track>
    suspend fun getAlbums(): List<Album>
    suspend fun getArtists(): List<Artist>
    suspend fun getPlaylists(): List<Playlist>
    suspend fun getPlaylistTracks(playlistId: Long): List<Track>?
    suspend fun createPlaylist(newPlaylist: Playlist, trackIds: LongArray)
    suspend fun deleteTracks(trackIds: LongArray): Int
    suspend fun deletePlaylist(playlistId: Long)
}

sealed class ChangeNotification(private val name: String) {
    override fun toString(): String = "NotificationChange ($name)"

    object AllTracks : ChangeNotification("All tracks")
    object AllAlbums : ChangeNotification("All albums")
    object AllArtists : ChangeNotification("All artists")
    object AllPlaylists : ChangeNotification("All playlists")
    data class Album(val albumId: Long) : ChangeNotification("Album with id $albumId")
    data class Artist(val artistId: Long): ChangeNotification("Artist with id $artistId")
    data class Playlist(val playlistId: Long) : ChangeNotification("Playlist with id $playlistId")
}