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

import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import fr.nihilus.music.media.MediaId

@DslMarker
internal annotation class MediaTreeDsl

internal fun mediaTree(
    rootId: String,
    builder: MediaTree.Builder.() -> Unit
): MediaTree = MediaTree.Builder(rootId).apply(builder).build()

internal class MediaTree
private constructor(
    private val rootId: String,
    private val types: Map<String, Type>
) {

    private val item: MediaItem by lazy {
        val description = MediaDescriptionCompat.Builder()
            .setMediaId(rootId)
            .build()
        MediaItem(description, MediaItem.FLAG_BROWSABLE)
    }

    suspend fun getChildren(parentId: MediaId): List<MediaItem>? = when {
        parentId.encoded == rootId -> types.map { (_, it) -> it.item }
        parentId.category == null -> types[parentId.type]?.categories()
        parentId.track != null -> null
        else -> types[parentId.type]?.categoryChildren(parentId.category)
    }

    suspend fun getItem(itemId: MediaId): MediaItem? = when {
        itemId.encoded == rootId -> item
        itemId.category == null -> types[itemId.type]?.item
        itemId.track == null -> types[itemId.type]?.categories()?.find { it.mediaId == itemId.encoded }
        else -> types[itemId.type]?.categoryChildren(itemId.category)?.find { it.mediaId == itemId.encoded }
    }

    @MediaTreeDsl
    class Builder(private val rootId: String) {

        private val _typeRegistry = mutableMapOf<String, Type>()
        val types: Map<String, Type>
            get() = _typeRegistry

        fun type(typeName: String, builder: Type.Builder.() -> Unit) {
            _typeRegistry[typeName] = Type.Builder(typeName).apply(builder).build()
        }

        fun build(): MediaTree = MediaTree(rootId, types)
    }
}

internal class Type
private constructor(
    private val type: String,
    private val title: CharSequence?,
    private val subtitle: CharSequence?,
    private val description: CharSequence?,
    private val staticCategories: Map<String, Category>,
    private val categoriesProvider: (suspend () -> List<MediaItem>?)?,
    private val dynamicCategoriesChildrenProvider: (suspend (String) -> List<MediaItem>?)?
) {
    val item: MediaItem by lazy {
        val description = MediaDescriptionCompat.Builder()
            .setMediaId(type)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .build()
        MediaItem(description, MediaItem.FLAG_BROWSABLE)
    }

    suspend fun categories(): List<MediaItem> {
        val dynamicCategories = categoriesProvider?.invoke().orEmpty()

        return ArrayList<MediaItem>(staticCategories.size + dynamicCategories.size).also {
            staticCategories.mapTo(it) { (_, category) -> category.item }
            it.addAll(dynamicCategories)
        }
    }

    suspend fun categoryChildren(categoryName: String): List<MediaItem>? {
        val staticCategory = staticCategories[categoryName]
        return if (staticCategory != null) staticCategory.provider()
        else dynamicCategoriesChildrenProvider?.invoke(categoryName)
    }

    override fun toString(): String = "[$type] {title=$title, subtitle=$subtitle, description=$description}"
    override fun equals(other: Any?): Boolean = other === this || (other is Type && type == other.type)
    override fun hashCode(): Int = type.hashCode()

    @MediaTreeDsl
    class Builder(
        val type: String
    ) {
        private var categoriesProvider: (suspend () -> List<MediaItem>?)? = null
        private val staticCategories = mutableMapOf<String, Category>()
        private var dynamicCategoriesChildrenProvider: (suspend (String) -> List<MediaItem>?)? = null

        var title: CharSequence? = null
        var subtitle: CharSequence? = null
        var description: CharSequence? = null

        fun categories(@MediaTreeDsl provider: suspend () -> List<MediaItem>?) {
            categoriesProvider = provider
        }

        fun category(
            categoryName: String,
            title: CharSequence? = null,
            subtitle: CharSequence? = null,
            description: CharSequence? = null,
            childrenProvider: suspend () -> List<MediaItem>?
        ) {
            staticCategories[categoryName] = Category(
                mediaId = MediaId.encode(type, categoryName),
                title = title,
                subtitle = subtitle,
                description = description,
                provider = childrenProvider
            )
        }

        fun categoryChildren(provider: suspend (categoryName: String) -> List<MediaItem>?) {
            dynamicCategoriesChildrenProvider = provider
        }

        fun build(): Type = Type(type, title, subtitle, description, staticCategories, categoriesProvider, dynamicCategoriesChildrenProvider)

        override fun toString(): String = "[$type] {title=$title, subtitle=$subtitle, description=$description}"
        override fun equals(other: Any?): Boolean = this === other || (other is Type && type == other.type)
        override fun hashCode(): Int = type.hashCode()
    }

    class Category(
        val mediaId: String,
        val title: CharSequence?,
        val subtitle: CharSequence?,
        val description: CharSequence?,
        val provider: suspend () -> List<MediaItem>?
    ) {
        val item: MediaItem by lazy {
            val description = MediaDescriptionCompat.Builder()
                .setMediaId(mediaId)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
                .build()
            MediaItem(description, MediaItem.FLAG_BROWSABLE)
        }

        override fun toString(): String = "[$mediaId] {title=$title, subtitle=$subtitle, description=$description}"
        override fun equals(other: Any?): Boolean = this === other || (other is Category && mediaId == other.mediaId)
        override fun hashCode(): Int = mediaId.hashCode()
    }
}