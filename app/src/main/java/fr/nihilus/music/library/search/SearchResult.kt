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

package fr.nihilus.music.library.search

import android.support.v4.media.MediaBrowserCompat
import androidx.annotation.StringRes

/**
 * An UI element in the list of search results.
 */
internal sealed class SearchResult {
    abstract fun hasSameId(other: SearchResult): Boolean
    abstract fun hasSameContent(other: SearchResult): Boolean

    /**
     * Title section that groups media of the same type.
     */
    data class SectionHeader(
        @StringRes val titleResId: Int
    ) : SearchResult() {

        override fun hasSameId(other: SearchResult): Boolean =
            other is SectionHeader && other.titleResId == titleResId

        override fun hasSameContent(other: SearchResult): Boolean = hasSameId(other)
    }

    /**
     * A reference to a media that matched the search query.
     */
    data class Media(
        val item: MediaBrowserCompat.MediaItem
    ) : SearchResult() {

        override fun hasSameId(other: SearchResult): Boolean =
            other is Media && other.item.mediaId == item.mediaId

        override fun hasSameContent(other: SearchResult): Boolean {
            val otherDescription = (other as? Media)?.item?.description ?: return false
            val thisDescription = item.description
            return otherDescription.title == thisDescription.title
                    && otherDescription.subtitle == thisDescription.subtitle
                    && otherDescription.iconUri == thisDescription.iconUri
        }
    }
}