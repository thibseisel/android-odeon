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

import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.service.browser.MediaTree
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Provides children from a pre-configured set of categories.
 * Depending on the requested parent media id, this returns either all configured categories
 * or the children of a specific category.
 */
internal class CategoryChildrenProvider(
    private val categories: Map<String, MediaTree.Category>
) : ChildrenProvider() {

    override fun findChildren(
        parentId: MediaId
    ): Flow<List<MediaItem>> = when (val categoryId = parentId.category) {
        null -> getCategories()
        else -> getCategoryChildren(categoryId)
    }

    private fun getCategoryChildren(categoryId: String?): Flow<List<MediaItem>> =
        categories[categoryId]?.children()
            ?: flow { throw NoSuchElementException("No such category: $categoryId") }

    private fun getCategories(): Flow<List<MediaItem>> = flow {
        val builder = MediaDescriptionCompat.Builder()

        val categoryItems = categories.map { (_, category) ->
            browsable(
                builder,
                category.mediaId.toString(),
                title = category.title,
                subtitle = category.subtitle,
                iconUri = category.iconUri
            )
        }

        emit(categoryItems)
        suspendCancellableCoroutine<Nothing> {}
    }
}