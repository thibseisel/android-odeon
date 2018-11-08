/*
 * Copyright 2018 Thibault Seisel
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

package fr.nihilus.music.client

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import timber.log.Timber

abstract class MediaListViewModel(
    connection: MediaBrowserConnection
) : MediaBrowserViewModel(connection) {

    private val _items = MutableLiveData<List<MediaItem>>()
    private var parentId: String? = null

    /**
     * The list of items that are direct children of the item specified in [loadChildrenOf].
     */
    val items: LiveData<List<MediaItem>> get() = _items

    private val childrenUpdateCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: List<MediaItem>) {
            onChildrenLoaded(parentId, children, Bundle.EMPTY)
        }

        override fun onChildrenLoaded(parentId: String, children: List<MediaItem>, options: Bundle) {
            _items.postValue(children)
        }

        override fun onError(parentId: String) {
            onError(parentId, Bundle.EMPTY)
        }

        override fun onError(parentId: String, options: Bundle) {
            Timber.w("Error while loading children of media ID: %s", parentId)
            _items.value = emptyList()
        }
    }

    /**
     * Trigger a load for direct children of the given [parentMediaId].
     * Loaded items can be observed from [items].
     *
     * Children can be loaded from only one parent at a time.
     *
     * @param parentMediaId The media id of the parent of the items to be loaded.
     */
    fun loadChildrenOf(parentMediaId: String) {
        parentId?.let(connection::unsubscribe)
        parentId = parentMediaId
        connection.subscribe(parentMediaId, childrenUpdateCallback)
    }

    override fun onCleared() {
        parentId?.let(connection::unsubscribe)
        super.onCleared()
    }
}