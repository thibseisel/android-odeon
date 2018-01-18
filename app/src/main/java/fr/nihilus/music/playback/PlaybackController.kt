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

import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.DefaultPlaybackController
import fr.nihilus.music.di.ServiceScoped
import fr.nihilus.music.settings.PreferenceDao
import javax.inject.Inject

/**
 * An extension of DefaultPlaybackController that saves repeat mode and shuffle mode
 * to shared preferences when any of them changes.
 */
@ServiceScoped
class PlaybackController
@Inject constructor(
    private val prefs: PreferenceDao
) : DefaultPlaybackController() {

    override fun onSetShuffleMode(player: Player?, shuffleMode: Int) {
        super.onSetShuffleMode(player, shuffleMode)

        // Save last shuffle mode to preferences
        prefs.shuffleMode = shuffleMode
    }

    override fun onSetRepeatMode(player: Player?, repeatMode: Int) {
        super.onSetRepeatMode(player, repeatMode)

        // Save last repeat mode to preferences
        prefs.repeatMode = repeatMode
    }

    /**
     * Restore player and media session states from shared preferences.
     * This includes the last used shuffle mode and repeat mode.
     *
     * @param player The player whose state is to be restored
     * @param mediaSession The media session whose state is to be restored
     */
    fun restoreStateFromPreferences(player: Player, mediaSession: MediaSessionCompat) {
        val savedShuffleMode = prefs.shuffleMode
        super.onSetShuffleMode(player, savedShuffleMode)
        mediaSession.setShuffleMode(savedShuffleMode)

        val savedRepeatMode = prefs.repeatMode
        super.onSetRepeatMode(player, savedRepeatMode)
        mediaSession.setRepeatMode(savedRepeatMode)
    }
}