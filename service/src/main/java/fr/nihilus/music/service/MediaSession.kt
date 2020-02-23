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
import fr.nihilus.music.service.browser.SearchQuery

/**
 * A Media Session is responsive for the communication with the player.
 * The session maintains a representation of the player's state and information
 * about what is playing.
 *
 * A session can receive callbacks from one or more media controllers.
 * This makes it possible for the player to be controller by the app's UI
 * as well as companion devices running Android Wear or Android Auto.
 *
 * You should [release] the session when done performing playback.
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

    /**
     * Terminates the media session, disconnecting all its clients.
     * This must be called when the app is no longer expected to play media,
     * for example when its containing service is destroyed.
     */
    fun release()

    interface Callback {
        fun onPlay()
        fun onPause()
        fun onPlayFromMediaId(mediaId: String?)
        fun onPlayFromSearch(query: SearchQuery)
        fun onPrepare()
        fun onPrepareFromMediaId(mediaId: String?)
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

/**
 * A [MediaSession] implementation that delegates to [MediaSessionCompat].
 * It abstracts implementation details such as [MediaMetadataCompat] and [MediaDescriptionCompat]
 * away so that the whole app gains in flexibility and testability.
 */
internal class MediaSessionCompatWrapper(
    context: Context
) : MediaSession {

    private val session = MediaSessionCompat(context, "MusicService").also {
        it.setRatingType(RatingCompat.RATING_NONE)
    }

    private val metadataBuilder = MediaMetadataCompat.Builder()
    private val itemBuilder = MediaDescriptionCompat.Builder()

    override var isActive: Boolean
        get() = session.isActive
        private set(value) {
            if (session.isActive != value) {
                session.isActive = value
            }
        }

    override fun setCurrentMedia(track: AudioTrack?) {
        val metadata = track?.let {
            metadataBuilder.apply {
                putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, it.id.toString())
                putString(MediaMetadataCompat.METADATA_KEY_TITLE, it.title)
                putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, it.title)
                putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, it.subtitle)
                putString(MediaMetadataCompat.METADATA_KEY_ARTIST, it.artist)
                putString(MediaMetadataCompat.METADATA_KEY_ALBUM, it.album)
                putLong(MediaMetadataCompat.METADATA_KEY_DURATION, it.duration)
                putLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER, it.discNumber.toLong())
                putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, it.trackNumber.toLong())

                val iconUriString = it.iconUri?.toString()
                putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, iconUriString)
                putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, iconUriString)

                // TODO Set the album artwork as Bitmap for it to be displayed on the lock screen
                putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null)
                putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, null)

            }.build()
        }

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

    override fun setQueue(tracks: List<AudioTrack>?) {
        val queueItems = tracks?.mapIndexed { index, it ->
            val mediaDescription = itemBuilder.setMediaId(it.id.toString())
                .setTitle(it.title)
                .setSubtitle(it.subtitle)
                .setMediaUri(it.mediaUri)
                .setIconUri(it.iconUri)
                .build()
            MediaSessionCompat.QueueItem(mediaDescription, index.toLong())
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

    override fun setCallback(callback: MediaSession.Callback) {
        session.setCallback(SessionCompatCallbackAdapter(callback))
    }

    override fun release() {
        session.release()
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
            callback.onPlayFromMediaId(mediaId)
        }

        override fun onPlayFromSearch(query: String?, extras: Bundle?) {
            val focusedQuery = SearchQuery.from(query, extras)
            callback.onPlayFromSearch(focusedQuery)
        }

        override fun onPrepare() {
            callback.onPrepare()
        }

        override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
            callback.onPrepareFromMediaId(mediaId)
        }

        override fun onPrepareFromSearch(query: String?, extras: Bundle?) {
            val focusedQuery = SearchQuery.from(query, extras)
            callback.onPrepareFromSearch(focusedQuery)
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