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
import android.util.SparseIntArray
import androidx.core.util.set
import androidx.recyclerview.widget.GridLayoutManager
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.toMediaId
import timber.log.Timber

internal class SearchSpanLookup(
    val spanCount: Int
) : GridLayoutManager.SpanSizeLookup() {
    private val spanSizeForPosition = SparseIntArray()
    private val spanIndexForPosition = SparseIntArray()
    private val spanGroupForPosition = SparseIntArray()

    init {
        require(spanCount > 0) { "rowSpanCount should be positive" }
    }

    override fun getSpanSize(position: Int): Int {
        val spanSize = spanSizeForPosition[position]
        Timber.d("getSpanSize(%d) = %d", position, spanSize)
        return spanSize
    }

    override fun getSpanIndex(position: Int, spanCount: Int): Int {
        val spanIndex = spanIndexForPosition[position]
        Timber.d("getSpanIndex(%d, %d) = %d", position, spanCount, spanIndex)
        return spanIndex
    }

    override fun getSpanGroupIndex(adapterPosition: Int, spanCount: Int): Int {
        val spanGroupIndex = spanGroupForPosition[adapterPosition]
        Timber.d("getSpanGroupIndex(%d, %d) = %d", adapterPosition, spanCount, spanGroupIndex)
        return spanGroupIndex
    }

    fun update(results: List<MediaBrowserCompat.MediaItem>) {
        val indicesForEachType = results.withIndex()
            .groupingBy { it.value.mediaType() }
            .fold({ _, (index) -> index..index }) { _, acc, (index) ->
                minOf(acc.first, index)..maxOf(acc.last, index)
            }

        var spanIndex = 0
        var spanGroup = 0
        for ((mediaType, positions) in indicesForEachType) {
            val spanSize = spanSizeOf(mediaType)
            for (position in positions) {
                spanSizeForPosition[position] = spanSize
                spanIndexForPosition[position] = spanIndex
                spanGroupForPosition[position] = spanGroup

                spanGroup += (spanIndex + spanSize) / spanCount
                spanIndex = (spanIndex + spanSize) % spanCount
            }

            // Spawn a new line after changing type
            if (spanIndex != 0) {
                spanIndex = 0
                spanGroup++
            }
        }
    }

    private fun MediaBrowserCompat.MediaItem.mediaType() = mediaId.toMediaId().type

    private fun spanSizeOf(mediaType: String) = when (mediaType) {
        MediaId.TYPE_ALBUMS, MediaId.TYPE_ARTISTS -> 1
        else -> spanCount
    }
}