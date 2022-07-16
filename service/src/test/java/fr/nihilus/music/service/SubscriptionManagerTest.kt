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
import app.cash.turbine.test
import fr.nihilus.music.core.context.AppDispatchers
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ALBUMS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_PLAYLISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.core.test.coroutines.CoroutineTestRule
import fr.nihilus.music.core.test.failAssumption
import fr.nihilus.music.media.MediaContent
import io.kotest.assertions.extracting
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.*
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class SubscriptionManagerTest {

    @get:Rule
    val test = CoroutineTestRule()

    private lateinit var dispatchers: AppDispatchers

    @BeforeTest
    fun initDispatchers() {
        dispatchers = AppDispatchers(test.dispatcher)
    }

    @Test
    fun `When loading children, then subscribe to their parent in the tree`() =
        test.runWithin { scope ->
            val manager = CachingSubscriptionManager(scope, TestBrowserTree, dispatchers)
            val children = manager.loadChildren(MediaId(TYPE_TRACKS, CATEGORY_ALL), null)

            extracting(children, MediaContent::id).shouldContainExactly(
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 161),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 309),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 481),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 48),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 125),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 294),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 219),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 75),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 464),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 477)
            )
        }

    @Test
    fun `Given active subscription, when reloading then return cached children`() =
        test.runWithin { scope ->
            val manager = CachingSubscriptionManager(scope, TestBrowserTree, dispatchers)
            val parentId = MediaId(TYPE_TRACKS, CATEGORY_ALL)

            // Trigger initial subscription
            val children = manager.loadChildren(parentId, null)

            // Reload children, and check that those are the same
            val reloadedChildren = manager.loadChildren(parentId, null)
            reloadedChildren shouldBeSameInstanceAs children
        }

    @Test
    fun `When loading children of invalid parent, then fail with NoSuchElementException`() =
        test.runWithin { scope ->
            val manager = CachingSubscriptionManager(scope, TestBrowserTree, dispatchers)

            shouldThrow<NoSuchElementException> {
                val invalidMediaId = MediaId(TYPE_PLAYLISTS, "unknown")
                manager.loadChildren(invalidMediaId, null)
            }
        }

    @Test
    fun `Given max subscriptions, when loading children then dispose oldest subscriptions`() =
        test.runWithin { scope ->
            val manager = CachingSubscriptionManager(scope, TestBrowserTree, dispatchers)

            // Trigger subscription of albums 0 to MAX included.
            val childrenPerAlbumId = (0..MAX_ACTIVE_SUBSCRIPTIONS).map { albumId ->
                val parentId = MediaId(TYPE_ALBUMS, albumId.toString())
                manager.loadChildren(parentId, null)
            }

            // Subscription to album 0 should have been disposed when subscribing to album MAX.
            // Its children should not be loaded from cache.
            val albumZeroChildren = manager.loadChildren(MediaId(TYPE_ALBUMS, "0"), null)
            albumZeroChildren shouldNotBeSameInstanceAs childrenPerAlbumId[0]

            // Previous re-subscription to album 0 should clear subscription to album 1,
            // and therefore it should not load its children from cache.
            val albumOneChildren = manager.loadChildren(MediaId(TYPE_ALBUMS, "1"), null)
            albumOneChildren shouldNotBeSameInstanceAs childrenPerAlbumId[1]

            // Subscription to album MAX should still be active,
            // hence children are loaded from cache.
            val lastAlbumId = MediaId(TYPE_ALBUMS, MAX_ACTIVE_SUBSCRIPTIONS.toString())
            val albumMaxChildren = manager.loadChildren(lastAlbumId, null)
            albumMaxChildren shouldBeSameInstanceAs childrenPerAlbumId[MAX_ACTIVE_SUBSCRIPTIONS]
        }

    @Test
    fun `Given max subscriptions, when reloading children then keep its subscription longer`() =
        test.runWithin { scope ->
            val manager = CachingSubscriptionManager(scope, TestBrowserTree, dispatchers)

            // Trigger subscriptions to reach the max allowed count.
            val childrenPerAlbumId = (0 until MAX_ACTIVE_SUBSCRIPTIONS).map { albumId ->
                val parentId = MediaId(TYPE_ALBUMS, albumId.toString())
                manager.loadChildren(parentId, null)
            }

            // Reload children of album 0, then create a new subscription.
            manager.loadChildren(MediaId(TYPE_ALBUMS, "0"), null)
            manager.loadChildren(MediaId(TYPE_ALBUMS, MAX_ACTIVE_SUBSCRIPTIONS.toString()), null)

            // If album 0 had not been reloaded, its subscription should have been disposed.
            // The oldest subscription now being that of album 1, it has been disposed instead.
            val albumZeroChildren = manager.loadChildren(MediaId(TYPE_ALBUMS, "0"), null)
            val albumOneChildren = manager.loadChildren(MediaId(TYPE_ALBUMS, "1"), null)

            albumZeroChildren shouldBeSameInstanceAs childrenPerAlbumId[0]
            albumOneChildren shouldNotBeSameInstanceAs childrenPerAlbumId[1]
        }

    @Test
    fun `Given pages of size N, when loading children then return the N first items`() =
        test.runWithin { scope ->
            val manager = CachingSubscriptionManager(scope, TestBrowserTree, dispatchers)

            val paginatedChildren = manager.loadChildren(
                MediaId(TYPE_TRACKS, CATEGORY_ALL),
                PaginationOptions(0, 3)
            )

            extracting(paginatedChildren, MediaContent::id).shouldContainExactly(
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 161),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 309),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 481)
            )
        }

    @Test
    fun `Given the page X of size N, when loading children then return N items from position NX`() =
        test.runWithin { scope ->
            val manager = CachingSubscriptionManager(scope, TestBrowserTree, dispatchers)

            val paginatedChildren = manager.loadChildren(
                MediaId(TYPE_TRACKS, CATEGORY_ALL),
                PaginationOptions(3, 2)
            )

            extracting(paginatedChildren, MediaContent::id).shouldContainExactly(
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 219),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 75)
            )
        }

    @Test
    fun `Given a page after the last page, when loading children then return no children`() =
        test.runWithin { scope ->
            val manager = CachingSubscriptionManager(scope, TestBrowserTree, dispatchers)

            val pagePastChildren = manager.loadChildren(
                MediaId(TYPE_TRACKS, CATEGORY_ALL),
                PaginationOptions(2, 5)
            )

            pagePastChildren.shouldBeEmpty()
        }

    @Test
    fun `When observing children and children changed, then notify for its parent`() =
        test.runWithin { scope ->
            val manager = CachingSubscriptionManager(scope, TestBrowserTree, dispatchers)

            manager.updatedParentIds.test {
                val allTracks = MediaId(TYPE_TRACKS, CATEGORY_ALL)
                manager.loadChildren(allTracks, null)

                awaitItem() shouldBe allTracks
                expectNoEvents()
            }
        }

    @Test
    fun `When observing multiple children, then notify for each parent`() =
        test.runWithin { scope ->
            val manager = CachingSubscriptionManager(scope, TestBrowserTree, dispatchers)

            manager.updatedParentIds.test {
                // Subscribe to the first parent.
                val allTracks = MediaId(TYPE_TRACKS, CATEGORY_ALL)
                manager.loadChildren(allTracks, null)

                awaitItem() shouldBe allTracks
                expectNoEvents()

                // Subscribe to another parent.
                val albumTracks = MediaId(TYPE_ALBUMS, "42")
                manager.loadChildren(albumTracks, null)

                val values = List(2) { awaitItem() }
                values.shouldContainExactlyInAnyOrder(allTracks, albumTracks)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `When observing parent changes, then all subscribers should be notified`() =
        test.runWithin { scope ->
            val manager = CachingSubscriptionManager(scope, TestBrowserTree, dispatchers)
            val albumId = MediaId(TYPE_ALBUMS, "42")

            manager.updatedParentIds.test {
                manager.updatedParentIds.test {
                    // trigger initial subscription
                    manager.loadChildren(albumId, null)
                    awaitItem() shouldBe albumId
                }

                awaitItem() shouldBe albumId
            }
        }

    @Test
    fun `Given invalid parent, when observing parent changes then dont throw`() =
        test.runWithin { scope ->
            val manager = CachingSubscriptionManager(scope, TestBrowserTree, dispatchers)

            manager.updatedParentIds.test {
                // Trigger initial subscription
                shouldThrow<NoSuchElementException> {
                    val invalidMediaId = MediaId(TYPE_PLAYLISTS, "unknown")
                    manager.loadChildren(invalidMediaId, null)
                }

                // No exceptions should be thrown.
                expectNoEvents()
            }
        }

    @Test
    fun `Given max subscriptions, when observing parent changes then dont notify for disposed subscriptions`() =
        test.runWithin { scope ->
            val manager = CachingSubscriptionManager(scope, TestBrowserTree, dispatchers)

            manager.updatedParentIds.test {
                (0..MAX_ACTIVE_SUBSCRIPTIONS).forEach { albumId ->
                    val albumMediaId = MediaId(TYPE_ALBUMS, albumId.toString())
                    manager.loadChildren(albumMediaId, null)
                }

                delay(1000)
                val values = List(5) { awaitItem() }
                val albumZeroId = MediaId(TYPE_ALBUMS, "0")
                values shouldNotContain albumZeroId
            }
        }

    @Test
    fun `Given max subscriptions, when reloading parent then observe its changes longer`() =
        test.runWithin { scope ->
            val manager = CachingSubscriptionManager(scope, TestBrowserTree, dispatchers)

            manager.updatedParentIds.test {

                // Trigger subscriptions to reach the max allowed count.
                repeat(MAX_ACTIVE_SUBSCRIPTIONS) { albumId ->
                    val parentId = MediaId(TYPE_ALBUMS, albumId.toString())
                    manager.loadChildren(parentId, null)
                }

                // Reload children of album 0, then create a new subscription.
                manager.loadChildren(MediaId(TYPE_ALBUMS, "0"), null)
                manager.loadChildren(
                    MediaId(TYPE_ALBUMS, MAX_ACTIVE_SUBSCRIPTIONS.toString()),
                    null
                )

                // If album 0 had not been reloaded, its subscription should have been disposed.
                // The oldest subscription now being that of album 1, it has been disposed instead,
                // therefore we should no longer receive updates for it.
                delay(1000)
                val values = List(5) { awaitItem() }
                values shouldContain MediaId(TYPE_ALBUMS, "0")
                values shouldNotContain MediaId(TYPE_ALBUMS, "1")
            }
        }

    @Test
    fun `Given active subscription, when getting a playable item then use cached children`() =
        test.runWithin { scope ->
            scope.assertGetItemFromCache(
                parentId = MediaId(TYPE_TRACKS, CATEGORY_ALL),
                itemId = MediaId(TYPE_TRACKS, CATEGORY_ALL, 481L)
            )
        }

    @Test
    fun `Given active subscription, when getting a browsable item then use cached children`() =
        test.runWithin { scope ->
            scope.assertGetItemFromCache(
                parentId = MediaId(TYPE_ALBUMS),
                itemId = MediaId(TYPE_ALBUMS, "42")
            )
        }

    private suspend fun CoroutineScope.assertGetItemFromCache(parentId: MediaId, itemId: MediaId) {
        // Trigger subscription to fill cache
        val manager = CachingSubscriptionManager(this, TestBrowserTree, dispatchers)
        val children = manager.loadChildren(parentId, null)

        val child = children.find { it.id == itemId }
            ?: failAssumption("$itemId should be listed in the children of $parentId, but wasn't")

        val item = manager.getItem(itemId)
        item.shouldNotBeNull()
        item shouldBeSameInstanceAs child
    }
}
