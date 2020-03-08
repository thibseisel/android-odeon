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
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.core.media.MediaId.Builder.encode
import fr.nihilus.music.core.test.coroutines.CoroutineTestRule
import fr.nihilus.music.core.test.stub
import fr.nihilus.music.service.browser.BrowserTree
import fr.nihilus.music.service.browser.PaginationOptions
import fr.nihilus.music.service.browser.SearchQuery
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Rule
import org.junit.runner.RunWith
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class SubscriptionManagerTest {

    @get:Rule
    val test = CoroutineTestRule()

    @Test
    fun `Given pages of size N, when loading children then return the N first items`() = test.run {
        val manager = SubscriptionManagerImpl(this, TestBrowserTree)

        val paginatedChildren = manager.loadChildren(
            MediaId(TYPE_TRACKS, CATEGORY_ALL),
            PaginationOptions(0, 3)
        )

        paginatedChildren.map { it.mediaId }.shouldContainExactly(
            encode(TYPE_TRACKS, CATEGORY_ALL, 161),
            encode(TYPE_TRACKS, CATEGORY_ALL, 309),
            encode(TYPE_TRACKS, CATEGORY_ALL, 481)
        )
    }

    @Test
    fun `Given the page X of size N, when loading children then return N items from position NX`() = test.run {
        val manager = SubscriptionManagerImpl(this, TestBrowserTree)

        val paginatedChildren = manager.loadChildren(
            MediaId(TYPE_TRACKS, CATEGORY_ALL),
            PaginationOptions(3, 2)
        )

        paginatedChildren.map { it.mediaId }.shouldContainExactly(
            encode(TYPE_TRACKS, CATEGORY_ALL, 219),
            encode(TYPE_TRACKS, CATEGORY_ALL, 75)
        )
    }

    @Test
    fun `Given a page after the last page, when loading children then return no children`() = test.run {
        val manager = SubscriptionManagerImpl(this, TestBrowserTree)

        val pagePastChildren = manager.loadChildren(
            MediaId(TYPE_TRACKS, CATEGORY_ALL),
            PaginationOptions(2, 5)
        )

        pagePastChildren.shouldBeEmpty()
    }

    private object TestBrowserTree : BrowserTree {

        override fun getChildren(parentId: MediaId): Flow<List<MediaItem>> {
            val (typeId, categoryId, trackId) = parentId
            check(typeId == TYPE_TRACKS)
            check(categoryId == CATEGORY_ALL)
            check(trackId == null)

            val builder = MediaDescriptionCompat.Builder()
            val trackItems = longArrayOf(161, 309, 481, 48, 125, 294, 219, 75, 464, 477).map {
                dummyItem(builder, it)
            }

            return flow {
                emit(trackItems)
                suspendCancellableCoroutine<Nothing> {}
            }
        }

        override suspend fun getItem(itemId: MediaId): MediaItem? = stub()

        override suspend fun search(query: SearchQuery): List<MediaItem> = stub()

        private fun dummyItem(
            builder: MediaDescriptionCompat.Builder,
            trackId: Long
        ): MediaItem {
            val mediaId = encode(TYPE_TRACKS, CATEGORY_ALL, trackId)
            val description = builder.setMediaId(mediaId).build()
            return MediaItem(description, MediaItem.FLAG_PLAYABLE)
        }
    }

}