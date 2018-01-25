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
import fr.nihilus.music.BuildConfig
import fr.nihilus.music.di.ServiceScoped
import fr.nihilus.music.media.source.MusicDao
import fr.nihilus.music.service.MusicService
import fr.nihilus.music.utils.MediaID
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

@ServiceScoped
class DeleteTracksCommand
@Inject internal constructor(
    private val service: MusicService,
    private val musicDao: MusicDao
) : MediaSessionCommand {

    override fun handle(params: Bundle?, cb: ResultReceiver?) {
        val idsToDelete = params?.getLongArray(PARAM_TRACK_IDS)
                ?: throw IllegalArgumentException("Required parameter: PARAM_TRACK_IDS")

        Observable.fromIterable(idsToDelete.toSet())
            .subscribeOn(Schedulers.io())
            .flatMapCompletable { musicId -> musicDao.deleteTrack(musicId.toString()) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { onSuccess(cb) },
                { error ->
                    if (BuildConfig.DEBUG) {
                        // Rethrow unexpected errors on debug builds
                        throw error
                    }
                })
    }

    private fun onSuccess(cb: ResultReceiver?) {
        service.notifyChildrenChanged(MediaID.ID_MUSIC)
        cb?.send(MediaSessionCommand.CODE_SUCCESS, null)
    }

    companion object {

        /**
         * The name of this command.
         *
         * Required parameters:
         * - [PARAM_TRACK_IDS]
         */
        const val CMD_NAME = "fr.nihilus.music.command.DeleteTrackCommand"

        /**
         * An array containing the music ids of the tracks to delete.
         *
         * Type: long array
         */
        const val PARAM_TRACK_IDS = "track_ids"
    }
}