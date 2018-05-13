/*
 * Copyright 2018 Thibault Seisel
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

/**
 * Defines conditions under which two media item instances are considered the same.
 */
object MediaItemDiffer : DiffUtil.ItemCallback<MediaItem>() {

    override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem) =
        oldItem.mediaId == newItem.mediaId

    override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
        val oldDesc = oldItem.description
        val newDesc = newItem.description

        return TextUtils.equals(oldDesc.title, newDesc.title)
                && TextUtils.equals(oldDesc.subtitle, newDesc.subtitle)
                && oldDesc.iconUri == newDesc.iconUri
                && oldDesc.mediaUri == newDesc.mediaUri
                && TextUtils.equals(oldDesc.description, newDesc.description)
    }

}
