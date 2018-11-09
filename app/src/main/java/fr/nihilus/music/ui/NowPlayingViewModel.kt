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

package fr.nihilus.music.ui

import android.arch.lifecycle.LiveData
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import fr.nihilus.music.client.MediaBrowserConnection
import fr.nihilus.music.client.MediaBrowserViewModel
import fr.nihilus.music.media.extensions.isPlaying
import javax.inject.Inject

class NowPlayingViewModel
@Inject constructor(
    connection: MediaBrowserConnection
) : MediaBrowserViewModel(connection) {

    val playbackState: LiveData<PlaybackStateCompat> get() = connection.playbackState
    val nowPlaying: LiveData<MediaMetadataCompat?> get() = connection.nowPlaying
    val repeatMode: LiveData<Int> get() = connection.repeatMode
    val shuffleMode: LiveData<Int> get() = connection.shuffleMode

    fun togglePlayPause() = connection.post {
        val isPlaying = playbackState.value?.isPlaying ?: false
        if (isPlaying) it.transportControls.pause() else it.transportControls.play()
    }

    fun skipToPrevious() = connection.post { it.transportControls.skipToPrevious() }

    fun skipToNext() = connection.post { it.transportControls.skipToNext() }

    fun seekTo(position: Long) = connection.post { it.transportControls.seekTo(position) }

    fun toggleShuffleMode() = connection.post { controller ->
        val currentMode = shuffleMode.value ?: SHUFFLE_MODE_INVALID
        with(controller.transportControls) {
            when (currentMode) {
                SHUFFLE_MODE_NONE, SHUFFLE_MODE_GROUP -> setShuffleMode(SHUFFLE_MODE_ALL)
                SHUFFLE_MODE_ALL -> setShuffleMode(SHUFFLE_MODE_NONE)
            }
        }
    }

    fun toggleRepeatMode() = connection.post { controller ->
        val currentMode = repeatMode.value ?: REPEAT_MODE_INVALID
        if (currentMode != REPEAT_MODE_INVALID) {
            controller.transportControls.setRepeatMode((currentMode + 1) % 3)
        }
    }
}