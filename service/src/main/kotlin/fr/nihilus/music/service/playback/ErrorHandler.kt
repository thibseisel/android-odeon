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

package fr.nihilus.music.service.playback

import android.content.Context
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Pair
import androidx.media3.common.ErrorMessageProvider
import androidx.media3.common.PlaybackException
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import fr.nihilus.music.service.R
import timber.log.Timber
import javax.inject.Inject

@ServiceScoped
internal class ErrorHandler @Inject constructor(
    @ApplicationContext private val context: Context
) : ErrorMessageProvider<PlaybackException> {

    override fun getErrorMessage(playbackException: PlaybackException): Pair<Int, String> =
        when (playbackException.errorCode) {
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> handleUnrecognizedFormat()
            PlaybackException.ERROR_CODE_DECODING_FAILED -> handleCorruptionError()
            else -> handleUnexpectedError(playbackException)
        }

    private fun handleCorruptionError() = Pair(
        PlaybackStateCompat.ERROR_CODE_ACTION_ABORTED,
        context.getString(R.string.svc_player_source_error_generic)
    )

    private fun handleUnrecognizedFormat(): Pair<Int, String> = Pair(
        PlaybackStateCompat.ERROR_CODE_NOT_SUPPORTED,
        context.getString(R.string.svc_player_error_unsupported_file)
    )

    private fun handleUnexpectedError(cause: Exception?): Pair<Int, String> {
        Timber.e(cause, "Unexpected player error.")
        return Pair(
            PlaybackStateCompat.ERROR_CODE_UNKNOWN_ERROR,
            context.getString(R.string.svc_player_unknown_error)
        )
    }
}
