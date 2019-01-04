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
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem

@Deprecated("This LiveData implementation has flaws: " +
        "its value is not updated when it is not observed, resulting is lost value updates. " +
        "Use the ReceiveChannel returned by MediaBrowserConnection.subscribe instead.")
class SubscriptionLiveData(
    private val browser: MediaBrowserCompat,
    private val parentMediaId: String
) : LiveData<List<MediaItem>>() {

    private val callback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String,children: List<MediaItem>) {
            value = children
        }

        override fun onChildrenLoaded(parentId: String, children: List<MediaItem>, options: Bundle) {
            value = children
        }

        override fun onError(parentId: String) {
            value = null
        }

        override fun onError(parentId: String, options: Bundle) {
            value = null
        }
    }

    override fun onActive() {
        browser.subscribe(parentMediaId, callback)
    }

    override fun onInactive() {
        browser.unsubscribe(parentMediaId, callback)
    }
}