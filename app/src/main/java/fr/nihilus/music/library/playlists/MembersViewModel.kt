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

package fr.nihilus.music.library.playlists

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.nihilus.music.common.media.CustomActions
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.client.MediaBrowserConnection
import fr.nihilus.music.core.ui.client.MediaSubscriptionException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

class MembersViewModel
@Inject constructor(
    private val connection: MediaBrowserConnection
): ViewModel() {
    private val token = MediaBrowserConnection.ClientToken()
    private var observeTracksJob: Job? = null

    private val _playlist = MutableLiveData<MediaItem>()
    val playlist: LiveData<MediaItem> = _playlist

    private val _members = MutableLiveData<LoadRequest<List<MediaItem>>>()
    val members: LiveData<LoadRequest<List<MediaItem>>> = _members

    init {
        connection.connect(token)
    }

    fun setPlaylist(playlistId: String) {
        viewModelScope.launch {
            _playlist.value = connection.getItem(playlistId)
        }

        observeTracksJob?.cancel()
        observeTracksJob = connection.getChildren(playlistId)
            .map { LoadRequest.Success(it) as LoadRequest<List<MediaItem>> }
            .onStart { emit(LoadRequest.Pending) }
            .catch { if (it is MediaSubscriptionException) emit(LoadRequest.Error(it)) }
            .onEach { _members.postValue(it) }
            .launchIn(viewModelScope)
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            val params = Bundle(1).apply {
                putStringArray(CustomActions.EXTRA_MEDIA_IDS, arrayOf(playlistId))
            }

            connection.executeAction(CustomActions.ACTION_DELETE_MEDIA, params)
        }
    }
}
