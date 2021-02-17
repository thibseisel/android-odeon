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

package fr.nihilus.music.service.browser.provider

import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.service.MediaContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Provides children of media categories.
 */
internal abstract class ChildrenProvider {

    /**
     * Returns children of a given media as an asynchronous stream of events.
     *
     * @param parentId The media id of the parent in the media tree.
     * This parent should be browsable.
     * @return An asynchronous stream whose latest emitted value is the current list of children
     * of the given parent. A new list of children is emitted whenever it changes.
     * The returned flow throws [NoSuchElementException] if the requested parent
     * is not browsable or is not part of the media tree.
     */
    fun getChildren(parentId: MediaId): Flow<List<MediaContent>> = when (parentId.track) {
        null -> findChildren(parentId)
        else -> flow<Nothing> {
            throw NoSuchElementException("$parentId is not browsable")
        }
    }

    /**
     * Override this function to provide children of a given browsable media.
     *
     * @param parentId The media id of the browsable parent in the media tree.
     * @return asynchronous stream whose last emitted value is the current list of children
     * of the given media restricted by the provided pagination parameters.
     * The returned flow should throw [NoSuchElementException] if the requested parent
     * is not browsable or is not part of the media tree.
     */
    protected abstract fun findChildren(parentId: MediaId): Flow<List<MediaContent>>
}
