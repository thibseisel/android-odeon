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

import android.net.Uri
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import androidx.media.MediaBrowserServiceCompat.BrowserRoot
import fr.nihilus.music.media.MediaId

/**
 * Mark declarations that are used to define the structure of a media browser tree.
 * Annotated elements cannot access the properties of an implicit receiver
 * (i.e. the properties of parent nodes).
 */
@DslMarker
internal annotation class MediaTreeDsl

/**
 * Build an async tree-like structure for browsing media available on the device.
 *
 * @param rootId The media of the root element.
 * This should match the [rootId of the BrowserRoot][BrowserRoot.getRootId] supplied to clients.
 * @param builder Block for defining the available types.
 * @return An immutable tree-like structure for browsing media.
 */
internal fun mediaTree(
    rootId: String,
    builder: MediaTree.Builder.() -> Unit
): MediaTree = MediaTree.Builder(rootId).apply(builder).build()

/**
 * An async tree-like structure for retrieving available media.
 * Media information can be retrieved in 2 different ways:
 * 1. individually by their media id,
 * 2. in group by specifying the media id of their parent.
 *
 * The media tree respects the structure of media id defined in [MediaId]: `type/category|track`.
 *
 * @constructor Create the immutable browser tree.
 * @param rootId The media id of the tree root. This should match the id of a [BrowserRoot].
 * @param rootName The title to be given to the root element.
 * @param types The children of the root node. Each child should have a type-only media id.
 */
internal class MediaTree
private constructor(
    private val rootId: String,
    private val rootName: String?,
    private val types: Map<String, Type>
) {
    /**
     * The media item representing the root of this media tree.
     */
    private val rootItem: MediaItem
        get() {
            val description = MediaDescriptionCompat.Builder()
                .setMediaId(rootId)
                .setTitle(rootName)
                .build()
            return MediaItem(description, MediaItem.FLAG_BROWSABLE)
        }

    /**
     * Retrieve children of a browsable node with the specified [media id][parentId].
     * Results returned by this function are paginated:
     * given a parent with 100 children, a [pageSize] of 20, when requesting the 3rd page
     * this will return children from index `40` to `59` (inclusive).
     *
     * @param parentId The media id of the parent node.
     * @param pageNumber The index of the page to be returned.
     * This should be greater than or equal to `0`.
     * If the page number goes beyond the actual number of children, then no children are returned.
     * @param pageSize The number of children per page.
     * This should be greater than or equal to `1`.
     * If the page size is greater than the number of children, all children are returned.
     * You may pass [Int.MAX_VALUE] to get all children.
     *
     * @return The children of the specified parent node, or `null` if no such parent exists.
     * The returned list contains at most [pageSize] items.
     */
    suspend fun getChildren(parentId: MediaId, pageNumber: Int, pageSize: Int): List<MediaItem>? {
        val fromIndex = pageSize * pageNumber

        return when {
            parentId.encoded == rootId -> types.map { (_, type) -> type.item }
            parentId.category == null -> types[parentId.type]?.categories()
            parentId.track != null -> null
            else -> types[parentId.type]?.categoryChildren(parentId.category!!, fromIndex, pageSize)
        }
    }

    /**
     * Retrieve the information of an rootItem in the media tree given its media id.
     *
     * @param itemId The media id of the rootItem to retrieve.
     * @return A media rootItem having the specified [itemId], or `null` if no such rootItem exists.
     */
    suspend fun getItem(itemId: MediaId): MediaItem? = when {
        itemId.encoded == rootId -> rootItem
        itemId.category == null -> types[itemId.type]?.item
        itemId.track == null -> types[itemId.type]?.categories()?.find { it.mediaId == itemId.encoded }
        else -> types[itemId.type]?.categoryChildren(itemId.category!!)?.find { it.mediaId == itemId.encoded }
    }

    /**
     * Define a DSL for creating the nodes of a [MediaTree].
     *
     * @constructor Create the media tree builder with the media id of its the topmost element.
     * You should not instantiate this class directly ; use the [mediaTree] function instead.
     *
     * @param rootId The media id of the root element.
     * This should match the [rootId of the BrowserRoot][BrowserRoot.getRootId] supplied to clients.
     */
    @MediaTreeDsl
    class Builder(private val rootId: String) {
        private val typeRegistry = mutableMapOf<String, Type>()

        /**
         * The display name of the media item representing the root node of the media tree.
         * It is `null` by default.
         */
        var rootName: String? = null

        /**
         * Define a _type_ node as a children of the root.
         *
         * @param typeId The unique identifier of the type to be created.
         * @param builder Block for defining properties of the newly created type.
         */
        fun type(typeId: String, builder: Type.Builder.() -> Unit) {
            check(typeId !in typeRegistry) { "Duplicate type: $typeId" }
            typeRegistry[typeId] = Type.Builder(typeId).apply(builder).build()
        }

        /**
         * Instantiate the media tree.
         * This should not be used from the DSL.
         */
        fun build(): MediaTree = MediaTree(rootId, rootName, typeRegistry)
    }
}

