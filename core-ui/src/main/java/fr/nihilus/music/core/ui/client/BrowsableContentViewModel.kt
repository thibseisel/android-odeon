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

import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.nihilus.music.core.ui.LoadRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch

abstract class BrowsableContentViewModel(
    private val connection: MediaBrowserConnection
) : ViewModel() {
    private val token = MediaBrowserConnection.ClientToken()

    private val _children = MutableLiveData<LoadRequest<List<MediaBrowserCompat.MediaItem>>>()
    val children: LiveData<LoadRequest<List<MediaBrowserCompat.MediaItem>>>
        get() = _children

    init {
        connection.connect(token)
    }

    protected fun observeChildren(parentId: String): Job = viewModelScope.launch {
        _children.postValue(LoadRequest.Pending)
        val subscription = connection.subscribe(parentId)
        try {
            subscription.consumeEach { childrenUpdate ->
                _children.postValue(LoadRequest.Success(childrenUpdate))
            }
        } catch (e: MediaSubscriptionException) {
            _children.value = LoadRequest.Error(e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        connection.disconnect(token)
    }
}