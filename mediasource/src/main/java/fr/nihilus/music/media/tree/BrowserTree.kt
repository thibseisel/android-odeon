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

package fr.nihilus.music.media.tree

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import fr.nihilus.music.media.MediaId
import io.reactivex.Flowable

/**
 * Define the hierarchy of media that can be browsed by remote clients connected to the service.
 * Media are organized in a tree-like structure, with 2 type of nodes:
 * - [Browsable items][MediaItem.isBrowsable] that have children themselves that can be retrieved using [getChildren],
 * - [Playable leafs][MediaItem.isPlayable] that do not have children but can be played.
 */
interface BrowserTree {

    companion object {

        /**
         * Used as a boolean extra field to denote that search results from [BrowserTree.search]
         * should only contain playable media.
         */
        const val EXTRA_PLAYABLE_ONLY = "fr.nihilus.music.extra.EXTRA_PLAYABLE_ONLY"
    }

    val updatedParentIds: Flowable<MediaId>

    /**
     * Retrieve children media of an item with the given [parentId] in the browser tree.
     * The nature of those children depends on the media id of its parent and the internal structure of the media tree.
     * See [MediaId] for more information.
     *
     * If the specified parent is browsable, this returns a list of items that may have children themselves ;
     * otherwise, if the parent is not browsable, `null` is returned to indicate the absence of children.
     * Likewise, if the specified media id does not match an existing media in the tree, this also returns `null`.
     *
     * Results can be paginated by specifying the optional [MediaBrowserCompat.EXTRA_PAGE_SIZE]
     * and [MediaBrowserCompat.EXTRA_PAGE] as [options].
     *
     * @param parentId The media id of an item whose children should be loaded.
     * @param options An optional bundle of client-specified options.
     * @return The list of children of the media with the id [parentId],
     * or `null` if that media is not browsable or doesn't exist.
     */
    suspend fun getChildren(parentId: MediaId, options: Bundle?): List<MediaItem>?

    /**
     * Retrieve an item identified by the specified [itemId] from the media tree.
     * If no item matches that media id, `null` is returned.
     *
     * @param itemId The media id of the item to retrieve.
     * @return An item with the same media id as the requested one, or `null` if no item matches.
     */
    suspend fun getItem(itemId: MediaId): MediaItem?

    /**
     * Search the browser tree for media items whose title matches the supplied [query].
     * What exactly should be searched is determined by the type of the [SearchQuery].
     *
     * @param query The client-provided search query.
     * @return A list of media items matching the search criteria.
     */
    suspend fun search(query: SearchQuery): List<MediaItem>
}