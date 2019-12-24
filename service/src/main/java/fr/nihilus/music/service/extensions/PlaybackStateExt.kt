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

package fr.nihilus.music.service.extensions

import android.support.v4.media.session.PlaybackStateCompat

val PlaybackStateCompat.isPlaying
    get() = (state == PlaybackStateCompat.STATE_BUFFERING) ||
            (state == PlaybackStateCompat.STATE_PLAYING)

val PlaybackStateCompat.isPlayEnabled
    get() = (actions and PlaybackStateCompat.ACTION_PLAY != 0L) ||
            ((actions and PlaybackStateCompat.ACTION_PLAY_PAUSE != 0L) &&
                    (state == PlaybackStateCompat.STATE_PAUSED))

val PlaybackStateCompat.isSkipToNextEnabled
    get() = actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L

val PlaybackStateCompat.isSkipToPreviousEnabled
    get() = actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS != 0L

/**
 * The name of the state code of this playback state.
 */
@Suppress("unused")
val PlaybackStateCompat.stateName: String get() = when (state) {
    PlaybackStateCompat.STATE_NONE -> "NONE"
    PlaybackStateCompat.STATE_STOPPED -> "STOPPED"
    PlaybackStateCompat.STATE_PAUSED -> "PAUSED"
    PlaybackStateCompat.STATE_PLAYING -> "PLAYING"
    PlaybackStateCompat.STATE_FAST_FORWARDING -> "FAST_FORWARDING"
    PlaybackStateCompat.STATE_REWINDING -> "REWINDING"
    PlaybackStateCompat.STATE_BUFFERING -> "BUFFERING"
    PlaybackStateCompat.STATE_ERROR -> "ERROR"
    PlaybackStateCompat.STATE_CONNECTING -> "CONNECTING"
    PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS -> "SKIPPING_TO_PREVIOUS"
    PlaybackStateCompat.STATE_SKIPPING_TO_NEXT -> "SKIPPING_TO_NEXT"
    PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM -> "SKIPPING_TO_QUEUE_ITEM"
    else -> "UNKNOWN"
}