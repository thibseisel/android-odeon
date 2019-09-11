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
import fr.nihilus.music.common.media.MediaId
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.base.BaseViewModel
import fr.nihilus.music.core.ui.client.MediaBrowserConnection
import fr.nihilus.music.core.ui.client.MediaSubscriptionException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import javax.inject.Inject

class PlaylistsViewModel
@Inject constructor(
    private val connection: MediaBrowserConnection
) : BaseViewModel() {
    private val token = MediaBrowserConnection.ClientToken()

    private val _children = MutableLiveData<LoadRequest<List<MediaBrowserCompat.MediaItem>>>()
    val children: LiveData<LoadRequest<List<MediaBrowserCompat.MediaItem>>>
        get() = _children

    init {
        connection.connect(token)
        loadBuiltinAndUserPlaylists()
    }

    private fun loadBuiltinAndUserPlaylists(): Job = launch {
        _children.postValue(LoadRequest.Pending)
        coroutineScope {
            val mostRecent = loadBuiltInPlaylistItemAsync(MediaId.CATEGORY_RECENTLY_ADDED)
            val mostRated = loadBuiltInPlaylistItemAsync(MediaId.CATEGORY_MOST_RATED)

            try {
                connection.subscribe(MediaId.ALL_PLAYLISTS).consumeEach { childrenUpdate ->
                    val displayedItems = ArrayList<MediaBrowserCompat.MediaItem>(childrenUpdate.size + 2)
                    displayedItems += mostRecent.await()
                    displayedItems += mostRated.await()
                    displayedItems += childrenUpdate

                    _children.postValue(LoadRequest.Success(displayedItems))
                }

            } catch (e: MediaSubscriptionException) {
                _children.postValue(LoadRequest.Error(e))
            }
        }
    }

    private fun CoroutineScope.loadBuiltInPlaylistItemAsync(category: String) = async {
        val itemId = MediaId.encode(MediaId.TYPE_TRACKS, category)
        connection.getItem(itemId) ?: error("Item with id $itemId should always exist.")
    }
}
