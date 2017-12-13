/*
 * Copyright 2017 Thibault Seisel
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

package fr.nihilus.music.utils

import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v7.util.DiffUtil
import android.text.TextUtils

class MediaItemDiffCallback(
        private val oldItems: List<MediaItem>,
        private val newItems: List<MediaItem>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldItems.size

    override fun getNewListSize() = newItems.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldId = oldItems[oldItemPosition].mediaId
        val newId = newItems[newItemPosition].mediaId
        return oldId == newId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldDesc = oldItems[oldItemPosition].description
        val newDesc = newItems[newItemPosition].description
        val sameTitle = TextUtils.equals(oldDesc.title, newDesc.title)
        val sameSubtitle = TextUtils.equals(oldDesc.subtitle, newDesc.subtitle)
        return sameTitle && sameSubtitle
    }
}
