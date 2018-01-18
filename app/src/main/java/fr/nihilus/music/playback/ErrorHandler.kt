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

package fr.nihilus.music.playback

import android.content.Context
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Pair
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.util.ErrorMessageProvider
import fr.nihilus.music.R
import fr.nihilus.music.di.ServiceScoped
import fr.nihilus.music.to
import javax.inject.Inject

@ServiceScoped
class ErrorHandler
@Inject constructor(
    private val context: Context
) : ErrorMessageProvider<ExoPlaybackException> {

    override fun getErrorMessage(playbackException: ExoPlaybackException?): Pair<Int, String>? {
        val unknownError = context.getString(R.string.player_unknown_error)
        if (playbackException == null) {
            return PlaybackStateCompat.ERROR_CODE_UNKNOWN_ERROR to unknownError
        }

        // TODO Error description should be more precise
        return PlaybackStateCompat.ERROR_CODE_APP_ERROR to unknownError
    }
}