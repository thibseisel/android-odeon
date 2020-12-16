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

package fr.nihilus.music.library.nowplaying

import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.parse
import fr.nihilus.music.core.playback.RepeatMode
import fr.nihilus.music.core.ui.client.BrowserClient
import fr.nihilus.music.service.extensions.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

class NowPlayingViewModel @Inject constructor(
    private val client: BrowserClient
) : ViewModel() {

    val state: LiveData<PlayerState> = liveData {
        // The Flow combine operator use a cached value of this Flow when the playback state changes.
        // Mapping here ensures that the transformation is only applied when metadata has changed.
        val currentTrackState = client.nowPlaying
            .filter { it == null || it.id != null }
            .map { nowPlaying ->
                nowPlaying?.let {
                    PlayerState.Track(
                        id = it.id.parse(),
                        title = it.displayTitle!!,
                        artist = it.displaySubtitle!!,
                        duration = when (it.containsKey(MediaMetadataCompat.METADATA_KEY_DURATION)) {
                            true -> it.duration
                            else -> PLAYBACK_POSITION_UNKNOWN
                        },
                        artworkUri = it.displayIconUri
                    )
                }
            }

        combine(
            client.playbackState,
            currentTrackState,
            client.shuffleMode,
            client.repeatMode
        ) { state, nowPlaying, shuffleModeCode, repeatModeCode ->
            PlayerState(
                isPlaying = state.isPlaying,
                currentTrack = nowPlaying,
                position = state.position,
                lastPositionUpdateTime = state.lastPositionUpdateTime,
                availableActions = parseAvailableActions(state.actions),
                shuffleModeEnabled = when (shuffleModeCode) {
                    SHUFFLE_MODE_ALL, SHUFFLE_MODE_GROUP -> true
                    else -> false
                },
                repeatMode = when (repeatModeCode) {
                    REPEAT_MODE_ALL, REPEAT_MODE_GROUP -> RepeatMode.ALL
                    REPEAT_MODE_ONE -> RepeatMode.ONE
                    else -> RepeatMode.DISABLED
                }
            )
        }.collect {
            emit(it)
        }
    }

    private fun parseAvailableActions(actionCodes: Long): Set<PlayerState.Action> {
        val actions = EnumSet.noneOf(PlayerState.Action::class.java)

        if (actionCodes and ACTION_PLAY_PAUSE != 0L) {
            actions += PlayerState.Action.TOGGLE_PLAY_PAUSE
        }

        if (actionCodes and ACTION_PLAY != 0L) {
            actions += PlayerState.Action.PLAY
        }

        if (actionCodes and ACTION_PAUSE != 0L) {
            actions += PlayerState.Action.PAUSE
        }

        if (actionCodes and ACTION_SKIP_TO_PREVIOUS != 0L) {
            actions += PlayerState.Action.SKIP_BACKWARD
        }

        if (actionCodes and ACTION_SKIP_TO_NEXT != 0L) {
            actions += PlayerState.Action.SKIP_FORWARD
        }

        if (actionCodes and ACTION_SET_SHUFFLE_MODE != 0L) {
            actions += PlayerState.Action.SET_SHUFFLE_MODE
        }

        if (actionCodes and ACTION_SET_REPEAT_MODE != 0L) {
            actions += PlayerState.Action.SET_REPEAT_MODE
        }

        return actions
    }

    fun togglePlayPause() {
        viewModelScope.launch {
            when (state.value?.isPlaying) {
                true -> client.pause()
                false -> client.play()
            }
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
            state.value?.let {
                client.setShuffleModeEnabled(!it.shuffleModeEnabled)
            }
        }
    }

    fun toggleRepeatMode() {
        viewModelScope.launch {
            state.value?.let {
                client.setRepeatMode(when (it.repeatMode) {
                    RepeatMode.ALL -> REPEAT_MODE_ONE
                    RepeatMode.ONE -> REPEAT_MODE_NONE
                    else -> REPEAT_MODE_ALL
                })
            }
        }
    }
}

data class PlayerState(
    val isPlaying: Boolean,
    val currentTrack: Track?,
    val shuffleModeEnabled: Boolean,
    val repeatMode: RepeatMode,
    val position: Long,
    val lastPositionUpdateTime: Long,
    val availableActions: Set<Action>
) {
    data class Track(
        val id: MediaId,
        val title: String,
        val artist: String,
        val duration: Long,
        val artworkUri: Uri?
    )

    enum class Action {
        TOGGLE_PLAY_PAUSE,
        PLAY,
        PAUSE,
        SKIP_FORWARD,
        SKIP_BACKWARD,
        SET_REPEAT_MODE,
        SET_SHUFFLE_MODE
    }
}