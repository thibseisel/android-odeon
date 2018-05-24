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

package fr.nihilus.music.media.repo

import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaMetadataCompat
import fr.nihilus.music.di.ServiceScoped
import fr.nihilus.music.media.browseCategoryOf
import fr.nihilus.music.media.browseHierarchyOf
import fr.nihilus.music.media.builtin.BuiltinItem
import fr.nihilus.music.media.cache.MediaCache
import fr.nihilus.music.media.source.MusicDao
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject

/**
 * A Music Repository that tries to fetch items and metadata from cache,
 * and then from the data source if not available.
 */
@ServiceScoped
internal class CachedMusicRepository
@Inject constructor(
    private val mediaDao: MusicDao,
    private val mediaCache: MediaCache,
    private val builtIns: Map<String, @JvmSuppressWildcards BuiltinItem>
) : MusicRepository {

    override fun getMediaItems(parentMediaId: String): Single<List<MediaItem>> {
        // Get the "true" parent in case the passed media id is a playable item
        val trueParent = browseCategoryOf(parentMediaId)

        val cachedItems = mediaCache[trueParent]
        if (cachedItems.isNotEmpty()) {
            return Single.just(cachedItems)
        }

        val parentHierarchy = browseHierarchyOf(trueParent)
        // Search the root media id in built-in items
        // Notify an error if no built-in is found
        val builtIn = builtIns[parentHierarchy[0]]
                ?: return Single.error(::UnsupportedOperationException)
        val items = builtIn.getChildren(trueParent).toList()

        return items.doOnSuccess {
            mediaCache.put(trueParent, it)
        }
    }

    // TODO Calculate diff with the cache (if entry is present) to emit more precise Media IDs to subscribers
    override fun getMediaChanges(): Observable<String> = mediaDao.getMediaChanges().doOnNext {
        val parentCategory = browseCategoryOf(it)
        mediaCache.remove(parentCategory)
    }

    override fun getMetadata(musicId: String): Single<MediaMetadataCompat> =
        mediaDao.findTrack(musicId).toSingle()

    override fun clear() = mediaCache.clear()
}
