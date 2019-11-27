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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import fr.nihilus.music.core.ui.client.BrowserClient
import fr.nihilus.music.service.extensions.isPlaying
import kotlinx.coroutines.launch
import javax.inject.Inject

class NowPlayingViewModel
@Inject constructor(
    private val client: BrowserClient
) : ViewModel() {

    val playbackState: LiveData<PlaybackStateCompat> = client.playbackState.asLiveData()
    val nowPlaying: LiveData<MediaMetadataCompat?> = client.nowPlaying.asLiveData()
    val repeatMode: LiveData<Int> = client.repeatMode.asLiveData()
    val shuffleMode: LiveData<Int> = client.shuffleMode.asLiveData()

    fun togglePlayPause() {
        viewModelScope.launch {
            val isPlaying = playbackState.value?.isPlaying ?: false
            if (isPlaying) client.pause() else client.play()
        }
    }

    fun skipToPrevious() {
        viewModelScope.launch { client.skipToPrevious() }
    }

    fun skipToNext() {
        viewModelScope.launch { client.skipToNext() }
    }

    fun seekTo(position: Long) {
        viewModelScope.launch { client.seekTo(position) }
    }

    fun toggleShuffleMode() {
        viewModelScope.launch {
            when (shuffleMode.value ?: SHUFFLE_MODE_INVALID) {
                SHUFFLE_MODE_NONE, SHUFFLE_MODE_GROUP -> client.setShuffleModeEnabled(true)
                SHUFFLE_MODE_ALL -> client.setShuffleModeEnabled(false)
            }
        }
    }

    fun toggleRepeatMode() {
        viewModelScope.launch {
            val currentMode = repeatMode.value ?: REPEAT_MODE_INVALID
            if (currentMode != REPEAT_MODE_INVALID) {
                client.setRepeatMode((currentMode + 1) % 3)
            }
        }
    }
}