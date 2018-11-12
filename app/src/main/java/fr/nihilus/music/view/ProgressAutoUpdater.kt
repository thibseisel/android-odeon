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

package fr.nihilus.music.view

import android.os.Handler
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.util.TimeUtils
import android.text.format.DateUtils
import android.widget.SeekBar
import android.widget.TextView
import fr.nihilus.music.media.extensions.duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * The number of milliseconds before starting updating progress.
 */
private const val PROGRESS_UPDATE_INITIAL_DELAY = 100L

/**
 * The period in milliseconds at which playback position is updated.
 */
private const val PROGRESS_UPDATE_PERIOD = 1000L

/**
 * Encapsulates the logic to automatically update display of the current playback position.
 * In order to listen for user requests to move playback to a specific position,
 * you may set this object as the listener for the passed SeekBar.
 *
 * @constructor Wrap Views into an auto-update controller.
 *
 * @param seekBar The seekBar whose position should be updated while playing.
 * @param seekPosition TextView displaying the current position in format `MM:SS`.
 * @param seekDuration TextView displaying the total duration in format `MM:SS`.
 * @param updateListener Callback executed when user changes progress of the SeekBar.
 * Parameter is the desired playback position in milliseconds.
 */
class ProgressAutoUpdater(
    private val seekBar: SeekBar,
    private val seekPosition: TextView?,
    private val seekDuration: TextView?,
    private val updateListener: (Long) -> Unit
) : SeekBar.OnSeekBarChangeListener {

    /**
     * Wrap a SeekBar into an auto-update controller.
     *
     * @param seekBar The seekBar whose position should be updated while playing.
     * @param updateListener Callback executed when user changes progress of the seekBar.
     * Parameter is the desired playback position in milliseconds.
     */
    constructor(
        seekBar: SeekBar,
        updateListener: (Long) -> Unit
    ) : this(seekBar, null, null, updateListener)

    private val builder = StringBuilder()
    private val executorService = Executors.newSingleThreadScheduledExecutor()
    private val handler = Handler()

    private val uiThreadUpdate = Runnable(this::updateProgress)
    private val scheduledUpdate = Runnable {
        handler.post(uiThreadUpdate)
    }

    private var currentState: PlaybackStateCompat? = null
    private var updateFuture: ScheduledFuture<*>? = null

    /**
     * Updates the media metadata for this SeekBar. It will be used to define the maximum progress
     * of this SeekBar based on the track's duration.
     *
     * It the metadata is `null`, the maximum progress will be reset to zero.
     *
     * @param metadata The metadata of the currently playing track
     */
    fun setMetadata(metadata: MediaMetadataCompat?) {
        val maxProgress = metadata?.duration ?: 0L
        seekBar.max = maxProgress.toInt()
        seekDuration?.text = DateUtils.formatElapsedTime(builder, maxProgress / 1000L)
    }

    /**
     * Updates the playback state for this SeekBar.
     * Automatic updates will only be done if playback is active.
     *
     * @param state The current playback state for this media session.
     */
    fun setPlaybackState(state: PlaybackStateCompat?) {
        currentState = state

        if (state != null) {
            val isPlaying = state.state == PlaybackStateCompat.STATE_PLAYING
            val position = state.position

            if (position != PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN) {
                seekBar.progress = position.toInt()
            } else {
                seekPosition?.text = null
            }

            if (isPlaying) scheduleProgressUpdate()
            else stopProgressUpdate()

        } else {
            seekPosition?.text = null
            stopProgressUpdate()
        }

    }

    override fun onProgressChanged(view: SeekBar, progress: Int, fromUser: Boolean) {
        seekPosition?.text = DateUtils.formatElapsedTime(builder, progress / 1000L)
    }

    override fun onStartTrackingTouch(view: SeekBar) {
        stopProgressUpdate()
    }

    override fun onStopTrackingTouch(view: SeekBar) {
        updateListener.invoke(view.progress.toLong())
        scheduleProgressUpdate()
    }

    private fun scheduleProgressUpdate() {
        updateFuture = executorService.scheduleAtFixedRate(
            scheduledUpdate,
            PROGRESS_UPDATE_INITIAL_DELAY, PROGRESS_UPDATE_PERIOD, TimeUnit.MILLISECONDS
        )
    }

    private fun stopProgressUpdate() {
        updateFuture?.cancel(false)
    }

    private fun updateProgress() {
        currentState?.let { state ->
            var currentPosition = state.position
            if (state.state == PlaybackStateCompat.STATE_PLAYING) {

                /* Calculate the elapsed time between the last position update and now
                 * and unless paused, we can assume (delta * speed) + current position
                 * is approximately the latest position. */
                val timeDelta = SystemClock.elapsedRealtime() - state.lastPositionUpdateTime
                currentPosition += (timeDelta * state.playbackSpeed).toLong()
            }

            seekBar.progress = currentPosition.toInt()
        }
    }
}