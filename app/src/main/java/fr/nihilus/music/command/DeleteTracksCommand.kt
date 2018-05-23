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
import fr.nihilus.music.di.ServiceScoped
import fr.nihilus.music.media.source.MusicDao
import timber.log.Timber
import javax.inject.Inject

@ServiceScoped
class DeleteTracksCommand
@Inject internal constructor(
    private val musicDao: MusicDao
) : MediaSessionCommand {

    override fun handle(params: Bundle?, cb: ResultReceiver?) {
        val idsToDelete = requireNotNull(params?.getLongArray(PARAM_TRACK_IDS)) {
            "Required parameter: PARAM_TRACK_IDS"
        }

        musicDao.deleteTracks(idsToDelete).subscribe(
            { onSuccess(cb, it) },
            { Timber.e(it, "Unexpected error while deleting tracks with ids: $idsToDelete") }
        )
    }

    private fun onSuccess(cb: ResultReceiver?, deleteCount: Int) {
        cb?.send(R.id.result_success, Bundle(1).apply {
            putInt(RESULT_DELETE_COUNT, deleteCount)
        })
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

        const val RESULT_DELETE_COUNT = "delete_count"
    }
}