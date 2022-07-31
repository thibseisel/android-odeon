/*
 * Copyright 2022 Thibault Seisel
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

package fr.nihilus.music.ui.nowplaying

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.SeekBar
import android.widget.TextView
import fr.nihilus.music.core.ui.formatDuration
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
    private val seekPosition: TextView,
    private val seekDuration: TextView,
    private val updateListener: (Long) -> Unit
) {
    private val executorService = Executors.newSingleThreadScheduledExecutor()
    private val handler = Handler(Looper.getMainLooper())

    private val uiThreadUpdate = Runnable(this::updateProgress)
    private val scheduledUpdate = Runnable {
        handler.post(uiThreadUpdate)
    }

    private var lastPositionUpdate = -1L
    private var lastPositionUpdateTime = -1L
    private var shouldAutoAdvance = false
    private var updateFuture: ScheduledFuture<*>? = null

    init {
        val listener = UserSeekListener()
        seekBar.setOnSeekBarChangeListener(listener)
    }

    fun update(position: Long, duration: Long, updateTime: Long, autoAdvance: Boolean) {
        assert(position <= duration)

        // Remember the last time position was updated for auto-update based on time.
        lastPositionUpdate = position
        lastPositionUpdateTime = updateTime

        // Update the max progression.
        seekBar.max = duration.toInt()
        seekDuration.text = formatDuration(duration)

        // Update the visual playback position.
        if (position != -1L) {
            seekBar.progress = position.toInt()
            seekPosition.text = formatDuration(position)
        } else {
            seekBar.progress = 0
            seekPosition.text = null
        }

        // Schedule to automatically update playback position if requested.
        shouldAutoAdvance = autoAdvance
        if (autoAdvance) {
            scheduleProgressUpdate()
        } else {
            stopProgressUpdate()
        }
    }

    private fun scheduleProgressUpdate() {
        updateFuture = executorService.scheduleAtFixedRate(
            scheduledUpdate,
            PROGRESS_UPDATE_INITIAL_DELAY,
            PROGRESS_UPDATE_PERIOD, TimeUnit.MILLISECONDS
        )
    }

    private fun stopProgressUpdate() {
        updateFuture?.cancel(false)
    }

    private fun updateProgress() {
        if (shouldAutoAdvance) {
            val elapsedTime = SystemClock.elapsedRealtime() - lastPositionUpdateTime
            val currentPosition = lastPositionUpdate + elapsedTime
            seekBar.progress = currentPosition.toInt()
        }
    }

    private inner class UserSeekListener : SeekBar.OnSeekBarChangeListener {

        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            seekPosition.text = formatDuration(progress.toLong())
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            stopProgressUpdate()
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            updateListener(seekBar.progress.toLong())
            scheduleProgressUpdate()
        }
    }
}
