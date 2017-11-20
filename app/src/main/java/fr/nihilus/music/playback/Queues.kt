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

package fr.nihilus.music.playback

import android.support.v4.media.session.MediaSessionCompat

/**
 * Indicates if a given [index] in a playing [queue]
 * correspond to a playable [MediaSessionCompat.QueueItem].
 * @param index of the item tu search in queue
 * @param queue in which the item might be
 * @return whether index points to a playable item in queue
 */
internal fun isIndexPlayable(index: Int, queue: List<MediaSessionCompat.QueueItem>) =
        index in 0 .. (queue.size - 1)

/**
 * Find the index of a specific [mediaId] in a playing [queue].
 * @param queue in which search for the mediaId
 * @param mediaId of a item that might be in the queue
 * @return position of an item with the specified mediaId, or -1 if not found
 */
internal fun musicIndexOnQueue(queue: List<MediaSessionCompat.QueueItem>, mediaId: String) =
        queue.indexOfFirst { it.description.mediaId == mediaId }

/**
 * Find the index of a specific [queueId] in the playing [queue].
 * @param queue in which search for the queueId
 * @param queueId of a item that might be in the queue
 * @return position of an item with the specified queueId, or -1 if not found
 */
internal fun musicIndexOnQueue(queue: List<MediaSessionCompat.QueueItem>, queueId: Long) =
        queue.indexOfFirst { it.queueId == queueId }