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

package fr.nihilus.music.service

import android.support.v4.media.MediaBrowserCompat.MediaItem
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.service.browser.BrowserTree
import fr.nihilus.music.service.browser.PaginationOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.broadcast
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * Manage active media subscriptions, observing changes to the media tree.
 */
internal interface SubscriptionManager {

    /**
     * An asynchronous stream of events that notifies observers of changes to the structure of the media tree.
     */
    val updatedParentIds: Flow<MediaId>

    /**
     * Load the current children node of the given parent node in the media tree.
     *
     * Results returned by this function are paginated:
     * given a parent with 100 children and a [PaginationOptions.size] of 20,
     * when requesting the 3rd page ([PaginationOptions.page] = `3`)
     * this will return children from index `40` to `59` (inclusive).
     *
     * @param parentId The browsable parent of the children to load.
     * @param options Optional pagination parameters to restrict the number of returned children.
     * @return Current children of the requested parent.
     * @throws NoSuchElementException if the requested parent is not browsable
     * or is not part of the media tree.
     */
    suspend fun loadChildren(parentId: MediaId, options: PaginationOptions?): List<MediaItem>

    /**
     * Get a single item from the media tree.
     *
     * @param itemId The media id of that item in the media tree.
     * @return The requested item, or `null` if that item is not part of the media tree.
     */
    suspend fun getItem(itemId: MediaId): MediaItem?
}

private const val MAX_ACTIVE_SUBSCRIPTIONS = 5

@ServiceScoped
internal class SubscriptionManagerImpl @Inject constructor(
    serviceScope: CoroutineScope,
    private val tree: BrowserTree
) : SubscriptionManager {

    private val scope = serviceScope + SupervisorJob()

    private val mutex = Mutex()
    private val cachedSubscriptions = mutableMapOf<MediaId, BroadcastChannel<List<MediaItem>>>()
    private val activeSubscriptions = BroadcastChannel<Flow<MediaId>>(Channel.BUFFERED)

    override val updatedParentIds: Flow<MediaId> = activeSubscriptions.asFlow()
        .map { it.drop(1) }
        .scan(emptyList()) { observed: List<Flow<MediaId>>, new: Flow<MediaId> ->
            when {
                observed.size < MAX_ACTIVE_SUBSCRIPTIONS -> observed + new
                else -> ArrayList<Flow<MediaId>>(MAX_ACTIVE_SUBSCRIPTIONS).apply {
                    addAll(observed.drop(1))
                    add(new)
                }
            }
        }.flatMapLatest { it.merge() }

    override suspend fun loadChildren(
        parentId: MediaId,
        options: PaginationOptions?
    ): List<MediaItem> {
        var subscription = mutex.withLock { cachedSubscriptions[parentId] }
        if (subscription == null) {
            subscription = tree.getChildren(parentId).cacheLatestIn(scope)
            mutex.withLock { cachedSubscriptions[parentId] = subscription }

            val childrenUpdateFlow = subscription.asFlow().map { parentId }
            activeSubscriptions.send(childrenUpdateFlow)
        }

        val children = subscription.consume { receive() }
        return if (options == null) children else {
            val fromIndex = options.page * options.size
            val toIndexExclusive = (fromIndex + options.size)

            return if (fromIndex < children.size && toIndexExclusive <= children.size) {
                children.subList(fromIndex, toIndexExclusive)
            } else emptyList()
        }
    }

    override suspend fun getItem(itemId: MediaId): MediaItem? = tree.getItem(itemId)

    private fun <T> Flow<T>.cacheLatestIn(scope: CoroutineScope): BroadcastChannel<T> {
        return scope.broadcast(capacity = Channel.CONFLATED, start = CoroutineStart.LAZY) {
            this@cacheLatestIn.collect { send(it) }
        }
    }
}