/*
 * Copyright 2022 Thibault Seisel
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
import fr.nihilus.music.core.test.coroutines.flow.infiniteFlowOf
import fr.nihilus.music.media.MediaContent
import fr.nihilus.music.media.browser.provider.ChildrenProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Test implementation of [ChildrenProvider] that emits the list of specified children.
 */
internal class FakeChildrenProvider(
    vararg childrenPerParentId: Pair<MediaId, List<MediaContent>>
) : ChildrenProvider() {
    private val children = mapOf(*childrenPerParentId)

    override fun findChildren(parentId: MediaId): Flow<List<MediaContent>> {
        val media = children[parentId]
        return if (media != null) {
            infiniteFlowOf(media)
        } else {
            flow { throw NoSuchElementException("Media $parentId does not exist or is not browsable.") }
        }
    }
}
