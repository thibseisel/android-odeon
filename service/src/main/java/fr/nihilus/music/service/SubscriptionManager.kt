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

import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.service.browser.PaginationOptions
import kotlinx.coroutines.flow.Flow

/**
 * The maximum number of subscriptions that can be maintained by [SubscriptionManager].
 */
internal const val MAX_ACTIVE_SUBSCRIPTIONS = 5

/**
 * Manage active media subscriptions, observing changes to the media tree.
 */
internal interface SubscriptionManager {

    /**
     * An asynchronous stream of events that notifies observers of changes
     * to the structure of the media tree. The value associated with each event is the [MediaId]
     * of the parent whose children has changed.
     *
     * Changes to a parent's children are only notified if its children have already been loaded
     * via [loadChildren]. At most [MAX_ACTIVE_SUBSCRIPTIONS] are observed at the same time.
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
     * Loading children of a given parent makes it possible to be notified when children of
     * that parent have changed in some way via [updatedParentIds].
     * A most [MAX_ACTIVE_SUBSCRIPTIONS] parents can be observed at the same time.
     * Once that limit has been reached, the oldest parent subscription is disposed,
     * i.e. changes to its children will no longer be notified until loaded again.
     * If children of a parent that is currently being observed are reloaded,
     * then its subscription is kept active longer.
     *
     * @param parentId The browsable parent of the children to load.
     * @param options Optional pagination parameters to restrict the number of returned children.
     * @return Current children of the requested parent.
     * @throws NoSuchElementException if the requested parent is not browsable
     * or is not part of the media tree.
     */
    suspend fun loadChildren(parentId: MediaId, options: PaginationOptions?): List<MediaContent>

    /**
     * Get a single item from the media tree.
     *
     * @param itemId The media id of that item in the media tree.
     * @return The requested item, or `null` if that item is not part of the media tree.
     */
    suspend fun getItem(itemId: MediaId): MediaContent?
}