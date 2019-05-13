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

package fr.nihilus.music.media.actions

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import com.github.thibseisel.kdenticon.Identicon
import com.github.thibseisel.kdenticon.IdenticonStyle
import com.github.thibseisel.kdenticon.android.drawToBitmap
import fr.nihilus.music.media.AppDispatchers
import fr.nihilus.music.media.MediaId
import fr.nihilus.music.media.MediaId.Builder.TYPE_PLAYLISTS
import fr.nihilus.music.media.os.FileSystem
import fr.nihilus.music.media.playlists.Playlist
import fr.nihilus.music.media.playlists.PlaylistDao
import fr.nihilus.music.media.playlists.PlaylistTrack
import kotlinx.coroutines.withContext

private const val IDENTICONS_FOLDER = "identicons"

internal class ManagePlaylistAction(
    private val playlistDao: PlaylistDao,
    private val files: FileSystem,
    private val dispatchers: AppDispatchers
) : BrowserAction {
    override suspend fun execute(parameters: Bundle?): Bundle? {
        when {
            parameters == null -> failDueToMissingParameters()
            parameters.containsKey(CustomActions.EXTRA_PLAYLIST_ID) -> {
                // Edit mode
                return executeAddTracks(
                    encodedPlaylistId = parameters.getString(CustomActions.EXTRA_PLAYLIST_ID),
                    encodedTrackIds = parameters.getStringArray(CustomActions.EXTRA_MEDIA_IDS)
                )
            }
            parameters.containsKey(CustomActions.EXTRA_TITLE) -> {
                // Create mode
                return executeCreatePlaylist(
                    title = parameters.getString(CustomActions.EXTRA_TITLE),
                    encodedTrackIds = parameters.getStringArray(CustomActions.EXTRA_MEDIA_IDS)
                )
            }
            else -> failDueToMissingParameters()
        }
    }

    private suspend fun executeCreatePlaylist(
        title: String?,
        encodedTrackIds: Array<String>?
    ): Bundle? {
        if (title == null) throw ActionFailure(
            CustomActions.ERROR_CODE_PARAMETER,
            "Missing parameter: ${CustomActions.EXTRA_TITLE}"
        )

        val iconBitmap = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)
        Identicon.fromValue(title, 320).apply {
            style = IdenticonStyle(
                backgroundColor = Color.TRANSPARENT,
                padding = 0f
            )
        }.drawToBitmap(iconBitmap)

        val playlistIconUri: Uri? = withContext(dispatchers.IO) {
            // Remove spaces from the seed to make a filename without spaces
            val fileName = title.replace("\\s".toRegex(), "_")
            files.writeBitmapToInternalStorage("$IDENTICONS_FOLDER/$fileName.png", iconBitmap)
        }

        val trackIds = if (encodedTrackIds != null) LongArray(encodedTrackIds.size) { index ->
            val encodedId = encodedTrackIds[index]
            extractTrackIdFrom(encodedId)
        } else LongArray(0)

        withContext(dispatchers.Database) {
            val newPlaylistId = playlistDao.savePlaylist(
                Playlist(title, playlistIconUri)
            )

            val newPlaylistTracks = trackIds.map { trackId ->
                PlaylistTrack(newPlaylistId, trackId)
            }

            if (newPlaylistTracks.isNotEmpty()) {
                playlistDao.addTracks(newPlaylistTracks)
            }
        }

        return null
    }

    private suspend fun executeAddTracks(
        encodedPlaylistId: String?,
        encodedTrackIds: Array<String>?
    ): Bundle? {
        val playlistId = MediaId.parseOrNull(encodedPlaylistId)
            ?.takeIf { it.type == TYPE_PLAYLISTS && it.category != null && it.track == null }
            ?.category?.toLongOrNull()
            ?: throw ActionFailure(
                CustomActions.ERROR_CODE_UNSUPPORTED_MEDIA_ID,
                "$encodedPlaylistId is not a valid playlist media id."
            )

        val playlistTracks = encodedTrackIds?.map { encodedId ->
            val trackId = extractTrackIdFrom(encodedId)
            PlaylistTrack(playlistId, trackId)
        } ?: throw ActionFailure(
            CustomActions.ERROR_CODE_PARAMETER,
            "You should specify the media ids of tracks to add to this playlist"
        )

        if (!playlistTracks.isNullOrEmpty()) {
            withContext(dispatchers.Database) {
                playlistDao.addTracks(playlistTracks)
            }
        }

        return null
    }

    private fun extractTrackIdFrom(encodedId: String): Long {
        return MediaId.parseOrNull(encodedId)?.track
            ?: throw ActionFailure(
                CustomActions.ERROR_CODE_UNSUPPORTED_MEDIA_ID,
                "$encodedId is not a valid track media id."
            )
    }

    private fun failDueToMissingParameters(): Nothing {
        throw ActionFailure(
            CustomActions.ERROR_CODE_PARAMETER,
            "Executing this action required at least the following parameters: " +
                    "${CustomActions.EXTRA_TITLE} (creating a playlist), or " +
                    "${CustomActions.EXTRA_PLAYLIST_ID} and ${CustomActions.EXTRA_MEDIA_IDS} (adding tracks t a playlist)"
        )
    }
}