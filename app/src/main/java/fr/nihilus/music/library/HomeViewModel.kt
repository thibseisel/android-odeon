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

package fr.nihilus.music.library

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.nihilus.music.common.media.CustomActions
import fr.nihilus.music.common.media.MediaId
import fr.nihilus.music.core.ui.Event
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.client.BrowserClient
import fr.nihilus.music.core.ui.client.MediaSubscriptionException
import fr.nihilus.music.core.ui.extensions.consumeAsLiveData
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

class HomeViewModel
@Inject constructor(
    private val client: BrowserClient
) : ViewModel() {
    private val token = BrowserClient.ClientToken()

    val tracks: LiveData<LoadRequest<List<MediaItem>>> by lazyChildrenOf(MediaId.ALL_TRACKS)
    val albums: LiveData<LoadRequest<List<MediaItem>>> by lazyChildrenOf(MediaId.ALL_ALBUMS)
    val artists: LiveData<LoadRequest<List<MediaItem>>> by lazyChildrenOf(MediaId.ALL_ARTISTS)
    val playlists: LiveData<LoadRequest<List<MediaItem>>> by lazyPlaylists()

    private val _deleteTracksConfirmation = MutableLiveData<Event<Int>>()
    val deleteTracksConfirmation: MutableLiveData<Event<Int>> = _deleteTracksConfirmation

    init {
        client.connect(token)
    }

    fun deleteSongs(songsToDelete: List<MediaItem>) {
        viewModelScope.launch {
            val trackMediaIds = Array(songsToDelete.size) { position ->
                songsToDelete[position].mediaId
            }

            val parameters = Bundle(1)
            parameters.putStringArray(CustomActions.EXTRA_MEDIA_IDS, trackMediaIds)
            val result = client.executeAction(CustomActions.ACTION_DELETE_MEDIA, parameters)

            val deletedTracksCount = result?.getInt(CustomActions.RESULT_TRACK_COUNT) ?: 0
            _deleteTracksConfirmation.value = Event(deletedTracksCount)
        }
    }

    fun playAllShuffled() {
        viewModelScope.launch {
            client.setShuffleModeEnabled(true)
            client.playFromMediaId(MediaId.ALL_TRACKS)
        }
    }

    private fun lazyChildrenOf(parentId: String): Lazy<LiveData<LoadRequest<List<MediaItem>>>> = lazy {
        client.getChildren(parentId)
            .loadState()
            .consumeAsLiveData(viewModelScope)
    }

    private fun lazyPlaylists(): Lazy<LiveData<LoadRequest<List<MediaItem>>>> {
        return lazy {
            val allPlaylists: Flow<List<MediaItem>> = combine(
                builtInPlaylistFlow(MediaId.CATEGORY_RECENTLY_ADDED),
                builtInPlaylistFlow(MediaId.CATEGORY_MOST_RATED),
                client.getChildren(MediaId.TYPE_PLAYLISTS)
            ) { mostRecent, mostRated, playlists ->
                ArrayList<MediaItem>(playlists.size + 2).also {
                    it += mostRecent
                    it += mostRated
                    it += playlists
                }
            }

            allPlaylists
                .loadState()
                .consumeAsLiveData(viewModelScope)
        }
    }

    private fun builtInPlaylistFlow(category: String) = flow<MediaItem> {
        val itemId = MediaId.encode(MediaId.TYPE_TRACKS, category)
        emit(client.getItem(itemId) ?: error("Item with id $itemId should always exist."))
    }

    override fun onCleared() {
        client.disconnect(token)
        super.onCleared()
    }
}

private fun <T> Flow<T>.loadState(): Flow<LoadRequest<T>> =
    map { LoadRequest.Success(it) as LoadRequest<T> }
        .onStart { emit(LoadRequest.Pending) }
        .catch { if (it is MediaSubscriptionException) emit(LoadRequest.Error(it)) }