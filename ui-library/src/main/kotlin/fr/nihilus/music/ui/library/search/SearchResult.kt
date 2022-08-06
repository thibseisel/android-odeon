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

package fr.nihilus.music.ui.library.search

import android.net.Uri
import androidx.annotation.StringRes
import fr.nihilus.music.core.media.MediaId

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
     * Playable or browsable media that matched the search query.
     */
    data class Track(
        val id: MediaId,
        val title: String,
        val iconUri: Uri?,
    ) : SearchResult() {

        override fun hasSameId(other: SearchResult): Boolean =
            other is Track && other.id == id

        override fun hasSameContent(other: SearchResult): Boolean = other == this
    }

    data class Browsable(
        val id: MediaId,
        val title: String,
        val subtitle : String?,
        val tracksCount: Int,
        val iconUri: Uri?,
    ) : SearchResult() {

        override fun hasSameId(other: SearchResult): Boolean =
            other is Browsable && other.id == id

        override fun hasSameContent(other: SearchResult): Boolean = other == this
    }
}
