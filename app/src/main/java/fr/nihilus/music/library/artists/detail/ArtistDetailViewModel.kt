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

package fr.nihilus.music.library.artists.detail

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.v4.media.MediaBrowserCompat
import fr.nihilus.music.base.BaseViewModel
import fr.nihilus.music.client.MediaBrowserConnection
import fr.nihilus.music.ui.LoadRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import javax.inject.Inject

class ArtistDetailViewModel
@Inject constructor(
    private val connection: MediaBrowserConnection
) : BaseViewModel() {

    private val _artistChildren = MutableLiveData<LoadRequest<List<MediaBrowserCompat.MediaItem>>>()
    val artistChildren: LiveData<LoadRequest<List<MediaBrowserCompat.MediaItem>>>
        get() = _artistChildren

    private var loadChildrenJob: Job? = null

    fun loadChildrenOfArtist(artist: MediaBrowserCompat.MediaItem) {
        loadChildrenJob?.cancel()
        _artistChildren.postValue(LoadRequest.Pending())

        launch {
            connection.subscribe(artist.mediaId!!).consumeEach { childrenUpdate ->
                _artistChildren.postValue(LoadRequest.Success(childrenUpdate))
            }
        }
    }

    fun playMedia(track: MediaBrowserCompat.MediaItem) {
        launch {
            connection.playFromMediaId(track.mediaId!!)
        }
    }
}