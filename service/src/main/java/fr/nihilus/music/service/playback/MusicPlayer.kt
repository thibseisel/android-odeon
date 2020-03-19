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

package fr.nihilus.music.service.playback

import fr.nihilus.music.core.playback.RepeatMode
import fr.nihilus.music.service.AudioTrack

internal interface MusicPlayer {

    val state: State
    val currentPosition: Long
    val bufferedPosition: Long
    val duration: Long
    val playbackSpeed: Float

    val playlist: List<AudioTrack>
    val currentPlaylistIndex: Int

    var playWhenReady: Boolean

    fun prepare(queueId: Long, playlist: List<AudioTrack>, startAtIndex: Int)
    fun stop()
    fun seekTo(position: Long)
    fun setShuffleModeEnabled(enabled: Boolean)
    fun setRepeatMode(mode: RepeatMode)
    fun skipForward()
    fun skipBackward()
    fun hasPrevious(): Boolean
    fun hasNext(): Boolean
    fun skipToPlaylistPosition(index: Int)
    fun registerEventListener(listener: EventListener)
    fun unregisterEventListener(listener: EventListener)

    enum class State {
        IDLE,
        PAUSED,
        PLAYING,
        ERROR
    }

    interface EventListener {
        fun onQueueChanged(queue: List<AudioTrack>)
        fun onPlayerStateChanged(newState: State)
        fun onCurrentMediaChanged(currentTrack: AudioTrack)
        fun onShuffleModeChanged(isEnabled: Boolean)
        fun onRepeatModeChanged(newMode: RepeatMode)
        fun onTrackCompleted(track: AudioTrack)
    }
}