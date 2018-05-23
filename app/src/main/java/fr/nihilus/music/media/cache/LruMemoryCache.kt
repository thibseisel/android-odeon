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

package fr.nihilus.music.media.cache

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.util.LruCache
import fr.nihilus.music.di.ServiceScoped
import javax.inject.Inject

/**
 * A MusicCache implementation that keeps only the latest media items in memory.
 */
@ServiceScoped
internal class LruMemoryCache
@Inject constructor() : MusicCache {

    private val itemsCache = LruCache<String, List<MediaBrowserCompat.MediaItem>>(5)

    override fun put(mediaId: String, items: List<MediaBrowserCompat.MediaItem>) {
        itemsCache.put(mediaId, items)
    }

    override fun get(mediaId: String) = itemsCache.get(mediaId) ?: emptyList()

    override fun remove(mediaId: String) {
        itemsCache.remove(mediaId)
    }

    override fun clear() {
        itemsCache.evictAll()
    }
}