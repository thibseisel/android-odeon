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

import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.nihilus.music.common.media.MediaId
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.client.MediaBrowserConnection
import fr.nihilus.music.core.ui.client.MediaSubscriptionException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class PlaylistsViewModel
@Inject constructor(
    private val connection: MediaBrowserConnection
) : ViewModel() {
    private val token = MediaBrowserConnection.ClientToken()

    private val _children = MutableLiveData<LoadRequest<List<MediaBrowserCompat.MediaItem>>>()
    val children: LiveData<LoadRequest<List<MediaBrowserCompat.MediaItem>>> = _children

    init {
        connection.connect(token)
        loadBuiltinAndUserPlaylists()
    }

    private fun loadBuiltinAndUserPlaylists(): Job {
        val allPlaylists = combine(
            builtInPlaylistFlow(MediaId.CATEGORY_RECENTLY_ADDED),
            builtInPlaylistFlow(MediaId.CATEGORY_MOST_RATED),
            connection.subscribe(MediaId.TYPE_PLAYLISTS)
        ) { mostRecent, mostRated, playlists ->
            ArrayList<MediaBrowserCompat.MediaItem>(playlists.size + 2).also {
                it += mostRecent
                it += mostRated
                it += playlists
            }
        }

        return allPlaylists
            .map { LoadRequest.Success(it) as LoadRequest<List<MediaBrowserCompat.MediaItem>> }
            .onStart { emit(LoadRequest.Pending) }
            .catch { if (it is MediaSubscriptionException) emit(LoadRequest.Error(it)) }
            .onEach { _children.value = it }
            .launchIn(viewModelScope)
    }

    private fun builtInPlaylistFlow(category: String) = flow<MediaBrowserCompat.MediaItem> {
        val itemId = MediaId.encode(MediaId.TYPE_TRACKS, category)
        emit(connection.getItem(itemId) ?: error("Item with id $itemId should always exist."))
    }
}
