/*
 * Copyright 2022 Thibault Seisel
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

package fr.nihilus.music.media.playlist

import android.graphics.Bitmap
import android.net.Uri
import fr.nihilus.music.core.context.AppDispatchers
import fr.nihilus.music.core.database.playlists.Playlist
import fr.nihilus.music.core.database.playlists.PlaylistDao
import fr.nihilus.music.core.database.playlists.PlaylistTrack
import fr.nihilus.music.core.os.Clock
import fr.nihilus.music.core.os.IconContentUri
import fr.nihilus.music.core.os.PlaylistIconDir
import fr.nihilus.music.media.TrackRepository
import fr.nihilus.music.media.provider.Track
import io.github.thibseisel.identikon.Identicon
import io.github.thibseisel.identikon.IdenticonStyle
import io.github.thibseisel.identikon.drawToBitmap
import io.github.thibseisel.identikon.rendering.Color
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Provider

private const val ICON_SIZE_PX = 320

class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val trackRepository: TrackRepository,
    @PlaylistIconDir private val iconDir: Provider<File>,
    @IconContentUri private val baseIconUri: Uri,
    private val clock: Clock,
    private val dispatchers: AppDispatchers,
) {

    /**
     * Live list of all user-defined playlists, sorted by ascending
     * [created time][Playlist.created].
     */
    val playlists: Flow<List<Playlist>>
        get() = playlistDao.playlists

    /**
     * Lists all tracks that are part of a given user-defined playlist,
     * sorted by their position in the whole playlist.
     * This returns an empty list when no playlist matches the given id.
     *
     * @param playlistId Unique identifier of the desired playlist.
     * @return Live list of tracks that are part of a given playlist.
     */
    fun getPlaylistTracks(playlistId: Long): Flow<List<Track>> =
        combine(
            playlistDao.getPlaylistTracks(playlistId),
            trackRepository.tracks
        ) { members, allTracks ->
            val tracksById = allTracks.associateBy(Track::id)
            members.mapNotNull { tracksById[it.trackId] }
        }

    /**
     * Creates a new user-defined playlist.
     * @param name Name given to the newly created playlist.
     */
    suspend fun createUserPlaylist(name: String): Playlist {
        require(name.isNotBlank())

        val playlistIconUri = withContext(dispatchers.Default) {
            val iconSpec = Identicon.fromValue(
                value = name,
                size = ICON_SIZE_PX,
                style = IdenticonStyle(
                    backgroundColor = Color.hex(0u),
                    padding = 0f
                )
            )
            val iconBitmap =
                Bitmap.createBitmap(ICON_SIZE_PX, ICON_SIZE_PX, Bitmap.Config.ARGB_8888)
            iconSpec.drawToBitmap(iconBitmap)

            // Sanitize playlist name to make a filename without spaces
            val iconFilename = name.replace(Regex("\\s"), "_") + ".png"
            writePlaylistIconFile(iconFilename, iconBitmap)
            Uri.withAppendedPath(baseIconUri, iconFilename)
        }

        val newPlaylist = Playlist(id = 0, name, clock.currentEpochTime, playlistIconUri)
        val newPlaylistId = playlistDao.savePlaylist(newPlaylist)
        return newPlaylist.copy(id = newPlaylistId)
    }

    private suspend fun writePlaylistIconFile(
        iconFilename: String,
        iconBitmap: Bitmap
    ) = withContext(dispatchers.IO) {
        val iconDir = iconDir.get()
        check(iconDir.exists() || iconDir.mkdirs()) {
            "Unable to create playlist icon directory"
        }
        File(iconDir, iconFilename).outputStream().use { iconFile ->
            iconBitmap.compress(Bitmap.CompressFormat.PNG, 100, iconFile)
        }
    }

    /**
     * Appends tracks to an existing playlist.
     * @param playlistId [Playlist.id] of an existing playlist.
     * @param trackIds [Identifier of tracks][Track.id] to be added to the playlist.
     * tracks are added at the end of the playlist in the same order.
     */
    suspend fun addTracksToPlaylist(playlistId: Long, trackIds: LongArray) {
        coroutineScope {
            launch {
                val allPlaylists = playlistDao.playlists.first()
                require(allPlaylists.any { it.id == playlistId }) {
                    "No playlist found matching id \"$playlistId\""
                }
            }
            launch {
                val allTracks = trackRepository.tracks.first()
                val allTrackIds = allTracks.mapTo(mutableSetOf(), Track::id)
                require(trackIds.all { it in allTrackIds })
            }
        }

        playlistDao.addTracks(
            trackIds.map { PlaylistTrack(playlistId, trackId = it) }
        )
    }

    /**
     * Deletes an existing playlist.
     * Tracks files that are part of that playlist won't be deleted.
     * Attempting to delete a non-existing playlist will complete with reporting an error.
     *
     * @param playlistId [Playlist.id] of an existing playlist to be deleted.
     */
    suspend fun deletePlaylist(playlistId: Long) {
        val targetPlaylist = playlistDao.playlists.first()
            .find { it.id == playlistId }
            ?: return
        val iconFilename = targetPlaylist.iconUri?.lastPathSegment
        if (iconFilename != null) withContext(dispatchers.IO) {
            File(iconDir.get(), iconFilename).delete()
        }
        playlistDao.deletePlaylist(playlistId)
    }
}