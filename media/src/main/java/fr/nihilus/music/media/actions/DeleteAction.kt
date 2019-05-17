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
import fr.nihilus.music.media.AppDispatchers
import fr.nihilus.music.media.MediaId
import fr.nihilus.music.media.playlists.PlaylistDao
import fr.nihilus.music.media.provider.MediaProvider
import kotlinx.coroutines.withContext

/**
 * An action for deleting media that are available to media browser clients.
 * It is executed with the name [CustomActions.ACTION_DELETE_MEDIA].
 * Media to be deleted with this action are identified by their id.
 *
 * See [CustomActions.ACTION_DELETE_MEDIA] for detailed usage.
 *
 * @constructor
 * @param provider The dao used for deleting tracks.
 * @param playlistDao The dao used for deleting playlists.
 * @param dispatchers Group of dispatchers to use for coroutine execution.
 */
internal class DeleteAction(
    private val provider: MediaProvider,
    private val playlistDao: PlaylistDao,
    private val dispatchers: AppDispatchers
) : BrowserAction {

    override val name: String
        get() = CustomActions.ACTION_DELETE_MEDIA

    override suspend fun execute(parameters: Bundle?): Bundle? {
        val deletedMediaIds = parameters?.getStringArray(CustomActions.EXTRA_MEDIA_IDS)?.map {
            MediaId.parseOrNull(it) ?: throw ActionFailure(
                CustomActions.ERROR_CODE_PARAMETER,
                "Invalid media id: $it"
            )

        } ?: throw ActionFailure(
            CustomActions.ERROR_CODE_PARAMETER,
            "Missing parameter ${CustomActions.EXTRA_MEDIA_IDS}"
        )

        val deletedTrackIds = mutableListOf<Long>()
        val deletedPlaylistIds = mutableListOf<Long>()
        val unsupportedMediaIds = mutableListOf<MediaId>()

        for (media in deletedMediaIds) {
            when {
                // Track from CATEGORY ALL
                media.type == MediaId.TYPE_TRACKS
                        && media.category == MediaId.CATEGORY_ALL
                        && media.track != null -> {
                    deletedTrackIds += media.track.toLong()
                }
                // Playlist
                media.type == MediaId.TYPE_PLAYLISTS
                        && media.category != null
                        && media.track == null -> {
                    deletedPlaylistIds += media.category.toLong()
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

        val deletedTrackCount = if (deletedTrackIds.isNotEmpty()) {
            withContext(dispatchers.IO) {
                val trackIds = LongArray(deletedTrackIds.size) { deletedTrackIds[it] }
                provider.deleteTracks(trackIds)
            }
        } else 0

        if (deletedPlaylistIds.isNotEmpty()) {
            withContext(dispatchers.Database) {
                deletedPlaylistIds.forEach { playlistId ->
                    playlistDao.deletePlaylist(playlistId)
                }
            }
        }

        return Bundle(1).apply {
            putInt(CustomActions.RESULT_TRACK_COUNT, deletedTrackCount)
        }
    }
}