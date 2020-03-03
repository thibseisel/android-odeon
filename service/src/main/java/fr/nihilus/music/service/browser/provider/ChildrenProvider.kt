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

import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaItems

/**
 * Provides children of media categories.
 */
internal abstract class ChildrenProvider {

    /**
     * Returns children of a given media.
     *
     * @param parentId The media id of the parent in the media tree.
     * This parent should be browsable.
     * @param fromIndex The index of the first item of the returned page.
     * Should be positive or zero.
     * @param count The number of items in the returned page.
     * Should be strictly positive. A count of [Int.MAX_VALUE] returns all items.
     *
     * @return The up-to-date list of children of the given media,
     * or `null` if that media is not browsable.
     */
    suspend fun getChildren(
        parentId: MediaId,
        fromIndex: Int = 0,
        count: Int = Int.MAX_VALUE
    ): List<MediaItem>? {
        return when {
            parentId.track != null -> null
            else -> findChildren(parentId, fromIndex, count)
        }
    }

    /**
     * Override this function to provide children of a given browsable media.
     * Each emitted list of children should be restricted by the provided pagination parameters.
     *
     * @param parentId The media id of the browsable parent in the media tree.
     * @param fromIndex The index of the first item in the returned page.
     * @param count The number of items in the returned page.
     * Passing [Int.MAX_VALUE] should return all items starting at [fromIndex].
     *
     * @return up-to-date list of children of the given media restricted by the provided
     * pagination parameters, or `null` if that media does not exist.
     */
    protected abstract suspend fun findChildren(
        parentId: MediaId,
        fromIndex: Int,
        count: Int
    ): List<MediaItem>?

    /**
     * Helper function to create a browsable [MediaItem].
     */
    protected fun browsable(
        builder: MediaDescriptionCompat.Builder,
        id: String,
        title: String,
        subtitle: String? = null,
        trackCount: Int = 0,
        iconUri: Uri? = null
    ): MediaItem {
        val extras = if (trackCount <= 0) null else Bundle(1).apply {
            putInt(MediaItems.EXTRA_NUMBER_OF_TRACKS, trackCount)
        }

        val description = builder.setMediaId(id)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setIconUri(iconUri)
            .setExtras(extras)
            .build()
        return MediaItem(description, MediaItem.FLAG_BROWSABLE)
    }

    /**
     * Helper function to create a playable [MediaItem].
     */
    protected fun playable(
        builder: MediaDescriptionCompat.Builder,
        id: String,
        title: String,
        subtitle: String,
        mediaUri: Uri,
        iconUri: Uri?,
        duration: Long,
        disc: Int,
        number: Int
    ) : MediaItem {
        val description = builder.setMediaId(id)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setMediaUri(mediaUri)
            .setIconUri(iconUri)
            .setExtras(Bundle().apply {
                putLong(MediaItems.EXTRA_DURATION, duration)
                putInt(MediaItems.EXTRA_DISC_NUMBER, disc)
                putInt(MediaItems.EXTRA_TRACK_NUMBER, number)
            }).build()
        return MediaItem(description, MediaItem.FLAG_PLAYABLE)
    }
}