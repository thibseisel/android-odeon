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

package fr.nihilus.music.core.ui.client

import android.support.v4.media.MediaBrowserCompat.MediaItem
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.nihilus.music.core.ui.LoadRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*

abstract class BrowsableContentViewModel(
    private val connection: MediaBrowserConnection
) : ViewModel() {
    private val token = MediaBrowserConnection.ClientToken()

    private val _children = MutableLiveData<LoadRequest<List<MediaItem>>>()
    val children: LiveData<LoadRequest<List<MediaItem>>> = _children

    init {
        connection.connect(token)
    }

    protected fun observeChildren(parentId: String): Job = connection.subscribe(parentId)
        .map { LoadRequest.Success(it) as LoadRequest<List<MediaItem>> }
        .onStart { emit(LoadRequest.Pending) }
        .catch { if (it is MediaSubscriptionException) emit(LoadRequest.Error(it)) }
        .onEach { _children.postValue(it) }
        .launchIn(viewModelScope)

    override fun onCleared() {
        super.onCleared()
        connection.disconnect(token)
    }
}