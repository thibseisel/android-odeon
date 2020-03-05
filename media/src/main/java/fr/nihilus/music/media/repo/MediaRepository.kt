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

import fr.nihilus.music.core.database.playlists.Playlist
import fr.nihilus.music.core.os.PermissionDeniedException
import fr.nihilus.music.media.provider.Album
import fr.nihilus.music.media.provider.Artist
import fr.nihilus.music.media.provider.Track
import kotlinx.coroutines.flow.Flow

/**
 * Main entry point for accessing and modifying media.
 * This class may cache loaded media to improve further access time.
 *
 * Due to media being read from the device's storage, most operations requires permission
 * to read from the external storage.
 */
@Deprecated("It is dangerous for MediaRepository to keep active subscriptions to media collections. " +
        "MediaDao and PlaylistDao should be used directly and BrowserTree should expose Flows instead of suspend functions.")
interface MediaRepository {

    /**
     * Stream of media change notifications.
     * Each element from this flow is a subclass of [ChangeNotification]
     * indicating which media collection has changed.
     */
    val changeNotifications: Flow<ChangeNotification>

    /**
     * Load the list of all available tracks.
     * Tracks are sorted by title in an alternative alphabetical order having non-letter characters
     * listed first and ignoring common english prefixes such as "the" or "an".
     *
     * @return The list of all tracks sorted by title.
     * @throws PermissionDeniedException If permission to read external storage is denied.
     */
    suspend fun getTracks(): List<Track>

    /**
     * Load the list of all available albums.
     * Albums are sorted by title in an alternative alphabetical order having non-letter characters
     * listed first and ignoring common english prefixes such as "the" or "an".
     *
     * @return The list of all albums sorted by title.
     * @throws PermissionDeniedException If permission to read external storage is denied.
     */
    suspend fun getAlbums(): List<Album>

    /**
     * Load the list of all available artists.
     * Artists are sorted by name in an alternative alphabetical order having non-letter characters
     * listed first and ignoring common english prefixes such as "the" or "an".
     *
     * @return The list of all artists sorted by name.
     * @throws PermissionDeniedException If permission to read external storage is denied.
     */
    suspend fun getArtists(): List<Artist>

    /**
     * Load the list of all user-defined playlists that have been created by [createPlaylist].
     * Playlists are sorted by ascending creation date, i.e. older playlists are listed first.
     *
     * @return The list of all user-defined playlists.
     */
    suspend fun getPlaylists(): List<Playlist>

    /**
     * Load the list of tracks that are part of the playlist with the specified [playlistId].
     * If no such a playlist exists then `null` is returned.
     * Tracks are ordered in the same order they were added to the playlist.
     *
     * @param playlistId [Playlist.id] of the playlist whose tracks are to be loaded.
     * @return The list of tracks that are part of the playlist,
     * or `null` if the playlist does not exist.
     */
    suspend fun getPlaylistTracks(playlistId: Long): List<Track>?

    /**
     * Create and new playlist with an initial set of tracks.
     * Additional tracks can be appended to the playlist with [addTracksToPlaylist].
     *
     * @param newPlaylist The playlist to create. Its id should be `null`.
     * @param trackIds Unique identifiers of the tracks that should be added to the playlist
     * as per [Track.id]. Passing no ids will successfully create a playlist with no tracks.
     */
    suspend fun createPlaylist(newPlaylist: Playlist, trackIds: LongArray)

    /**
     * Append tracks to an existing playlist with the specified [playlistId].
     * If there is no such playlist then nothing happens.
     * Tracks are added at the end of the playlist in the same order they were listed in [trackIds].
     *
     * If one or more tracks whose id is listed in [trackIds] are already part of the playlist
     * then those tracks are ignored and not added again.
     *
     * @param playlistId The [Playlist.id] of the playlist to which tracks should be added.
     * This should be an existing playlist.
     * @param trackIds Unique identifiers of the tracks, as per [Track.id].
     */
    suspend fun addTracksToPlaylist(playlistId: Long, trackIds: LongArray)

    /**
     * Delete multiple tracks from the repository.
     * Those tracks will be removed from the result of [getTracks] and from playlists
     * that reference them.
     * If one or more ids in [trackIds] don't match existing tracks,
     * then no track will be deleted for those ids.
     *
     * This operation requires user permission to write to external storage.
     *
     * @param trackIds Unique identifiers of tracks that should be deleted, as per [Track.id].
     * Passing an empty array will do nothing.
     * @return The number of tracks that have been deleted.
     * @throws PermissionDeniedException If permission to write to external storage is denied.
     */
    suspend fun deleteTracks(trackIds: LongArray): Int

    /**
     * Delete a playlist from the repository.
     * Tracks that are part of the playlist are not deleted themselves ;
     * only their association with the playlist is removed.
     * Nothing happens if the target playlist does not exist.
     *
     * @param playlistId The identifier of the playlist to delete, as per [Playlist.id].
     */
    suspend fun deletePlaylist(playlistId: Long)
}

sealed class ChangeNotification(private val name: String) {
    override fun toString(): String = "NotificationChange ($name)"

    object AllTracks : ChangeNotification("All tracks")
    object AllAlbums : ChangeNotification("All albums")
    object AllArtists : ChangeNotification("All artists")
    object AllPlaylists : ChangeNotification("All playlists")
    data class Album(val albumId: Long) : ChangeNotification("Album with id $albumId")
    data class Artist(val artistId: Long) : ChangeNotification("Artist with id $artistId")
    data class Playlist(val playlistId: Long) : ChangeNotification("Playlist with id $playlistId")
}