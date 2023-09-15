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

package fr.nihilus.music.media.browser

import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.media.AudioTrack
import fr.nihilus.music.media.MediaCategory
import fr.nihilus.music.media.MediaContent
import kotlinx.coroutines.flow.Flow

/**
 * Define the hierarchy of media that can be browsed by remote clients connected to the service.
 * Media are organized in a tree-like structure, with 2 type of nodes:
 * - [Browsable items][MediaCategory] that have children themselves that can be retrieved using [getChildren],
 * - [Playable leafs][AudioTrack] that do not have children but can be played.
 */
interface BrowserTree {

    /**
     * Retrieve children media of a media with the given [parentId] in the browser tree.
     * The nature of those children depends on the media id of its parent and the internal structure of the media tree.
     * See [MediaId] for more information.
     *
     * @param parentId The media id of a media whose children should be loaded.
     *
     * @return An asynchronous stream whose latest emitted value is the current list of children
     * of the specified parent node (whose media id is [parentId]) in the media tree.
     * A new list of children is emitted whenever it has changed.
     * The returned flow will throw [NoSuchElementException] if the requested parent node
     * is not browsable or not part of the media tree.
     */
    fun getChildren(parentId: MediaId): Flow<List<MediaContent>>

    /**
     * Retrieve an item identified by the specified [itemId] from the media tree.
     * If no item matches that media id, `null` is returned.
     *
     * @param itemId The media id of the item to retrieve.
     * @return An item with the same media id as the requested one, or `null` if no item matches.
     */
    suspend fun getItem(itemId: MediaId): MediaContent?
}
