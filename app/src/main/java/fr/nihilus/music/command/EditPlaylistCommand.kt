/*
 * Copyright 2018 Thibault Seisel
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

package fr.nihilus.music.command

import android.os.Bundle
import android.os.ResultReceiver
import fr.nihilus.music.R
import fr.nihilus.music.database.PlaylistDao
import fr.nihilus.music.database.PlaylistTrack
import fr.nihilus.music.service.MusicService
import fr.nihilus.music.utils.MediaID
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class EditPlaylistCommand
@Inject internal constructor(
    private val service: MusicService,
    private val playlistDao: PlaylistDao
) : MediaSessionCommand {

    companion object {

        /**
         * The name to use to call this command from MediaBrowserCompat.
         *
         * Required parameters :
         * - [PARAM_PLAYLIST_ID]
         * - [PARAM_NEW_TRACKS]
         */
        const val CMD_NAME = "fr.nihilus.music.command.EditPlaylistCommand"

        /**
         * The unique id of the existing user-defined playlist to change.
         *
         * If no existing playlist has this id, [CODE_ERROR_PLAYLIST_NOT_EXISTS] will be returned.
         */
        const val PARAM_PLAYLIST_ID = "playlist_id"

        /**
         * An array of Long containing all music ids of tracks
         * to add to the newly created playlist.
         *
         * If not specified, no track will be added to the playlist.
         */
        const val PARAM_NEW_TRACKS = "added_track_ids"

        /**
         * An error code that indicates that there is no user-defined playlist whose unique id
         * matches the one provided by [PARAM_PLAYLIST_ID].
         */
        const val CODE_ERROR_PLAYLIST_NOT_EXISTS = -3
    }

    override fun handle(params: Bundle?, cb: ResultReceiver?) {
        params ?: throw IllegalArgumentException("This command should have parameters")
        val playlistId = params.getLong(PARAM_PLAYLIST_ID, -1L)
        val trackIds = params.getLongArray(PARAM_NEW_TRACKS) ?: LongArray(0)

        if (playlistId == -1L) {
            throw IllegalArgumentException("Missing parameter: PARAM_PLAYLIST_ID")
        }

        val tracks = trackIds.map { musicId -> PlaylistTrack(playlistId, musicId) }

        Single.fromCallable { playlistDao.addTracks(tracks) }
            .subscribeOn(Schedulers.io())
            .doOnSuccess { service.notifyChildrenChanged(MediaID.ID_PLAYLISTS) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { _, error ->
                if (error != null) {
                    Timber.e(error, "Unexpected error: %s", error.message)
                    cb?.send(CODE_ERROR_PLAYLIST_NOT_EXISTS, null)
                } else {
                    cb?.send(R.id.result_success, null)
                }
            }
    }
}