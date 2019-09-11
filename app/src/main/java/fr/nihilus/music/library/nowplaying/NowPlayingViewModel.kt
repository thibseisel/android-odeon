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

package fr.nihilus.music.library.nowplaying

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import androidx.lifecycle.LiveData
import fr.nihilus.music.base.BaseViewModel
import fr.nihilus.music.client.MediaBrowserConnection
import fr.nihilus.music.service.extensions.isPlaying
import kotlinx.coroutines.launch
import javax.inject.Inject

class NowPlayingViewModel
@Inject constructor(
    private val connection: MediaBrowserConnection
) : BaseViewModel() {

    val playbackState: LiveData<PlaybackStateCompat> get() = connection.playbackState
    val nowPlaying: LiveData<MediaMetadataCompat?> get() = connection.nowPlaying
    val repeatMode: LiveData<Int> get() = connection.repeatMode
    val shuffleMode: LiveData<Int> get() = connection.shuffleMode

    fun togglePlayPause() {
        launch {
            val isPlaying = playbackState.value?.isPlaying ?: false
            if (isPlaying) connection.pause() else connection.play()
        }
    }

    fun skipToPrevious() {
        launch { connection.skipToPrevious() }
    }

    fun skipToNext() {
        launch { connection.skipToNext() }
    }

    fun seekTo(position: Long) {
        launch { connection.seekTo(position) }
    }

    fun toggleShuffleMode() {
        launch {
            when (shuffleMode.value ?: SHUFFLE_MODE_INVALID) {
                SHUFFLE_MODE_NONE, SHUFFLE_MODE_GROUP -> connection.setShuffleModeEnabled(true)
                SHUFFLE_MODE_ALL -> connection.setShuffleModeEnabled(false)
            }
        }
    }

    fun toggleRepeatMode() {
        launch {
            val currentMode = repeatMode.value ?: REPEAT_MODE_INVALID
            if (currentMode != REPEAT_MODE_INVALID) {
                connection.setRepeatMode((currentMode + 1) % 3)
            }
        }
    }
}