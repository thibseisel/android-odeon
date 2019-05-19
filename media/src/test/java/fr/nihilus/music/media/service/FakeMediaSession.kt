/*
 * Copyright 2019 Thibault Seisel
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

package fr.nihilus.music.media.service

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

@ExperimentalMediaApi
internal class FakeMediaSession : MediaSession {

    var flags: Int = 0
        private set

    var metadata: MediaMetadataCompat? = null
        private set

    var playbackState: PlaybackStateCompat? = null
        private set

    @get:PlaybackStateCompat.RepeatMode
    var repeatMode: Int = PlaybackStateCompat.REPEAT_MODE_INVALID
        private set

    @get:PlaybackStateCompat.ShuffleMode
    var shuffleMode: Int = PlaybackStateCompat.SHUFFLE_MODE_INVALID
        private set

    var queueTitle: CharSequence? = null
        private set

    var queue: List<MediaSessionCompat.QueueItem>? = null
        private set

    var isReleased: Boolean = false
        private set

    override val token: MediaSessionCompat.Token
        get() = throw UnsupportedOperationException("Test double cannot create session tokens")

    override var isActive: Boolean = false

    override fun setFlags(flags: Int) {
        this.flags = flags
    }

    override fun setMetadata(metadata: MediaMetadataCompat?) {
        this.metadata = metadata
    }

    override fun setPlaybackState(state: PlaybackStateCompat?) {
        this.playbackState = state
    }

    override fun setRepeatMode(mode: Int) {
        this.repeatMode = repeatMode
    }

    override fun setShuffleMode(mode: Int) {
        this.shuffleMode = shuffleMode
    }

    override fun setQueueTitle(title: CharSequence?) {
        this.queueTitle = title
    }

    override fun setQueue(playQueue: List<MediaSessionCompat.QueueItem>?) {
        this.queue = playQueue
    }

    override fun release() {
        isActive = false
        isReleased = true
    }
}