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
import android.net.Uri
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Pair
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException
import com.google.android.exoplayer2.util.ErrorMessageProvider
import fr.nihilus.music.service.R
import fr.nihilus.music.service.ServiceScoped
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

@ServiceScoped
internal class ErrorHandler @Inject constructor(
    private val context: Context
) : ErrorMessageProvider<ExoPlaybackException> {

    override fun getErrorMessage(playbackException: ExoPlaybackException?): Pair<Int, String>? =
        when (playbackException?.type) {
            ExoPlaybackException.TYPE_SOURCE -> handleSourceError(playbackException.sourceException)
            ExoPlaybackException.TYPE_RENDERER -> handleUnexpectedError(playbackException.rendererException)
            ExoPlaybackException.TYPE_UNEXPECTED -> handleUnexpectedError(playbackException.unexpectedException)
            else -> handleUnexpectedError(null)
        }

    private fun handleSourceError(cause: IOException): Pair<Int, String>? = when (cause) {
        is UnrecognizedInputFormatException -> handleUnrecognizedFormat(cause.uri)
        else -> Pair(
            PlaybackStateCompat.ERROR_CODE_ACTION_ABORTED,
            context.getString(R.string.svc_player_source_error_generic)
        )
    }

    private fun handleUnrecognizedFormat(failingUri: Uri?): Pair<Int, String> {
        if (failingUri != null && failingUri.isHierarchical) {
            failingUri.path?.substringAfterLast('.')?.let { fileExtension ->
                return Pair(
                    PlaybackStateCompat.ERROR_CODE_NOT_SUPPORTED,
                    context.getString(R.string.svc_player_error_unsupported_file_format, fileExtension)
                )
            }
        }

        return Pair(
            PlaybackStateCompat.ERROR_CODE_NOT_SUPPORTED,
            context.getString(R.string.svc_player_error_unsupported_file)
        )
    }

    private fun handleUnexpectedError(cause: Exception?): Pair<Int, String>? {
        Timber.e(cause, "Unexpected player error.")
        return Pair(
            PlaybackStateCompat.ERROR_CODE_UNKNOWN_ERROR,
            context.getString(R.string.svc_player_unknown_error)
        )
    }
}