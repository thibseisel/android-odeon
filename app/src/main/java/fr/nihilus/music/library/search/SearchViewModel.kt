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

package fr.nihilus.music.library.search

import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import fr.nihilus.music.core.ui.base.BaseViewModel
import fr.nihilus.music.core.ui.client.MediaBrowserConnection
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

class SearchViewModel
@Inject constructor(
    private val connection: MediaBrowserConnection
) : BaseViewModel() {

    private val _searchResults = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
    val searchResults: LiveData<List<MediaBrowserCompat.MediaItem>>
        get() = _searchResults

    private val searchActor = Channel<String>(Channel.CONFLATED).also { channel ->
        channel.consumeAsFlow()
            .filter { it.isNotBlank() && it.length >= 2 }
            .distinctUntilChanged()
            .mapLatest { query ->
                delay(200)
                connection.search(query)
            }
            .onEach { _searchResults.value = it }
            .launchIn(this)
    }

    fun search(query: CharSequence) {
        searchActor.offer(query.toString())
    }

    fun play(item: MediaBrowserCompat.MediaItem) {
        launch {
            connection.playFromMediaId(item.mediaId!!)
        }
    }
}