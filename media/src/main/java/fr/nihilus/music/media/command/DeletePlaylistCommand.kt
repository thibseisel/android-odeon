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

package fr.nihilus.music.media.command

import android.os.Bundle
import android.os.ResultReceiver
import fr.nihilus.music.media.BuildConfig
import fr.nihilus.music.media.CATEGORY_PLAYLISTS
import fr.nihilus.music.media.R
import fr.nihilus.music.media.di.ServiceScoped
import fr.nihilus.music.media.playlists.PlaylistDao
import fr.nihilus.music.media.service.MusicService
import fr.nihilus.music.media.utils.plusAssign
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

@ServiceScoped
class DeletePlaylistCommand
@Inject internal constructor(
    private val service: MusicService,
    private val playlistDao: PlaylistDao,
    private val subscriptions: CompositeDisposable
) : MediaSessionCommand {

    override fun handle(params: Bundle?, cb: ResultReceiver?) {
        params ?: throw IllegalArgumentException("This command should have parameters")

        require(params.containsKey(PARAM_PLAYLIST_ID))
        val playlistId = params.getLong(PARAM_PLAYLIST_ID)

        subscriptions += Single.fromCallable { playlistDao.deletePlaylist(playlistId) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    service.notifyChildrenChanged(CATEGORY_PLAYLISTS)
                    cb?.send(R.id.abc_result_success, null)
                },
                { error ->
                    if (BuildConfig.DEBUG) {
                        // Rethrow unexpected errors on debug builds
                        throw error
                    }
                }
            )
    }

    companion object {

        /**
         * The name of this command.
         *
         * Required parameters:
         * - [PARAM_PLAYLIST_ID]
         */
        const val CMD_NAME = "fr.nihilus.music.command.DeletePlaylistCommand"

        /**
         * The unique identifier of the playlist to delete.
         *
         * Type: Long
         */
        const val PARAM_PLAYLIST_ID = "playlist_id"
    }
}