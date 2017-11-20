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
import android.support.v4.media.session.PlaybackStateCompat

interface MusicPlayer {
    var callback: Callback?

    /**
     * The ID of the currently playing media.
     */
    var currentMediaId: String?
    /**
     * The current [android.media.session.PlaybackState.getState]
     */
    @PlaybackStateCompat.State
    val state: Int
    /**
     * Indicate whether the player is playing or is supposed to be
     * playing when we gain audio focus.
     */
    val isPlaying: Boolean

    /**
     * The current position in the audio stream in milliseconds.
     */
    val currentPosition: Long

    /**
     * Start playback for an [item] in the queue.
     * If the media id of this item is the same as [currentMediaId], then the playback is resumed.
     * Otherwise, the specified [item] will be played instead.
     *
     * Playback will begin only if the application has audio focus.
     */
    fun play(item: MediaSessionCompat.QueueItem)

    /**
     * Pause the player.
     * If you call this method during media buffering,
     * playback will not resume when the loading is complete.
     *
     * Listeners are notified when playback state is updated.
     */
    fun pause()

    /**
     * Seek to the specified position in the currently playing media.
     * When playing, playback will resume at this position.
     * @param position in the currently playing media in milliseconds
     */
    fun seekTo(position: Long)

    /**
     * Stop the player. All resources are de-allocated.
     */
    fun stop()

    /**
     * Propagates events produced by a MusicPlayer instance.
     */
    interface Callback {

        /**
         * Called when the playback status has changed.
         * Use this callback to update playback state on the media session.
         */
        fun onPlaybackStatusChanged(@PlaybackStateCompat.State newState: Int)

        /** Called when the currently playing music has completed. */
        fun onCompletion()

        /**
         * Called when an message occur during media playback.
         * @param message describing the error to be added to the PlaybackState
         */
        fun onError(message: String)
    }
}