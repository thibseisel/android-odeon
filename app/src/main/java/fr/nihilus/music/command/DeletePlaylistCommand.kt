/*
 * Copyright 2017 Thibault Seisel
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
import android.util.Log
import fr.nihilus.music.database.PlaylistDao
import fr.nihilus.music.di.ServiceScoped
import fr.nihilus.music.service.MusicService
import fr.nihilus.music.utils.MediaID
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

@ServiceScoped
class DeletePlaylistCommand
@Inject internal constructor(
        private val service: MusicService,
        private val playlistDao: PlaylistDao
) : MediaSessionCommand {

    override fun handle(params: Bundle?, cb: ResultReceiver?) {
        params ?: throw IllegalArgumentException("This command should have parameters")
        val playlistId = params.getLong(PARAM_PLAYLIST_ID, -1L)
        require (playlistId != -1L) { "Missing parameter: PARAM_PLAYLIST_ID" }

        Single.fromCallable { playlistDao.deletePlaylist(playlistId) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    service.notifyChildrenChanged(MediaID.ID_PLAYLISTS)
                    cb?.send(MediaSessionCommand.CODE_SUCCESS, null)
                }, { error ->
                    Log.e(TAG, "Unexpected error while deleting playlist", error)
                    cb?.send(MediaSessionCommand.CODE_UNEXPECTED_ERROR, null)
                })
    }

    companion object {
        private const val TAG = "DeletePlaylistCmd"

        /**
         * The name of this command.
         *
         * Required parameters:
         * - [PARAM_PLAYLIST_ID]
         */
        const val CMD_NAME = "fr.nihilus.music.command.DeletePlaylistCommand"
        const val PARAM_PLAYLIST_ID = "playlist_id"
    }
}