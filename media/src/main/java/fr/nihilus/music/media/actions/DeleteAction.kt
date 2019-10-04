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

import android.os.Bundle
import fr.nihilus.music.common.media.CustomActions
import fr.nihilus.music.common.media.InvalidMediaException
import fr.nihilus.music.common.media.MediaId
import fr.nihilus.music.common.media.toMediaId
import fr.nihilus.music.common.os.PermissionDeniedException
import fr.nihilus.music.media.di.ServiceScoped
import fr.nihilus.music.database.playlists.PlaylistDao
import fr.nihilus.music.media.provider.MediaDao
import javax.inject.Inject

/**
 * An action for deleting media that are available to media browser clients.
 * It is executed with the name [CustomActions.ACTION_DELETE_MEDIA].
 * Media to be deleted with this action are identified by their id.
 *
 * See [CustomActions.ACTION_DELETE_MEDIA] for detailed usage.
 *
 * @constructor
 * @param dao The dao used for deleting tracks.
 * @param playlistDao The dao used for deleting playlists.
 */
@ServiceScoped
internal class DeleteAction
@Inject constructor(
    private val dao: MediaDao,
    private val playlistDao: PlaylistDao
) : BrowserAction {

    override val name: String
        get() = CustomActions.ACTION_DELETE_MEDIA

    override suspend fun execute(parameters: Bundle?): Bundle? {
        val deletedMediaIds = try {
            parameters?.getStringArray(CustomActions.EXTRA_MEDIA_IDS)
                ?.map(String::toMediaId)
                ?: throw ActionFailure(
                    CustomActions.ERROR_CODE_PARAMETER,
                    "Missing parameter ${CustomActions.EXTRA_MEDIA_IDS}"
                )

        } catch (ime: InvalidMediaException) {
            throw ActionFailure(CustomActions.ERROR_CODE_PARAMETER, ime.message)
        }

        val deletedTrackIds = mutableListOf<Long>()
        val deletedPlaylistIds = mutableListOf<Long>()
        val unsupportedMediaIds = mutableListOf<MediaId>()

        for (media in deletedMediaIds) {
            when {
                // Track from CATEGORY ALL or CATEGORY_DISPOSABLE
                media.type == MediaId.TYPE_TRACKS
                        && (media.category == MediaId.CATEGORY_ALL || media.category == MediaId.CATEGORY_DISPOSABLE)
                        && media.track != null -> {
                    deletedTrackIds += media.track!!.toLong()
                }
                // Playlist
                media.type == MediaId.TYPE_PLAYLISTS
                        && media.category != null
                        && media.track == null -> {
                    deletedPlaylistIds += media.category!!.toLong()
                }
                else -> unsupportedMediaIds += media
            }
        }

        if (unsupportedMediaIds.isNotEmpty()) {
            throw ActionFailure(
                CustomActions.ERROR_CODE_UNSUPPORTED_MEDIA_ID,
                "Cannot delete media with ids: $unsupportedMediaIds"
            )
        }

        // Proceed with deleting playlists, if any.
        if (deletedPlaylistIds.isNotEmpty()) {
            deletedPlaylistIds.forEach { playlistId ->
                playlistDao.deletePlaylist(playlistId)
            }
        }

        try {
            // Proceed with deleting tracks, if any.
            val deletedTrackCount = if (deletedTrackIds.isEmpty()) 0 else {
                val trackIds = LongArray(deletedTrackIds.size) { deletedTrackIds[it] }
                dao.deleteTracks(trackIds)
            }

            return Bundle(1).apply {
                putInt(CustomActions.RESULT_TRACK_COUNT, deletedTrackCount)
            }

        } catch (pde: PermissionDeniedException) {
            // Permission to write to storage is not granted.
            throw ActionFailure(CustomActions.ERROR_CODE_PERMISSION_DENIED, pde.permission)
        }
    }
}