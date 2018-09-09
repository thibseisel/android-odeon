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

/**
 * A cache that stores media items and metadata for a later use.
 * Using a cache is a convenient way to decrease overall loading times when the cached data
 * comes from storage or network.
 *
 * Implementations can use any underlying storage (in-memory, disk...)
 * or caching strategy (eviction, cache-aside...)
 * provided it is faster than fetching items from their original source.
 */
internal interface MediaCache {

    /**
     * Put media items in the cache. Those items could then be retrieved via `get(mediaId)`.
     * Depending on the cache implementation, older items may be removed
     * as other set of media items are put into the cache.
     *
     * @param mediaId unique identifier of the parent that contains the media items
     */
    fun put(mediaId: String, items: List<MediaBrowserCompat.MediaItem>)

    /**
     * Retrieve a set of media items stored in the cache.
     * Depending on the cache implementation, the requested items may or may not be in the cache,
     * even if it has been saved in [put] earlier.
     * In case items are absent, an empty list will be returned.
     *
     * @param mediaId of the parent that contains the media items
     * @return a list of those media items, or an empty list if not in cache
     */
    operator fun get(mediaId: String): List<MediaBrowserCompat.MediaItem>

    /**
     * Removes a set of media items from the cache.
     * Subsequent calls to [get] for the same [mediaId] are then guaranteed to return an empty list.
     *
     * @param mediaId The media id of the parent that contains the media items.
     */
    fun remove(mediaId: String)

    /**
     * Remove all items stored in the cache, effectively releasing all reference to them.
     */
    fun clear()
}

