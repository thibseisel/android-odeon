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

import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ALBUMS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_PLAYLISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.core.media.MediaId.Builder.encode
import fr.nihilus.music.core.test.coroutines.CoroutineTestRule
import fr.nihilus.music.core.test.coroutines.flow.test
import fr.nihilus.music.core.test.coroutines.withinScope
import fr.nihilus.music.core.test.failAssumption
import fr.nihilus.music.service.browser.PaginationOptions
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotlintest.matchers.collections.shouldNotContain
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.matchers.types.shouldNotBeSameInstanceAs
import io.kotlintest.shouldThrow
import kotlinx.coroutines.debug.junit4.CoroutinesTimeout
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.yield
import org.junit.Rule
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class SubscriptionManagerTest {

    @get:Rule
    val test = CoroutineTestRule()

    @get:Rule
    val timeout = CoroutinesTimeout.seconds(5)

    @Test
    fun `When loading children, then subscribe to their parent in the tree`() = test {
        val manager = SubscriptionManagerImpl(this, TestBrowserTree)
        val children = manager.loadChildren(MediaId(TYPE_TRACKS, CATEGORY_ALL), null)

        children.map { it.mediaId }.shouldContainExactly(
            encode(TYPE_TRACKS, CATEGORY_ALL,161),
            encode(TYPE_TRACKS, CATEGORY_ALL,309),
            encode(TYPE_TRACKS, CATEGORY_ALL,481),
            encode(TYPE_TRACKS, CATEGORY_ALL,48),
            encode(TYPE_TRACKS, CATEGORY_ALL,125),
            encode(TYPE_TRACKS, CATEGORY_ALL,294),
            encode(TYPE_TRACKS, CATEGORY_ALL,219),
            encode(TYPE_TRACKS, CATEGORY_ALL,75),
            encode(TYPE_TRACKS, CATEGORY_ALL,464),
            encode(TYPE_TRACKS, CATEGORY_ALL,477)
        )
    }

    @Test
    fun `Given active subscription, when reloading then return cached children`() = test {
        val manager = SubscriptionManagerImpl(this, TestBrowserTree)
        val parentId = MediaId(TYPE_TRACKS, CATEGORY_ALL)

        // Trigger initial subscription
        val children = manager.loadChildren(parentId, null)

        // Reload children, and check that those are the same
        val reloadedChildren = manager.loadChildren(parentId, null)
        reloadedChildren shouldBeSameInstanceAs children
    }

    @Test
    fun `When loading children of invalid parent, then fail with NoSuchElementException`() = test {
        val manager = SubscriptionManagerImpl(this, TestBrowserTree)

        shouldThrow<NoSuchElementException> {
            val invalidMediaId = MediaId(TYPE_PLAYLISTS, "unknown")
            manager.loadChildren(invalidMediaId, null)
        }
    }

    @Test
    fun `Given pages of size N, when loading children then return the N first items`() = test {
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
    fun `Given the page X of size N, when loading children then return N items from position NX`() = test {
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
    fun `Given a page after the last page, when loading children then return no children`() = test {
        val manager = SubscriptionManagerImpl(this, TestBrowserTree)

        val pagePastChildren = manager.loadChildren(
            MediaId(TYPE_TRACKS, CATEGORY_ALL),
            PaginationOptions(2, 5)
        )

        pagePastChildren.shouldBeEmpty()
    }

    @Test
    fun `When observing children and children changed, then notify for its parent`() = test {
        val manager = SubscriptionManagerImpl(this, TestBrowserTree)

        manager.updatedParentIds.test {
            val allTracks = MediaId(TYPE_TRACKS, CATEGORY_ALL)
            manager.loadChildren(allTracks, null)

            expect(1, 1000, TimeUnit.MILLISECONDS)
            expectNone()

            values.shouldContainExactly(allTracks)
        }
    }

    @Test
    fun `When observing multiple children, then notify for each parent`() = test {
        val manager = SubscriptionManagerImpl(this, TestBrowserTree)

        manager.updatedParentIds.test {
            // Subscribe to the first parent.
            val allTracks = MediaId(TYPE_TRACKS, CATEGORY_ALL)
            manager.loadChildren(allTracks, null)

            expect(1, 1000, TimeUnit.MILLISECONDS)
            expectNone()

            // Subscribe to another parent.
            val albumTracks = MediaId(TYPE_ALBUMS, "42")
            manager.loadChildren(albumTracks, null)

            yield()
            expect(2, 1000, TimeUnit.MILLISECONDS)
            values.shouldContainExactlyInAnyOrder(allTracks, allTracks, albumTracks)
        }
    }

    @Test
    fun `Given invalid parent, when observing parent changes then dont throw`() = test {
        val manager = SubscriptionManagerImpl(this, TestBrowserTree)

        manager.updatedParentIds.test {
            // Trigger initial subscription
            shouldThrow<NoSuchElementException> {
                val invalidMediaId = MediaId(TYPE_PLAYLISTS, "unknown")
                manager.loadChildren(invalidMediaId, null)
            }

            // No exceptions should be thrown.
            expectNone()
        }
    }

    @Test
    fun `Given active subscription, when getting a single item then use cached children`() = test {
        val manager = SubscriptionManagerImpl(this, TestBrowserTree)

        // Trigger subscription to all tracks.
        val parentId = MediaId(TYPE_TRACKS, CATEGORY_ALL)
        val children = manager.loadChildren(parentId, null)

        val itemId = parentId.copy(track = 481L)
        val item = manager.getItem(itemId)
        item.shouldNotBeNull()

        val child = children.find { it.mediaId == item.mediaId }
            ?: failAssumption("$itemId should be listed in the children of $parentId, but wasn't")
        item shouldBeSameInstanceAs child
    }

    private fun test(testBody: suspend TestCoroutineScope.() -> Unit) = test.run {
        withinScope(testBody)
    }
}