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

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import fr.nihilus.music.core.playback.RepeatMode

internal interface MediaSession {
    val isActive: Boolean
    fun setMetadata(metadata: MediaMetadataCompat?)
    fun setPlaybackState(state: PlaybackStateCompat)
    fun setQueue(tracks: List<MediaDescriptionCompat>?)
    fun setQueueTitle(title: CharSequence?)
    fun setRepeatMode(mode: RepeatMode)
    fun setShuffleModeEnabled(shouldShuffle: Boolean)
    fun release()
    fun setCallback(callback: Callback)

    interface Callback {
        fun onPlay()
        fun onPause()
        fun onPlayFromMediaId(mediaId: String?, extras: Bundle?)
        fun onPlayFromSearch(query: String?, extras: Bundle?)
        fun onPrepare()
        fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?)
        fun onPrepareFromSearch(query: String?, extras: Bundle?)
        fun onSeekTo(position: Long)
        fun onSetRepeatMode(mode: RepeatMode)
        fun onSetShuffleMode(enabled: Boolean)
        fun onSkipBackward()
        fun onSkipForward()
        fun onSkipToQueueItem(queueId: Long)
        fun onStop()
    }
}

internal class MediaSessionCompatWrapper(
    context: Context
) : MediaSession {

    private val session = MediaSessionCompat(context, "MusicService").also {
        it.setRatingType(RatingCompat.RATING_NONE)
    }

    override var isActive: Boolean
        get() = session.isActive
        private set(value) {
            if (session.isActive != value) {
                session.isActive = value
            }
        }

    override fun setMetadata(metadata: MediaMetadataCompat?) {
        session.setMetadata(metadata)
    }

    override fun setPlaybackState(state: PlaybackStateCompat) {
        session.setPlaybackState(state)

        when (state.state) {
            PlaybackStateCompat.STATE_PLAYING -> isActive = true
            PlaybackStateCompat.STATE_NONE,
            PlaybackStateCompat.STATE_STOPPED,
            PlaybackStateCompat.STATE_ERROR -> isActive = false
        }
    }

    override fun setQueue(tracks: List<MediaDescriptionCompat>?) {
        val queueItems = tracks?.mapIndexed { index, it ->
            MediaSessionCompat.QueueItem(it, index.toLong())
        }
        session.setQueue(queueItems)
    }

    override fun setQueueTitle(title: CharSequence?) {
        session.setQueueTitle(title)
    }

    override fun setRepeatMode(mode: RepeatMode) {
        session.setRepeatMode(
            when (mode) {
                RepeatMode.ALL -> PlaybackStateCompat.REPEAT_MODE_ALL
                RepeatMode.ONE -> PlaybackStateCompat.REPEAT_MODE_ONE
                else -> PlaybackStateCompat.REPEAT_MODE_NONE
            }
        )
    }

    override fun setShuffleModeEnabled(shouldShuffle: Boolean) {
        session.setShuffleMode(
            when {
                shouldShuffle -> PlaybackStateCompat.SHUFFLE_MODE_ALL
                else -> PlaybackStateCompat.SHUFFLE_MODE_NONE
            }
        )
    }

    override fun release() {
        session.release()
    }

    override fun setCallback(callback: MediaSession.Callback) {
        session.setCallback(SessionCompatCallbackAdapter(callback))
    }

    private class SessionCompatCallbackAdapter(
        private val callback: MediaSession.Callback
    ) : MediaSessionCompat.Callback() {

        override fun onPlay() {
            callback.onPlay()
        }

        override fun onPause() {
            callback.onPause()
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            callback.onPlayFromMediaId(mediaId, extras)
        }

        override fun onPlayFromSearch(query: String?, extras: Bundle?) {
            callback.onPlayFromSearch(query, extras)
        }

        override fun onPrepare() {
            callback.onPrepare()
        }

        override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
            callback.onPrepareFromMediaId(mediaId, extras)
        }

        override fun onPrepareFromSearch(query: String?, extras: Bundle?) {
            callback.onPrepareFromSearch(query, extras)
        }

        override fun onSeekTo(pos: Long) {
            callback.onSeekTo(pos)
        }

        override fun onSkipToPrevious() {
            callback.onSkipBackward()
        }

        override fun onSkipToNext() {
            callback.onSkipForward()
        }

        override fun onSkipToQueueItem(id: Long) {
            callback.onSkipToQueueItem(id)
        }

        override fun onSetRepeatMode(repeatMode: Int) {
            callback.onSetRepeatMode(
                when (repeatMode) {
                    PlaybackStateCompat.REPEAT_MODE_ALL,
                    PlaybackStateCompat.REPEAT_MODE_GROUP -> RepeatMode.ALL
                    PlaybackStateCompat.REPEAT_MODE_ONE -> RepeatMode.ONE
                    else -> RepeatMode.DISABLED
                }
            )
        }

        override fun onSetShuffleMode(shuffleMode: Int) {
            callback.onSetShuffleMode(
                shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL ||
                        shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_GROUP
            )
        }

        override fun onStop() {
            callback.onStop()
        }
    }
}