/**
 * A node from a [MediaTree] that groups media by type.
 * The media id of a type node should be _type-only_,
 * which means it has neither a category nor a track part, as explained in [MediaId].
 *
 * A type node may only have browsable categories.
 * Also, each category may have its own children, and if it does,
 * all its children should not be browsable.
 *
 * @constructor
 * @param type The name of the type this node represents, which is used as its media id.
 * @param title The display title of the media item associated with this type.
 * @param subtitle The display subtitle of the media item associated with this type.
 * @param description The display description of the media item associated with this type.
 * @param staticCategories Set of categories that are always listed as children of this type.
 * @param categoriesProvider An async function for querying dynamic categories of this type.
 * @param categoriesChildrenProvider An async function for querying children of a dynamic category
 * given the category's media id.
 */
internal class Type
private constructor(
    private val type: String,
    private val title: CharSequence?,
    private val subtitle: CharSequence?,
    private val description: CharSequence?,
    private val staticCategories: Map<String, Category>,
    private val categoriesProvider: (suspend () -> List<MediaItem>?)?,
    private val categoriesChildrenProvider: (suspend (categoryId: String, firstIndex: Int, count: Int) -> List<MediaItem>?)?
) {
    /**
     * The media item representing this type in the media tree.
     */
    val item: MediaItem
        get() {
            val description = MediaDescriptionCompat.Builder()
                .setMediaId(type)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
                .build()
            return MediaItem(description, MediaItem.FLAG_BROWSABLE)
        }

    /**
     * Load children categories of this type.
     *
     * @return A list of all direct children of this type.
     */
    suspend fun categories(): List<MediaItem> {
        val dynamicCategories = categoriesProvider?.invoke() ?: emptyList()

        return ArrayList<MediaItem>(staticCategories.size + dynamicCategories.size).also {
            staticCategories.mapTo(it) { (_, category) -> category.item }
            it.addAll(dynamicCategories)
        }
    }

    /**
     * Load children of a category with the specified [categoryId].
     * This function supports pagination: among all available media items in that category,
     * only those between [fromIndex] and [count] (exclusive).
     *
     * @param categoryId The identifier of the parent category.
     * @param fromIndex The index of the first element included in the page results, `0` by default.
     * @param count The maximum number of items in the returned page. No maximum by default.
     * @return The children of the specified category, or `null` if this type has not such category.
     */
    suspend fun categoryChildren(
        categoryId: String,
        fromIndex: Int = 0,
        count: Int = Int.MAX_VALUE
    ): List<MediaItem>? {
        val staticCategory = staticCategories[categoryId]
        return if (staticCategory != null) staticCategory.provider(fromIndex, count)
        else categoriesChildrenProvider?.invoke(categoryId, fromIndex, count)
    }

    override fun toString(): String = "[$type] {title=$title, subtitle=$subtitle}"
    override fun equals(other: Any?): Boolean = other === this || (other is Type && type == other.type)
    override fun hashCode(): Int = type.hashCode()

    /**
     * Define a DSL for adding _type nodes_ to the media tree.
     *
     * @constructor Create a type builder by specifying its identifier.
     * You should not instantiate this class directly ; use the [MediaTree.Builder.type] function instead.
     *
     * @param typeId The unique identifier of the newly created type. This is used as its media id.
     */
    @MediaTreeDsl
    class Builder(
        private val typeId: String
    ) {
        private val staticCategories = mutableMapOf<String, Category>()
        private var categoriesProvider: (suspend () -> List<MediaItem>?)? = null
        private var dynamicCategoriesChildrenProvider: (suspend (String, Int, Int) -> List<MediaItem>?)? = null

        /**
         * The display title for the media item representing this type.
         * This is `null` by default.
         */
        var title: CharSequence? = null

        /**
         * The display subtitle for the media item representing this type.
         * This is `null` by default.
         */
        var subtitle: CharSequence? = null

        /**
         * The display description for the media item representing this type.
         */
        var description: CharSequence? = null

        /**
         * Define how children categories of the newly created type should be retrieved.
         *
         * @param provider An async function called to load children categories.
         * Categories loaded by this function should have a media id whose _type_ part matches
         * their parent's [typeId].
         */
        fun categories(@MediaTreeDsl provider: suspend () -> List<MediaItem>) {
            categoriesProvider = provider
        }

        /**
         * Define a static category, direct child of this type.
         * Unlike [dynamic categories][categories], static categories are always part of the tree.
         *
         * @param categoryId Unique identifier of the category.
         * This contributes to the media id of the category.
         * @param title The display title of the media item representing this category.
         * @param subtitle The display subtitle of the media item representing this category.
         * @param description The display description of the media item representing this category.
         * @param iconUri An uri pointing to an icon representing this category.
         * @param children An async function called to load children of that category.
         * It should return the items between the specified indices,
         * or `null` if the category has no children.
         */
        fun category(
            categoryId: String,
            title: CharSequence? = null,
            subtitle: CharSequence? = null,
            description: CharSequence? = null,
            iconUri: Uri? = null,
            children: suspend (fromIndex: Int, count: Int) -> List<MediaItem>?
        ) {
            check(categoryId !in staticCategories) { "Duplicate category: $categoryId" }
            staticCategories[categoryId] = Category(
                mediaId = MediaId.encode(typeId, categoryId),
                title = title,
                subtitle = subtitle,
                description = description,
                iconUri = iconUri,
                provider = children
            )
        }

        /**
         * Define how children of [dynamic categories][categories] should be retrieved.
         *
         * @param provider An async function called to load children of a category given its id.
         * Items loaded by this function should:
         * - be playable and not browsable
         * - have a full media id whose type part matches [typeId],
         * a category part matching the given category id and an unique track part.
         * Also, if the specified category id does not match an existing category
         * then this function should return `null`.
         */
        fun categoryChildren(provider: suspend (categoryId: String, firstIndex: Int, count: Int) -> List<MediaItem>?) {
            dynamicCategoriesChildrenProvider = provider
        }

        /**
         * Instantiate the type definition.
         * This should not be used from the DSL.
         */
        fun build(): Type = Type(
            typeId,
            title,
            subtitle,
            description,
            staticCategories,
            categoriesProvider,
            dynamicCategoriesChildrenProvider
        )
    }

    /**
     * A node from the media tree that defines a subset of media in a given type.
     * Categories represented by instances of this class are static ;
     * those are always part of the media tree as opposed to dynamic categories
     * that are determined from the available media.
     *
     * The media id of a category is composed of its parent type plus its own category id,
     * as explained in [MediaId].
     * A category node may only have playable, non-browsable children.
     *
     * @constructor Create the definition of a static category.
     * You should not create instances of this class directly ; use [Type.Builder] instead.
     *
     * @param mediaId The media id of this category.
     * @param title The display title of the media item representing this category.
     * @param subtitle The display subtitle of the media item representing this category.
     * @param description The display description of the media item representing this category.
     * @param iconUri An uri pointing to an icon representing this category.
     * @param provider An async function for querying the children of this category.
     */
    class Category(
        val mediaId: String,
        val title: CharSequence?,
        val subtitle: CharSequence?,
        val description: CharSequence?,
        val iconUri: Uri?,
        val provider: suspend (fromIndex: Int, count: Int) -> List<MediaItem>?
    ) {
        /**
         * The media item representing this static category.
         */
        val item: MediaItem
            get() {
                val description = MediaDescriptionCompat.Builder()
                    .setMediaId(mediaId)
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setDescription(description)
                    .setIconUri(iconUri)
                    .build()
                return MediaItem(description, MediaItem.FLAG_BROWSABLE)
            }

        override fun toString(): String = "[$mediaId] {title=$title, subtitle=$subtitle}"
        override fun equals(other: Any?): Boolean = this === other || (other is Category && mediaId == other.mediaId)
        override fun hashCode(): Int = mediaId.hashCode()
    }
}