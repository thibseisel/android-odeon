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

import androidx.collection.LruCache
import dagger.hilt.android.scopes.ServiceScoped
import fr.nihilus.music.core.context.AppDispatchers
import fr.nihilus.music.core.flow.dematerialize
import fr.nihilus.music.core.flow.materialize
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.media.MediaContent
import fr.nihilus.music.media.browser.BrowserTree
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * Implementation of [SubscriptionManager] that caches at most [MAX_ACTIVE_SUBSCRIPTIONS].
 *
 * @param serviceScope The lifecycle of the service that owns this manager.
 * @param tree The source of media metadata.
 */
@ServiceScoped
internal class CachingSubscriptionManager @Inject constructor(
    @ServiceCoroutineScope serviceScope: CoroutineScope,
    private val tree: BrowserTree,
    private val dispatchers: AppDispatchers
) : SubscriptionManager {

    private val scope =
        serviceScope + SupervisorJob(serviceScope.coroutineContext.job) + CoroutineName("SubscriptionSupervisor")

    private val mutex = Mutex()
    private val cachedSubscriptions = LruSubscriptionCache()
    private val _updatedParentIds = MutableSharedFlow<MediaId>()

    override val updatedParentIds: Flow<MediaId> = _updatedParentIds.asSharedFlow()

    override suspend fun loadChildren(
        parentId: MediaId,
        options: PaginationOptions?
    ): List<MediaContent> {
        val subscription = mutex.withLock {
            cachedSubscriptions.get(parentId)
                ?: createSubscription(parentId).also { cachedSubscriptions.put(parentId, it) }
        }

        try {
            val children = subscription.children.first()
            return applyPagination(children, options)
        } catch (failure: Exception) {
            cachedSubscriptions.remove(parentId)
            throw failure
        }
    }

    private fun applyPagination(
        children: List<MediaContent>,
        options: PaginationOptions?
    ): List<MediaContent> = when (options) {
        null -> children
        else -> {
            val fromIndex = options.page * options.size
            val toIndexExclusive = (fromIndex + options.size)

            if (fromIndex < children.size && toIndexExclusive <= children.size) {
                children.subList(fromIndex, toIndexExclusive)
            } else {
                emptyList()
            }
        }
    }

    private fun createSubscription(parentId: MediaId): Subscription {
        val subscriptionJob = Job(parent = scope.coroutineContext.job)
        val subscriptionScope = scope + subscriptionJob + CoroutineName("Subscription_$parentId")

        val liveChildren = tree.getChildren(parentId)
            .buffer(Channel.CONFLATED)
            .flowOn(dispatchers.Default)
            .materialize()
            .shareIn(subscriptionScope, SharingStarted.Lazily, 1)
            .dematerialize()
        liveChildren
            .drop(1)
            .catch { if (it !is Exception) throw it }
            .onEach { _updatedParentIds.emit(parentId) }
            .launchIn(scope)
        return Subscription(liveChildren, subscriptionJob)
    }

    override suspend fun getItem(itemId: MediaId): MediaContent? {
        val parentId = when {
            itemId.track != null -> itemId.copy(track = null)
            itemId.category != null -> itemId.copy(category = null)
            else -> MediaId(MediaId.TYPE_ROOT)
        }

        val parentSubscription = mutex.withLock { cachedSubscriptions[parentId] }

        return if (parentSubscription != null) {
            val children = parentSubscription.children.first()
            children.find { it.id == itemId }
        } else withContext(dispatchers.Default) {
            tree.getItem(itemId)
        }
    }

    private class LruSubscriptionCache : LruCache<MediaId, Subscription>(MAX_ACTIVE_SUBSCRIPTIONS) {
        override fun entryRemoved(
            evicted: Boolean,
            key: MediaId,
            oldValue: Subscription,
            newValue: Subscription?
        ) = oldValue.consumeJob.cancel("Evicted from cache")
    }
}

private data class Subscription(
    val children: Flow<List<MediaContent>>,
    val consumeJob: Job
)
