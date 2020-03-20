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

import android.net.Uri
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

    override fun getChildren(parentId: MediaId): Flow<List<MediaContent>> {
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

    private fun provideAllTracksFlow(): Flow<List<MediaContent>> = periodicFlow(1000).map {
        longArrayOf(161, 309, 481, 48, 125, 294, 219, 75, 464, 477).map { trackId ->
            AudioTrack(
                id = MediaId(TYPE_TRACKS, CATEGORY_ALL, trackId),
                title = "Track #$trackId",
                subtitle = null,
                album = "Some album",
                artist = "Some artist",
                discNumber = 1,
                trackNumber = 1,
                duration = 0L,
                mediaUri = Uri.EMPTY,
                iconUri = null
            )
        }
    }

    private fun getAlbumChildrenFlow(albumId: Long) = periodicFlow(500).map {
        listOf(
            AudioTrack(
                id = MediaId(TYPE_ALBUMS, albumId.toString(), albumId),
                title = "Track #$albumId",
                subtitle = "Album #$albumId",
                album = "Some album",
                artist = "Some artist",
                discNumber = 1,
                trackNumber = 1,
                duration = 0L,
                mediaUri = Uri.EMPTY,
                iconUri = null
            )
        )
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

    override suspend fun getItem(itemId: MediaId): MediaContent? = stub()

    override suspend fun search(query: SearchQuery): List<MediaContent> = stub()
}