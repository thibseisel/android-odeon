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

import android.net.Uri
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.media.MediaCategory
import fr.nihilus.music.media.MediaContent
import fr.nihilus.music.media.browser.provider.CategoryChildrenProvider
import fr.nihilus.music.media.browser.provider.ChildrenProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Mark declarations that are used to define the structure of a media browser tree.
 * Annotated elements cannot access the properties of an implicit receiver
 * (i.e. the properties of parent nodes).
 */
@DslMarker
internal annotation class MediaTreeDsl

/**
 * An async tree-like structure for retrieving available media.
 * Media information can be retrieved in 2 different ways:
 * 1. individually by their media id,
 * 2. in group by specifying the media id of their parent.
 *
 * The media tree respects the structure of media id defined in [MediaId]: `type/category|track`.
 *
 * @constructor Create the immutable browser tree.
 * @param rootId The media id of the tree root. This should match the id of the `BrowserRoot`.
 * @param rootName The title to be given to the root element.
 * @param types The children of the root node. Each child should have a type-only media id.
 */
internal class MediaTree
private constructor(
    private val rootId: MediaId,
    private val rootName: String,
    private val types: Map<String, Type>
) {

    companion object {
        /**
         * Build an async tree-like structure for browsing media available on the device.
         *
         * @param rootId The media of the root element. This should match the rootId of the
         * `BrowserRoot` supplied to clients.
         * @param rootName The display name of the media item representing the root node of the media tree.
         * @param block Block for defining the available types.
         * @return An immutable tree-like structure for browsing media.
         */
        @MediaTreeDsl
        operator fun invoke(
            rootId: MediaId,
            rootName: String,
            block: Builder.() -> Unit
        ): MediaTree = Builder(rootId, rootName).apply(block).build()
    }

    /**
     * The media item representing the root of this media tree.
     */
    private val rootItem: MediaCategory
        get() = MediaCategory(
            id = rootId,
            title = rootName
        )

    /**
     * Retrieve children of a browsable node with the specified [media id][parentId].
     *
     * @param parentId The media id of the parent node.
     * @return An asynchronous stream whose latest emitted value is the current list of children
     * of the specified parent node (whose media id is [parentId]) in the media tree.
     * A new list of children is emitted whenever it has changed.
     * The returned flow will throw [NoSuchElementException] if the requested parent node
     * is not browsable or not part of the media tree.
     */
    fun getChildren(parentId: MediaId): Flow<List<MediaContent>> {
        val (typeId, categoryId, trackId) = parentId

        return when {
            parentId == rootId -> rootChildren()
            categoryId == null -> requireType(typeId).categories()
            trackId != null -> throw NoSuchElementException("$parentId is not browsable")
            else -> requireType(typeId).categoryChildren(categoryId)
        }
    }

    private fun rootChildren() = flow {
        emit(types.map { (_, type) -> type.item })
        suspendCancellableCoroutine<Nothing> {}
    }

    private fun requireType(typeId: String) = types[typeId]
        ?: throw NoSuchElementException("No such media type: $typeId")

    /**
     * Retrieve the information of an rootItem in the media tree given its media id.
     *
     * @param itemId The media id of the rootItem to retrieve.
     * @return A media rootItem having the specified [itemId], or `null` if no such rootItem exists.
     */
    suspend fun getItem(itemId: MediaId): MediaContent? {
        val (typeId, categoryId, trackId) = itemId
        return when {
            itemId == rootId -> rootItem
            categoryId == null -> types[typeId]?.item
            trackId == null -> types[typeId]?.categories()?.first()?.find { it.id == itemId }
            else -> types[typeId]?.categoryChildren(categoryId)?.first()?.find { it.id == itemId }
        }
    }

    /**
     * Define a DSL for creating the nodes of a [MediaTree].
     *
     * @constructor Create the media tree builder with the media id of its the topmost element.
     * You should not instantiate this class directly ; use the [MediaTree] function instead.
     *
     * @param rootId The media id of the root element.
     * This should match the rootId of the `BrowserRoot` supplied to clients.
     */
    @MediaTreeDsl
    class Builder(
        private val rootId: MediaId,
        private val rootName: String
    ) {
        private val typeRegistry = mutableMapOf<String, Type>()

        /**
         * Define a _type_ node as a children of the root.
         *
         * @param typeId The unique identifier of the type to be created.
         * @param title The display title for the media item representing this type.
         * @param subtitle The display subtitle for the media item representing this type.
         * This is `null` by default.
         * @param categoryBuilder Block for defining properties of the newly created type.
         */
        fun type(
            typeId: String,
            title: String,
            subtitle: String? = null,
            categoryBuilder: Type.Builder.() -> Unit
        ) {
            check(typeId !in typeRegistry) { "Duplicate type: $typeId" }
            typeRegistry[typeId] = Type.Builder(typeId, title, subtitle)
                .apply(categoryBuilder)
                .build()
        }

        fun type(
            typeId: String,
            title: String,
            subtitle: String? = null,
            provider: ChildrenProvider
        ) {
            val typeMediaId = MediaId(typeId)
            check(typeId !in typeRegistry) { "Duplicate type: $typeMediaId" }
            typeRegistry[typeId] = Type(typeMediaId, title, subtitle, provider)
        }

        /**
         * Instantiate the media tree.
         * This should not be used from the DSL.
         */
        fun build(): MediaTree = MediaTree(rootId, rootName, typeRegistry)
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
     * @constructor Create a new type under the root of the media tree.
     * Do not call this constructor directly ; use [MediaTree.Builder.type] function instead.
     *
     * @param mediaId The name of the type this node represents, which is used as its media id.
     * @param title The display title of the media item associated with this type.
     * @param subtitle The display subtitle of the media item associated with this type.
     * @param childrenProvider Describe how children of this type should be retrieved.
     */
    class Type(
        private val mediaId: MediaId,
        private val title: String,
        private val subtitle: String?,
        private val childrenProvider: ChildrenProvider
    ) {
        /**
         * The media item representing this type in the media tree.
         */
        val item: MediaCategory
            get() = MediaCategory(
                id = mediaId,
                title = title,
                subtitle = subtitle
            )

        /**
         * Load children categories of this type.
         *
         * @return A list of all direct children of this type.
         */
        fun categories(): Flow<List<MediaContent>> = childrenProvider.getChildren(mediaId)

        /**
         * Load children of a category with the specified [categoryId].
         *
         * @param categoryId The identifier of the parent category.
         * @return The children of the specified category as an asynchronous stream.
         */
        fun categoryChildren(
            categoryId: String
        ): Flow<List<MediaContent>> {
            val parentId = MediaId(mediaId.type, categoryId)
            return childrenProvider.getChildren(parentId)
        }

        override fun toString(): String = "[$mediaId] {title=$title, subtitle=$subtitle}"
        override fun equals(other: Any?): Boolean =
            other === this || (other is Type && mediaId == other.mediaId)

        override fun hashCode(): Int = mediaId.hashCode()

        /**
         * Define a DSL for adding _type nodes_ to the media tree.
         *
         * @constructor Create a type builder by specifying its identifier.
         * You should not instantiate this class directly ; use the [MediaTree.Builder.type] function instead.
         *
         * @param typeId The unique identifier of the newly created type. This is used as its media id.
         * @param title The display title for the media item representing this type.
         * @param subtitle The display subtitle for the media item representing this type.
         * This is `null` by default.
         */
        @MediaTreeDsl
        class Builder(
            private val typeId: String,
            private val title: String,
            private val subtitle: String?
        ) {
            private val categoryRegistry = mutableMapOf<String, Category>()

            /**
             * Define a static category, direct child of this type.
             * Unlike dynamic categories, static categories are always part of the tree.
             *
             * @param categoryId Unique identifier of the category.
             * This contributes to the media id of the category.
             * @param title The display title of the media item representing this category.
             * @param subtitle The display subtitle of the media item representing this category.
             * @param iconUri An uri pointing to an icon representing this category.
             * @param playable Whether this category should be both browsable and playable.
             * @param provider Defines how children of this category should be retrieved.
             */
            fun category(
                categoryId: String,
                title: String,
                subtitle: String? = null,
                iconUri: Uri? = null,
                playable: Boolean = false,
                provider: ChildrenProvider
            ) {
                val categoryMediaId = MediaId(typeId, categoryId)
                check(categoryId !in categoryRegistry) { "Duplicate category: $categoryMediaId" }

                categoryRegistry[categoryId] = Category(
                    mediaId = categoryMediaId,
                    title = title,
                    subtitle = subtitle,
                    iconUri = iconUri,
                    playable = playable,
                    provider = provider
                )
            }

            /**
             * Instantiate the type definition.
             * This should not be used from the DSL.
             */
            fun build(): Type = Type(
                MediaId(typeId),
                title,
                subtitle,
                CategoryChildrenProvider(categoryRegistry)
            )
        }
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
     * @param iconUri An uri pointing to an icon representing this category.
     * @param playable Whether this category should be both playable and browsable.
     * @param provider An async function for querying the children of this category.
     */
    class Category(
        val mediaId: MediaId,
        val title: String,
        val subtitle: String?,
        val iconUri: Uri?,
        val playable: Boolean,
        val provider: ChildrenProvider
    ) {
        /**
         * The media item representing this static category.
         */
        val item: MediaCategory
            get() = MediaCategory(
                id = mediaId,
                title = title,
                subtitle = subtitle,
                iconUri = iconUri,
                playable = playable
            )

        fun children(): Flow<List<MediaContent>> = provider.getChildren(mediaId)

        override fun toString(): String = "[$mediaId] {title=$title, subtitle=$subtitle}"
        override fun equals(other: Any?): Boolean =
            this === other || (other is Category && mediaId == other.mediaId)

        override fun hashCode(): Int = mediaId.hashCode()
    }
}
