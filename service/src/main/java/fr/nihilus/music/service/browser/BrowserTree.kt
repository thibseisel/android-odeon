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

package fr.nihilus.music.service.browser

import android.support.v4.media.MediaBrowserCompat.MediaItem
import fr.nihilus.music.core.media.MediaId
import kotlinx.coroutines.flow.Flow

/**
 * Define the hierarchy of media that can be browsed by remote clients connected to the service.
 * Media are organized in a tree-like structure, with 2 type of nodes:
 * - [Browsable items][MediaItem.isBrowsable] that have children themselves that can be retrieved using [getChildren],
 * - [Playable leafs][MediaItem.isPlayable] that do not have children but can be played.
 */
internal interface BrowserTree {

    /**
     * Retrieve children media of an item with the given [parentId] in the browser tree.
     * The nature of those children depends on the media id of its parent and the internal structure of the media tree.
     * See [MediaId] for more information.
     *
     * If the specified parent is browsable, this returns a list of items that may have children themselves ;
     * otherwise if the parent is not browsable or does not exist,
     * a [NoSuchElementException] is thrown to indicate the absence of children.
     *
     * @param parentId The media id of an item whose children should be loaded.
     * @param options Optional parameters specifying how results should be paginated,
     * or `null` to return all results at once (no pagination).
     *
     * @return The list of children of the media with the id [parentId].
     * @throws NoSuchElementException If the requested parent is not browsable or does not exist.
     */
    suspend fun getChildren(parentId: MediaId, options: PaginationOptions?): List<MediaItem>

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

/**
 * Define the parameters for paginating media items returned by [BrowserTree.getChildren].
 *
 * @param page The index of the page of results to return, `being` the first page.
 * @param size The number of items returned per page.
 */
class PaginationOptions(val page: Int, val size: Int) {

    companion object {

        /**
         * The default index of the returned page of media children when none is specified.
         * This is the index of the first page.
         */
        const val DEFAULT_PAGE_NUMBER = 0

        /**
         * The default number of media items to return in a page when none is specified.
         * All children will be returned in the same page.
         */
        const val DEFAULT_PAGE_SIZE = Int.MAX_VALUE

        /**
         * The minimum accepted value for [PaginationOptions.page].
         * This is the index of the first page.
         */
        internal const val MINIMUM_PAGE_NUMBER = 0

        /**
         * The minimum accepted value for [PaginationOptions.size].
         * This is the minimum of items that can be displayed in a page.
         */
        internal const val MINIMUM_PAGE_SIZE = 1
    }
}