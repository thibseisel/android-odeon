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

package fr.nihilus.music.library

import android.support.v4.media.MediaBrowserCompat.MediaItem
import androidx.lifecycle.*
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.parse
import fr.nihilus.music.core.ui.Event
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.actions.DeleteTracksAction
import fr.nihilus.music.core.ui.client.BrowserClient
import fr.nihilus.music.core.ui.client.MediaSubscriptionException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

class HomeViewModel @Inject constructor(
    private val client: BrowserClient,
    private val actions: DeleteTracksAction
) : ViewModel() {

    val tracks: LiveData<LoadRequest<List<MediaItem>>> = childrenOf(MediaId.ALL_TRACKS)
    val albums: LiveData<LoadRequest<List<MediaItem>>> = childrenOf(MediaId.ALL_ALBUMS)
    val artists: LiveData<LoadRequest<List<MediaItem>>> = childrenOf(MediaId.ALL_ARTISTS)
    val playlists: LiveData<LoadRequest<List<MediaItem>>> = allPlaylists()

    private val _deleteTracksConfirmation = MutableLiveData<Event<Int>>()
    val deleteTracksConfirmation: MutableLiveData<Event<Int>> = _deleteTracksConfirmation

    fun deleteSongs(songsToDelete: List<MediaItem>) {
        viewModelScope.launch {
            val trackIds = songsToDelete.map { it.mediaId.parse() }
            val deletedTracksCount = actions.delete(trackIds)
            _deleteTracksConfirmation.value = Event(deletedTracksCount)
        }
    }

    fun playAllShuffled() {
        viewModelScope.launch {
            client.setShuffleModeEnabled(true)
            client.playFromMediaId(MediaId.ALL_TRACKS)
        }
    }

    private fun allPlaylists(): LiveData<LoadRequest<List<MediaItem>>> = combine(
        builtInPlaylistFlow(MediaId.CATEGORY_RECENTLY_ADDED),
        builtInPlaylistFlow(MediaId.CATEGORY_MOST_RATED),
        client.getChildren(MediaId.ALL_PLAYLISTS)
    ) { mostRecent, mostRated, playlists ->
        ArrayList<MediaItem>(playlists.size + 2).also {
            it += mostRecent
            it += mostRated
            it += playlists
        }
    }
        .loadState()
        .asLiveData()


    private fun builtInPlaylistFlow(category: String) = flow {
        val itemId = MediaId.encode(MediaId.TYPE_TRACKS, category)
        emit(client.getItem(itemId) ?: error("Item with id $itemId should always exist."))
    }

    private fun childrenOf(parentId: String) = client.getChildren(parentId)
        .loadState()
        .asLiveData()
}

private fun <T> Flow<T>.loadState(): Flow<LoadRequest<T>> =
    map { LoadRequest.Success(it) as LoadRequest<T> }
        .onStart { emit(LoadRequest.Pending) }
        .catch { if (it is MediaSubscriptionException) emit(LoadRequest.Error(it)) }