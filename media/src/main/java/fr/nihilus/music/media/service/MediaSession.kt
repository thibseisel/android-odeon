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

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

@ExperimentalMediaApi
internal interface MediaSession {
    val token: MediaSessionCompat.Token
    var isActive: Boolean
    fun setFlags(flags: Int)
    fun setMetadata(metadata: MediaMetadataCompat?)
    fun setPlaybackState(state: PlaybackStateCompat?)
    fun setRepeatMode(@PlaybackStateCompat.RepeatMode mode: Int)
    fun setShuffleMode(@PlaybackStateCompat.ShuffleMode mode: Int)
    fun setQueueTitle(title: CharSequence?)
    fun setQueue(playQueue: List<MediaSessionCompat.QueueItem>?)
    fun release()

    @ExperimentalMediaApi
    interface Callback {
        fun onPlay() = Unit
        fun onPause() = Unit
        fun onStop() = Unit
        fun onPrepare() = Unit
        fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) = Unit
        fun onPrepareFromSearch(query: String?, extras: Bundle?) = Unit
        fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) = Unit
        fun onPlayFromSearch(query: String?, extras: Bundle?) = Unit
        fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) = Unit
        fun onSeekTo(positionMillis: Long) = Unit
        fun onRewind() = Unit
        fun onFastForward() = Unit
        fun onSetRepeatMode(@PlaybackStateCompat.RepeatMode repeatMode: Int) = Unit
        fun onSetShuffleMode(@PlaybackStateCompat.ShuffleMode shuffleMode: Int) = Unit
        fun onSkipToPrevious() = Unit
        fun onSkipToNext() = Unit
        fun onSkipToQueueItem(itemId: Long) = Unit
    }
}

private const val SESSION_TAG = "Odeon"
private const val ALL_SESSION_FLAGS = MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS or
        MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS

@ExperimentalMediaApi
internal class MediaSessionWrapper(
    context: Context,
    launchUiIntent: PendingIntent?,
    callback: MediaSession.Callback
) : MediaSession {

    private val session = MediaSessionCompat(context.applicationContext, SESSION_TAG).apply {
        setSessionActivity(launchUiIntent)
        setRatingType(RatingCompat.RATING_NONE)
        setCallback(SessionCallbackAdapter(callback))
    }

    override val token: MediaSessionCompat.Token
        get() = session.sessionToken

    override var isActive: Boolean
        get() = session.isActive
        set(value) { session.isActive = value }

    override fun setFlags(flags: Int) {
        session.setFlags(flags and ALL_SESSION_FLAGS)
    }

    override fun setMetadata(metadata: MediaMetadataCompat?) {
        session.setMetadata(metadata)
    }

    override fun setPlaybackState(state: PlaybackStateCompat?) {
        session.setPlaybackState(state)
    }

    override fun setRepeatMode(@PlaybackStateCompat.RepeatMode mode: Int) {
        session.setRepeatMode(mode)
    }

    override fun setShuffleMode(@PlaybackStateCompat.ShuffleMode mode: Int) {
        session.setShuffleMode(mode)
    }

    override fun setQueueTitle(title: CharSequence?) {
        session.setQueueTitle(title)
    }

    override fun setQueue(playQueue: List<MediaSessionCompat.QueueItem>?) {
        session.setQueue(playQueue)
    }

    override fun release() {
        session.release()
    }

    private class SessionCallbackAdapter(
        private val delegate: MediaSession.Callback
    ) : MediaSessionCompat.Callback() {

        override fun onPlay() {
            delegate.onPlay()
        }

        override fun onPause() {
            delegate.onPause()
        }

        override fun onStop() {
            delegate.onStop()
        }

        override fun onPrepare() {
            delegate.onPrepare()
        }

        override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
            delegate.onPrepareFromMediaId(mediaId, extras)
        }

        override fun onPrepareFromSearch(query: String?, extras: Bundle?) {
            delegate.onPrepareFromSearch(query, extras)
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            delegate.onPlayFromMediaId(mediaId, extras)
        }

        override fun onPlayFromSearch(query: String?, extras: Bundle?) {
            delegate.onPlayFromSearch(query, extras)
        }

        override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
            delegate.onCommand(command, extras, cb)
        }

        override fun onSeekTo(pos: Long) {
            delegate.onSeekTo(pos)
        }

        override fun onRewind() {
            delegate.onRewind()
        }

        override fun onFastForward() {
            delegate.onFastForward()
        }

        override fun onSetRepeatMode(repeatMode: Int) {
            delegate.onSetRepeatMode(repeatMode)
        }

        override fun onSetShuffleMode(shuffleMode: Int) {
            delegate.onSetShuffleMode(shuffleMode)
        }

        override fun onSkipToPrevious() {
            delegate.onSkipToPrevious()
        }

        override fun onSkipToNext() {
            delegate.onSkipToNext()
        }

        override fun onSkipToQueueItem(id: Long) {
            delegate.onSkipToQueueItem(id)
        }
    }
}