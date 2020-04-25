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

package fr.nihilus.music.library.playlists

import android.support.v4.media.MediaBrowserCompat.MediaItem
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.actions.ManagePlaylistAction
import fr.nihilus.music.core.ui.client.BrowserClient
import fr.nihilus.music.core.ui.client.MediaSubscriptionException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

class MembersViewModel @Inject constructor(
    private val client: BrowserClient,
    private val actions: ManagePlaylistAction
): ViewModel() {

    private var observeTracksJob: Job? = null

    private val _playlist = MutableLiveData<MediaItem>()
    val playlist: LiveData<MediaItem> = _playlist

    private val _members = MutableLiveData<LoadRequest<List<MediaItem>>>()
    val members: LiveData<LoadRequest<List<MediaItem>>> = _members

    fun setPlaylist(playlistId: String) {
        viewModelScope.launch {
            _playlist.value = checkNotNull(client.getItem(playlistId)) {
                "Unable to load detail of playlist $playlistId"
            }
        }

        observeTracksJob?.cancel()
        observeTracksJob = client.getChildren(playlistId)
            .map { LoadRequest.Success(it) as LoadRequest<List<MediaItem>> }
            .onStart { emit(LoadRequest.Pending) }
            .catch { if (it is MediaSubscriptionException) emit(LoadRequest.Error(it)) }
            .onEach { _members.postValue(it) }
            .launchIn(viewModelScope)
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            val playlistMediaId = MediaId(MediaId.TYPE_PLAYLISTS, playlistId)
            actions.deletePlaylist(playlistMediaId)
        }
    }
}
