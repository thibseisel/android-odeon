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

package fr.nihilus.music.service

import android.app.Service
import android.content.Intent
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.content.ContextCompat

@ExperimentalMediaApi
internal interface MediaSessionHolder {

    /**
     * The current active state of the media session.
     * A session should be active when it is ready to receive commands.
     */
    var sessionIsActive: Boolean

    /**
     * The current started state of the underlying service.
     * A started service can run even when no clients are connected.
     * Starting a service is a costly operation and should not be done when already started.
     */
    @Deprecated("Management of the service started state should be done by the service itself.")
    var isStarted: Boolean

    /**
     * Update the state of the media session.
     *
     * @param state The new state for the media session.
     */
    fun publishPlaybackState(state: PlaybackStateCompat)

    /**
     * Update the media session metadata.
     * Specify `null` to indicate that there is no current item prepared for playback.
     *
     * @param metadata The metadata of the currently playing item or `null`.
     */
    fun publishMetadata(metadata: MediaMetadataCompat?)

    /**
     * Update the title associated with the media session queue.
     * The UI should display this title with the play queue itself.
     *
     * @param queueTitle The title of the current play queue,
     * for example the name of the album, artist, playlist whose tracks are part of the queue.
     */
    fun publishQueueTitle(queueTitle: CharSequence?)

    /**
     * Updates the list of items in the play queue.
     * It is an ordered list and should contain the current item,
     * and previous or upcoming items if they exist.
     * Specify `null` if there is no current play queue.
     *
     * The queue should be of reasonable size.
     * If the play queue is unbounded,
     * it is better to send a reasonable amount in a sliding window instead.
     *
     * @param queue A list of items in the play queue.
     */
    fun publishQueue(queue: List<MediaSessionCompat.QueueItem>?)

    /**
     * Notifies connected media browsers that the current shuffle mode has changed.
     * When shuffle mode is enabled media queues are played in a random order.
     *
     * @param enabled `false` if the current queue is played in order, `true` if it is shuffled.
     */
    fun publishShuffleModeEnabled(enabled: Boolean)

    /**
     * Notifies connected media browsers of a change in the media session's repeat mode.
     * Repeat mode indicates if and how playback of media queues should be repeated.
     *
     * @param mode The new repeat mode.
     */
    fun publishRepeatMode(mode: RepeatMode)
}

@ExperimentalMediaApi
internal class MediaSessionHolderImpl(
    private val service: Service,
    private val session: MediaSessionCompat
) : MediaSessionHolder {

    private val serviceIntent = Intent(service, service::class.java)

    override var sessionIsActive: Boolean
        get() = session.isActive
        set(isActive) {
            session.isActive = isActive
        }

    override var isStarted: Boolean = false
        set(shouldStart) {
            if (!shouldStart) {
                service.stopService(serviceIntent)
                field = false
            } else if (!field) {
                ContextCompat.startForegroundService(service, serviceIntent)
                field = true
            }
        }

    override fun publishPlaybackState(state: PlaybackStateCompat) {
        session.setPlaybackState(state)
    }

    override fun publishMetadata(metadata: MediaMetadataCompat?) {
        session.setMetadata(metadata)
    }

    override fun publishQueueTitle(queueTitle: CharSequence?) {
        session.setQueueTitle(queueTitle)
    }

    override fun publishQueue(queue: List<MediaSessionCompat.QueueItem>?) {
        session.setQueue(queue)
    }

    override fun publishShuffleModeEnabled(enabled: Boolean) {
        session.setShuffleMode(
            when (enabled) {
                false -> PlaybackStateCompat.SHUFFLE_MODE_NONE
                true -> PlaybackStateCompat.SHUFFLE_MODE_ALL
            }
        )
    }

    override fun publishRepeatMode(mode: RepeatMode) {
        session.setRepeatMode(
            when (mode) {
                RepeatMode.OFF -> PlaybackStateCompat.REPEAT_MODE_NONE
                RepeatMode.ONE -> PlaybackStateCompat.REPEAT_MODE_ONE
                RepeatMode.ALL -> PlaybackStateCompat.REPEAT_MODE_ALL
            }
        )
    }
}

/**
 * Available options for repeating playback of a list of media or a single item.
 */
internal enum class RepeatMode {

    /**
     * Repeat mode is disabled.
     * Playback will be stopped at the end of the current play queue.
     *
     * @see PlaybackStateCompat.REPEAT_MODE_NONE
     */
    OFF,
    /**
     * Repeat a single item.
     * The current media will be replayed after completed.
     *
     * @see PlaybackStateCompat.REPEAT_MODE_ONE
     */
    ONE,
    /**
     * Repeat the whole play queue.
     * After the current queue has been played, it is replayed from the beginning.
     *
     * @see PlaybackStateCompat.REPEAT_MODE_ALL
     */
    ALL
}