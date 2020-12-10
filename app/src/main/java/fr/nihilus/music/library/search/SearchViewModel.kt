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

package fr.nihilus.music.library.search

import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.toMediaId
import fr.nihilus.music.core.ui.client.BrowserClient
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

class SearchViewModel @Inject constructor(
    private val client: BrowserClient
) : ViewModel() {

    private val mediaTypeImportance = compareBy<MediaBrowserCompat.MediaItem> { item ->
        when (item.mediaId.toMediaId().type) {
            MediaId.TYPE_TRACKS -> 0
            MediaId.TYPE_PLAYLISTS -> 1
            MediaId.TYPE_ARTISTS -> 2
            MediaId.TYPE_ALBUMS -> 3
            else -> 5
        }
    }

    private val searchQuery = MutableStateFlow("")

    @OptIn(FlowPreview::class)
    val searchResults: LiveData<List<MediaBrowserCompat.MediaItem>> = searchQuery
        .debounce(300)
        .mapLatest { query ->
            if (query.isNotBlank()) {
                val results = client.search(query)
                results.sortedWith(mediaTypeImportance)
            } else {
                emptyList()
            }
        }
        .asLiveData()

    fun search(query: CharSequence) {
        searchQuery.value = query.toString()
    }

    fun play(item: MediaBrowserCompat.MediaItem) {
        viewModelScope.launch {
            client.playFromMediaId(item.mediaId!!)
        }
    }
}
