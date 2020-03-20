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

import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import fr.nihilus.music.core.media.InvalidMediaException
import fr.nihilus.music.core.media.toMediaId
import fr.nihilus.music.core.playback.RepeatMode
import fr.nihilus.music.service.browser.SearchQuery
import timber.log.Timber

/**
 * A [MediaSession] implementation that delegates to [MediaSessionCompat].
 * It abstracts implementation details such as [MediaMetadataCompat] and [MediaDescriptionCompat]
 * away so that the whole app gains in flexibility and testability.
 *
 * @param session The platform media session to which operations are delegated.
 * This media session should still be released after use.
 */
internal class MediaSessionCompatWrapper(
    private val session: MediaSessionCompat
) : MediaSession {

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

    private class SessionCompatCallbackAdapter(
        private val callback: MediaSession.Callback
    ) : MediaSessionCompat.Callback() {

        override fun onPlay() {
            callback.onPlay()
        }

        override fun onPause() {
            callback.onPause()
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) = try {
            callback.onPlayFromMediaId(mediaId.toMediaId())
        } catch (invalid: InvalidMediaException) {
            Timber.i("Cannot play from malformed media id: %s.", mediaId)
        }

        override fun onPlayFromSearch(query: String?, extras: Bundle?) {
            val focusedQuery = SearchQuery.from(query, extras)
            callback.onPlayFromSearch(focusedQuery)
        }

        override fun onPrepare() {
            callback.onPrepare()
        }

        override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) = try {
            callback.onPrepareFromMediaId(mediaId.toMediaId())
        } catch (invalid: InvalidMediaException) {
            Timber.i("Cannot prepare from malformed media id: %s", mediaId)
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