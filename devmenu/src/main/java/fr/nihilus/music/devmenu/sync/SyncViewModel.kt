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

package fr.nihilus.music.devmenu.sync

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.nihilus.music.spotify.manager.SpotifyManager
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import javax.inject.Inject

class SyncViewModel @Inject constructor(
    private val manager: SpotifyManager
) : ViewModel() {

    private val syncActor = viewModelScope.actor<Unit> {
        consumeEach {
            try {
                _isLoading.value = true
                manager.sync()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun startSync() {
        syncActor.offer(Unit)
    }
}