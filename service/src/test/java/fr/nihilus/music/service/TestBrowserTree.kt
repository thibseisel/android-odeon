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

package fr.nihilus.music.service

import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ALBUMS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.core.test.stub
import fr.nihilus.music.service.browser.BrowserTree
import fr.nihilus.music.service.browser.SearchQuery
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * A fake implementation of [BrowserTree] that provides static collections of children.
 */
internal object TestBrowserTree : BrowserTree {

    override fun getChildren(parentId: MediaId): Flow<List<MediaItem>> {
        val (type, category, track) = parentId

        return when {
            type == TYPE_TRACKS && category == CATEGORY_ALL -> provideAllTracksFlow()
            type == TYPE_ALBUMS -> when {
                category != null && track == null -> getAlbumChildrenFlow(category.toLong())
                else -> noChildrenFlow()
            }

            else -> noChildrenFlow()
        }
    }

    private fun provideAllTracksFlow(): Flow<List<MediaItem>> = periodicFlow(1000).map {
        val builder = MediaDescriptionCompat.Builder()
        longArrayOf(161, 309, 481, 48, 125, 294, 219, 75, 464, 477).map { trackId ->
            val mediaId = MediaId(TYPE_TRACKS, CATEGORY_ALL, trackId)
            val description = builder.setMediaId(mediaId.encoded)
                .setTitle("Track #$trackId")
                .build()

            MediaItem(description, MediaItem.FLAG_BROWSABLE)
        }
    }

    private fun getAlbumChildrenFlow(albumId: Long) = periodicFlow(500).map {
        val albumTrackId = MediaId(TYPE_ALBUMS, albumId.toString(), albumId)
        val description = MediaDescriptionCompat.Builder()
            .setMediaId(albumTrackId.encoded)
            .setTitle("Track #$albumId")
            .setSubtitle("Album #$albumId")
            .build()

        listOf(MediaItem(description, MediaItem.FLAG_PLAYABLE))
    }

    private fun periodicFlow(period: Long) = flow {
        while (true) {
            emit(Unit)
            delay(period)
        }
    }

    private fun noChildrenFlow() = flow<Nothing> {
        throw NoSuchElementException()
    }

    override suspend fun getItem(itemId: MediaId): MediaItem? = stub()

    override suspend fun search(query: SearchQuery): List<MediaItem> = stub()
}