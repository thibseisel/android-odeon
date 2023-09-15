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

package fr.nihilus.music.ui

import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.nihilus.music.core.permissions.PermissionRepository
import fr.nihilus.music.core.ui.Event
import fr.nihilus.music.core.ui.client.BrowserClient
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class MusicLibraryViewModel @Inject constructor(
    client: BrowserClient,
    private val permissions: PermissionRepository,
) : ViewModel() {

    private val _playerSheetVisible = MutableLiveData<Boolean>()
    val playerSheetVisible: LiveData<Boolean> = _playerSheetVisible

    private val _playerError = MutableLiveData<Event<CharSequence>>()
    val playerError: LiveData<Event<CharSequence>> = _playerError

    init {
        client.playbackState.onEach {
            when (it.state) {
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
        }.launchIn(viewModelScope)
    }

    /**
     * Notify that user has granted permission to read media files
     * from the device's external storage.
     */
    fun onReadPermissionGranted() {
        permissions.refreshPermissions()
    }
}
