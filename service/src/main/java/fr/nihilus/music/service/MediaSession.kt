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

package fr.nihilus.music.service

import android.support.v4.media.session.PlaybackStateCompat
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.playback.RepeatMode
import fr.nihilus.music.service.browser.SearchQuery

/**
 * A Media Session is responsive for the communication with the player.
 * The session maintains a representation of the player's state and information
 * about what is playing.
 *
 * A session can receive callbacks from one or more media controllers.
 * This makes it possible for the player to be controller by the app's UI
 * as well as companion devices running Android Wear or Android Auto.
 */
internal interface MediaSession {

    /**
     * The current active state of this session.
     * An active session receives events sent by the Android system when hardware buttons
     * (such as the play/pause button on wired and bluetooth headsets) are pressed.
     *
     * The Android system may send incoming key events to an inactive media session
     * in an attempt to restart it. This only happens if:
     * - no other media session is active,
     * - the event has not been handled by the current foreground activity,
     * - that media session has been the last active session.
     */
    val isActive: Boolean

    /**
     * Sets the currently selected track for this session.
     * When specified, that track album artwork will be displayed on the lock screen.
     *
     * @param track The currently selected media or `null` if no media is set to play.
     */
    fun setCurrentMedia(track: AudioTrack?)
    fun setPlaybackState(state: PlaybackStateCompat)
    fun setQueue(tracks: List<AudioTrack>?)
    fun setQueueTitle(title: CharSequence?)

    /**
     * Sets the repeat mode for this session.
     * This defaults to [RepeatMode.DISABLED] if not set.
     *
     * @param mode The current repeat mode.
     */
    fun setRepeatMode(mode: RepeatMode)

    /**
     * Sets whether tracks are played in shuffle order for this session.
     * This defaults to `false` if not set.
     *
     * @param shouldShuffle Whether shuffle mode is enabled.
     */
    fun setShuffleModeEnabled(shouldShuffle: Boolean)
    fun setCallback(callback: Callback)

    interface Callback {
        fun onPlay()
        fun onPause()
        fun onPlayFromMediaId(mediaId: MediaId)
        fun onPlayFromSearch(query: SearchQuery)
        fun onPrepare()
        fun onPrepareFromMediaId(mediaId: MediaId)
        fun onPrepareFromSearch(query: SearchQuery)
        fun onSeekTo(position: Long)
        fun onSetRepeatMode(mode: RepeatMode)
        fun onSetShuffleMode(enabled: Boolean)
        fun onSkipBackward()
        fun onSkipForward()
        fun onSkipToQueueItem(queueId: Long)
        fun onStop()
    }
}