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

package fr.nihilus.music.library.playlists

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.github.thibseisel.kdenticon.Identicon
import com.github.thibseisel.kdenticon.IdenticonStyle
import com.github.thibseisel.kdenticon.android.drawToBitmap
import fr.nihilus.music.core.context.AppDispatchers
import fr.nihilus.music.core.database.playlists.Playlist
import fr.nihilus.music.core.database.playlists.PlaylistDao
import fr.nihilus.music.core.database.playlists.PlaylistTrack
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_PLAYLISTS
import fr.nihilus.music.core.os.Clock
import fr.nihilus.music.core.os.FileSystem
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * The width/height in pixels of the generated playlist icons.
 */
private const val ICON_SIZE_PX = 320

/**
 * The name of the folder into which generated playlist icons are written.
 * This folder only contains PNG files.
 */
private const val PLAYLIST_ICON_FOLDER = "playlist_icons"

/**
 * Handler for actions related to the management of user-defined playlists.
 *
 * Some actions require to specify the media id of the playlist it should operate on.
 * The media id of a playlist should have the following format:
 * - [type][MediaId.type]         = [TYPE_PLAYLISTS]
 * - [category][MediaId.category] = an integer which is the unique identifier of that playlist
 * - [track][MediaId.track]       = `null`
 *
 * Track media ids can have any [type][MediaId.type] or [category][MediaId.category]
 * but should have a non-`null` [track identifier][MediaId.track].
 *
 * @property playlistDao Handle to the playlists storage.
 * @property files Handle to read/write internal storage files.
 * @property clock Provider of the system time.
 * @property dispatchers Set of coroutine dispatchers.
 */
internal class ManagePlaylistAction @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val files: FileSystem,
    private val clock: Clock,
    private val dispatchers: AppDispatchers
) {

    private val reWhitespaces = Regex("\\s")

    /**
     * Creates a new user-defined playlist with the given [name] and optional track members.
     *
     * @param name The name of the playlist to create. It should be non-empty and not blank.
     * @param members Optional media ids of tracks to be added to the newly created playlist,
     * in the order of addition.
     */
    suspend fun createPlaylist(name: String, members: List<MediaId>) {
        require(name.isNotBlank())
        val trackIds = requireTrackIds(members)

        val iconUri = generatePlaylistIcon(name)
        val newPlaylist = Playlist(null, name, clock.currentEpochTime, iconUri)
        playlistDao.createPlaylist(newPlaylist, trackIds)
    }

    /**
     * Adds track members to an existing user-defined playlist, right after existing members.
     * Tracks are appended in the same order they were specified.
     * If some of those tracks are already part of the target playlist then those are not appended.
     *
     * Any attempt to add tracks to a non-existing playlist will fail with an exception.
     *
     * @param targetPlaylist The media id of the playlist to which tracks should be added.
     * @param members The media ids of tracks to be added to the playlist.
     */
    suspend fun appendMembers(targetPlaylist: MediaId, members: List<MediaId>) {
        val playlistId = requirePlaylistId(targetPlaylist)
        val trackIds = requireTrackIds(members)

        if (trackIds.isNotEmpty()) {
            val playlistTracks = trackIds.map { PlaylistTrack(playlistId, it) }
            playlistDao.addTracks(playlistTracks)
        }
    }

    /**
     * Deletes an existing user-defined playlist.
     * Tracks that were part of the playlist are not deleted themselves.
     * Attempting to delete a non-existing playlist will complete without reporting an error.
     *
     * @param targetPlaylist The media id of the playlist to delete.
     */
    suspend fun deletePlaylist(targetPlaylist: MediaId) {
        val playlistId = requirePlaylistId(targetPlaylist)
        playlistDao.deletePlaylist(playlistId)
    }

    private suspend fun generatePlaylistIcon(playlistName: String): Uri? = withContext(dispatchers.Default) {
        val iconSpec = Identicon.fromValue(playlistName, ICON_SIZE_PX).apply {
            style = IdenticonStyle(
                backgroundColor = Color.TRANSPARENT,
                padding = 0f
            )
        }

        val outputBitmap = Bitmap.createBitmap(ICON_SIZE_PX, ICON_SIZE_PX, Bitmap.Config.ARGB_8888)
        iconSpec.drawToBitmap(outputBitmap)

        withContext(dispatchers.IO) {
            // Sanitize playlist name to make a filename without spaces
            val fileName = playlistName.replace(reWhitespaces, "_")
            files.writeBitmapToInternalStorage("$PLAYLIST_ICON_FOLDER/$fileName.png", outputBitmap)
        }
    }

    private fun requirePlaylistId(playlist: MediaId): Long {
        val playlistId = playlist.category?.toLongOrNull()
        require(playlist.type == TYPE_PLAYLISTS && playlistId != null && playlist.track == null) {
            "Expected the media id of a playlist, but got $playlist"
        }

        return playlistId
    }

    private fun requireTrackIds(mediaIds: List<MediaId>): LongArray {
        return LongArray(mediaIds.size) {
            val mediaId = mediaIds[it]
            requireNotNull(mediaId.track) { "Invalid track media id: $mediaId" }
        }
    }
}