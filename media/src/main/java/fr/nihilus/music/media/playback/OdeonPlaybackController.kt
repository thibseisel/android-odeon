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

package fr.nihilus.music.media.playback

import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.DefaultPlaybackController
import fr.nihilus.music.media.di.ServiceScoped
import javax.inject.Inject

/**
 * A custom [DefaultPlaybackController] that allows setting shuffle mode and repeat mode
 * even if the current timeline is empty.
 */
@ServiceScoped
class OdeonPlaybackController
@Inject constructor() : DefaultPlaybackController() {

    override fun getSupportedPlaybackActions(player: Player?): Long =
        PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE or
                PlaybackStateCompat.ACTION_SET_REPEAT_MODE or
                super.getSupportedPlaybackActions(player)
}