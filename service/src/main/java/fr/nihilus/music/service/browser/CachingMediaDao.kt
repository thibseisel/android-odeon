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

package fr.nihilus.music.service.browser

import fr.nihilus.music.media.provider.Album
import fr.nihilus.music.media.provider.Artist
import fr.nihilus.music.media.provider.MediaDao
import fr.nihilus.music.media.provider.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Wraps a [MediaDao] to introduce a memory cache layer.
 *
 * When collecting a flow, the latest emitted value is cached in memory.
 * If another subscriber collects the same flow, then it will receive the cached value first.
 * This results in performance gains when collecting the same flow multiple times.
 *
 * This cache can be safely accessed by multiple coroutines.
 *
 * @property delegate The decorated [MediaDao] to which calls should be delegated.
 */
class CachingMediaDao(
    private val delegate: MediaDao
) : MediaDao by delegate {

    private val latestTracks = MutexCache<List<Track>>()
    private val latestAlbums = MutexCache<List<Album>>()
    private val latestArtists = MutexCache<List<Artist>>()

    override val tracks: Flow<List<Track>> = delegate.tracks.cacheLatestTo(latestTracks)

    override val albums: Flow<List<Album>> = delegate.albums.cacheLatestTo(latestAlbums)

    override val artists: Flow<List<Artist>> = delegate.artists.cacheLatestTo(latestArtists)

    private fun <T : Any> Flow<T>.cacheLatestTo(store: MutexCache<T>): Flow<T> =
        onEach { latest ->
            store.put(latest)
        }.onStart {
            val cached = store.get()
            if (cached != null) {
                emit(cached)
            }
        }

    /**
     * A coroutine-safe mutable value.
     * Only one coroutine at a time can read or write the cached value.
     */
    private class MutexCache<T : Any> {
        private val mutex = Mutex()
        private var value: T? = null

        /**
         * Reads the cache value.
         * @return The latest cached value or `null` if no value has been cached yet.
         */
        suspend fun get(): T? = mutex.withLock { value }

        /**
         * Updates the cache, overwriting the previously cached value.
         * @param value The value to be cached.
         */
        suspend fun put(value: T) = mutex.withLock {
            this.value = value
        }
    }
}