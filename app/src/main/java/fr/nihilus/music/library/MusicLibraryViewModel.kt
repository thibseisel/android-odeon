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

package fr.nihilus.music.library

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import fr.nihilus.music.common.media.MediaId
import fr.nihilus.music.core.ui.Event
import fr.nihilus.music.core.ui.base.BaseViewModel
import fr.nihilus.music.core.ui.client.MediaBrowserConnection
import kotlinx.coroutines.launch
import javax.inject.Inject

class MusicLibraryViewModel
@Inject constructor(
    context: Context,
    private val connection: MediaBrowserConnection
) : BaseViewModel() {
    private val client = MediaBrowserConnection.ClientToken()

    private val _playerSheetVisible = MutableLiveData<Boolean>()
    val playerSheetVisible: LiveData<Boolean>
        get() = _playerSheetVisible

    private val _playerError = MutableLiveData<Event<CharSequence>>()
    val playerError: LiveData<Event<CharSequence>>
        get() = _playerError

    private val playbackStateObserver = Observer<PlaybackStateCompat> {
        when (it?.state) {
            PlaybackStateCompat.STATE_NONE,
            PlaybackStateCompat.STATE_STOPPED -> {
                _playerSheetVisible.value = false
            }

            PlaybackStateCompat.STATE_ERROR -> {
                _playerSheetVisible.value = false
                _playerError.value = Event(it.errorMessage)
            }

            PlaybackStateCompat.STATE_PAUSED,
            PlaybackStateCompat.STATE_PLAYING -> {
                _playerSheetVisible.value = true
            }
        }
    }

    init {
        connection.connect(client)
        connection.playbackState.observeForever(playbackStateObserver)
    }

    fun playMedia(playableMedia: MediaBrowserCompat.MediaItem) {
        require(playableMedia.isPlayable) { "The specified media is not playable." }

        launch {
            connection.playFromMediaId(playableMedia.mediaId!!)
        }
    }

    fun playAllShuffled() {
        launch {
            connection.setShuffleModeEnabled(true)
            connection.playFromMediaId(MediaId.ALL_TRACKS)
        }
    }

    override fun onCleared() {
        connection.playbackState.removeObserver(playbackStateObserver)
        super.onCleared()
        connection.disconnect(client)
    }
}