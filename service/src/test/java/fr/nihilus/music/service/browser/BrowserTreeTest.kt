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

package fr.nihilus.music.service.browser

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_MOST_RATED
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_POPULAR
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_RECENTLY_ADDED
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ALBUMS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ARTISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_PLAYLISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ROOT
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.core.test.fail
import fr.nihilus.music.core.test.failAssumption
import fr.nihilus.music.service.AudioTrack
import fr.nihilus.music.service.MediaCategory
import fr.nihilus.music.service.MediaContent
import io.kotest.assertions.extracting
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.BeforeTest

/**
 * Validate the structure of the [BrowserTree]:
 * - tree can be browsed from the root to the topmost leaves,
 * - children of those nodes are correctly fetched and mapped to [MediaContent]s.
 */
@RunWith(AndroidJUnit4::class)
internal class BrowserTreeTest {
    private lateinit var browserTree: BrowserTree

    @BeforeTest
    fun setup() {
        browserTree = BrowserTreeImpl(
            context = ApplicationProvider.getApplicationContext(),
            tracks = FakeChildrenProvider(
                MediaId(TYPE_TRACKS, CATEGORY_ALL) to listOf(
                    AudioTracks.IsolatedSystem,
                    AudioTracks.Algorithm,
                    AudioTracks.DirtyWater,
                ),
                MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED) to listOf(
                    AudioTracks.Algorithm.withId(category = CATEGORY_MOST_RATED),
                    AudioTracks.DirtyWater.withId(category = CATEGORY_RECENTLY_ADDED),
                    AudioTracks.IsolatedSystem.withId(category = CATEGORY_MOST_RATED),
                ),
                MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED) to listOf(
                    AudioTracks.Algorithm.withId(category = CATEGORY_RECENTLY_ADDED),
                    AudioTracks.DirtyWater.withId(category = CATEGORY_RECENTLY_ADDED),
                    AudioTracks.IsolatedSystem.withId(category = CATEGORY_RECENTLY_ADDED),
                ),
                MediaId(TYPE_TRACKS, CATEGORY_POPULAR) to emptyList()
            ),
            albums = FakeChildrenProvider(
                MediaId(TYPE_ALBUMS) to listOf(
                    MediaCategories.The2ndLaw,
                    MediaCategories.ConcreteAndGold,
                    MediaCategories.SimulationTheory,
                ),
                MediaId(TYPE_ALBUMS, "40") to listOf(
                    AudioTracks.IsolatedSystem.withId(
                        TYPE_ALBUMS,
                        "40"
                    )
                ),
                MediaId(TYPE_ALBUMS, "102") to listOf(
                    AudioTracks.DirtyWater.withId(
                        TYPE_ALBUMS,
                        "102"
                    )
                ),
                MediaId(TYPE_ALBUMS, "98") to listOf(
                    AudioTracks.Algorithm.withId(
                        TYPE_ALBUMS,
                        "98"
                    )
                ),
            ),
            artists = FakeChildrenProvider(
                MediaId(TYPE_ARTISTS) to listOf(
                    MediaCategories.FooFighters,
                    MediaCategories.Muse,
                ),
                MediaId(TYPE_ARTISTS, "13") to listOf(
                    MediaCategories.ConcreteAndGold,
                    AudioTracks.DirtyWater.withId(TYPE_ARTISTS, "13")
                ),
                MediaId(TYPE_ARTISTS, "26") to listOf(
                    MediaCategories.SimulationTheory,
                    MediaCategories.The2ndLaw,
                    AudioTracks.IsolatedSystem.withId(TYPE_ARTISTS, "26"),
                    AudioTracks.Algorithm.withId(TYPE_ARTISTS, "26")
                )
            ),
            playlists = FakeChildrenProvider(
                MediaId(TYPE_PLAYLISTS) to listOf(MediaCategories.MyFavorites),
                MediaId(TYPE_PLAYLISTS, "1") to listOf(
                    AudioTracks.Algorithm.withId(TYPE_PLAYLISTS, "1"),
                    AudioTracks.DirtyWater.withId(TYPE_PLAYLISTS, "1"),
                )
            ),
        )
    }

    @Test
    fun `When loading children of Root, then return all available types`() = runTest {
        val rootChildren = browserTree.getChildren(MediaId(TYPE_ROOT)).first()

        extracting(rootChildren) { id }.shouldContainExactlyInAnyOrder(
            MediaId(TYPE_TRACKS),
            MediaId(TYPE_ARTISTS),
            MediaId(TYPE_ALBUMS),
            MediaId(TYPE_PLAYLISTS),
        )

        rootChildren.shouldAllBeBrowsable()
        rootChildren.noneShouldBePlayable()
    }

    @Test
    fun `When loading children of Track type, then return track categories`() = runTest {
        val trackTypeChildren = browserTree.getChildren(MediaId(TYPE_TRACKS)).first()

        extracting(trackTypeChildren) { id }.shouldContainExactlyInAnyOrder(
            MediaId(TYPE_TRACKS, CATEGORY_ALL),
            MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED),
            MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED),
            MediaId(TYPE_TRACKS, CATEGORY_POPULAR)
        )

        trackTypeChildren.shouldAllBeBrowsable()
        trackTypeChildren.shouldAllBePlayable()
    }

    @Test
    fun `When loading children of Album type, then return all albums`() = runTest {
        val albums = browserTree.getChildren(MediaId(TYPE_ALBUMS)).first()

        albums.shouldContainExactly(
            MediaCategories.The2ndLaw,
            MediaCategories.ConcreteAndGold,
            MediaCategories.SimulationTheory,
        )
    }

    @Test
    fun `When loading children of Artist type, then return all artists`() = runTest {
        val artists = browserTree.getChildren(MediaId(TYPE_ARTISTS)).first()

        artists.shouldContainExactly(
            MediaCategories.FooFighters,
            MediaCategories.Muse,
        )
    }

    @Test
    fun `When loading children of Playlist type, then return all playlists`() = runTest {
        val playlists = browserTree.getChildren(MediaId(TYPE_PLAYLISTS)).first()

        playlists.shouldContainExactly(MediaCategories.MyFavorites)
    }

    @Test
    fun `Given any browsable parent, when loading its children then never throw`() = runTest {
        browserTree.walk(MediaId(TYPE_ROOT)) { child, _ ->
            if (child is MediaCategory) {
                shouldNotThrowAny {
                    browserTree.getChildren(child.id).first()
                }
            }
        }
    }

    @Test
    fun `Given any non browsable item, when loading its children then throw NoSuchElementException`() =
        runTest {
            browserTree.walk(MediaId(TYPE_ROOT)) { child, _ ->
                if (child !is MediaCategory) {
                    shouldThrow<NoSuchElementException> {
                        browserTree.getChildren(child.id).first()
                    }
                }
            }
        }

    @Test
    fun `When loading children of All Tracks, then return all tracks`() = runTest {
        val tracks = browserTree.getChildren(MediaId(TYPE_TRACKS, CATEGORY_ALL)).first()

        tracks.shouldContainExactly(
            AudioTracks.IsolatedSystem,
            AudioTracks.Algorithm,
            AudioTracks.DirtyWater,
        )
    }

    @Test
    fun `When loading children of Most Rated, then return most rated tracks`() = runTest {
        val mostRatedTracks =
            browserTree.getChildren(MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED)).first()

        mostRatedTracks.shouldContainExactly(
            AudioTracks.Algorithm.withId(category = CATEGORY_MOST_RATED),
            AudioTracks.DirtyWater.withId(category = CATEGORY_RECENTLY_ADDED),
            AudioTracks.IsolatedSystem.withId(category = CATEGORY_MOST_RATED),
        )
    }

    @Test
    fun `When loading children of Recently Added, then return recently added tracks`() = runTest {
        val recentlyAdded =
            browserTree.getChildren(MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED)).first()

        recentlyAdded.shouldContainExactly(
            AudioTracks.Algorithm.withId(category = CATEGORY_RECENTLY_ADDED),
            AudioTracks.DirtyWater.withId(category = CATEGORY_RECENTLY_ADDED),
            AudioTracks.IsolatedSystem.withId(category = CATEGORY_RECENTLY_ADDED),
        )
    }

    @Test
    fun `When loading children of an album, then return tracks from that album`() = runTest {
        val albumTracks = browserTree.getChildren(MediaId(TYPE_ALBUMS, "98")).first()

        albumTracks.shouldContainExactly(
            AudioTracks.Algorithm.withId(type = TYPE_ALBUMS, category = "98")
        )
    }

    @Test
    fun `When loading children of an artist, then return its children`() = runTest {
        val artistChildren = browserTree.getChildren(MediaId(TYPE_ARTISTS, "13")).first()

        artistChildren.shouldContainExactly(
            MediaCategories.ConcreteAndGold,
            AudioTracks.DirtyWater.withId(type = TYPE_ARTISTS, "13"),
        )
    }

    @Test
    fun `When loading children of a playlist, then return tracks from that playlist`() = runTest {
        val playlistChildren = browserTree.getChildren(MediaId(TYPE_PLAYLISTS, "1")).first()

        playlistChildren.shouldContainExactly(
            AudioTracks.Algorithm.withId(type = TYPE_PLAYLISTS, category = "1"),
            AudioTracks.DirtyWater.withId(type = TYPE_PLAYLISTS, category = "1"),
        )
    }

    @Test
    fun `Given an unknown category, when loading its children then return null`() = runTest {
        assertHasNoChildren(MediaId("unknown"))
        assertHasNoChildren(MediaId(TYPE_TRACKS, "undefined"))
        assertHasNoChildren(MediaId(TYPE_ALBUMS, "1234"))
        assertHasNoChildren(MediaId(TYPE_ARTISTS, "1234"))
        assertHasNoChildren(MediaId(TYPE_PLAYLISTS, "1234"))
    }

    @Test
    fun `When requesting any item, then return an item with the same id as requested`() = runTest {
        MediaId(TYPE_ROOT).shouldGetItemWithSameId()
        MediaId(TYPE_TRACKS, CATEGORY_ALL).shouldGetItemWithSameId()
        MediaId(TYPE_TRACKS, CATEGORY_ALL, 65).shouldGetItemWithSameId()
        MediaId(TYPE_ALBUMS, "40").shouldGetItemWithSameId()
        MediaId(TYPE_TRACKS).shouldGetItemWithSameId()
        MediaId(TYPE_ALBUMS, "40", 65).shouldGetItemWithSameId()
        MediaId(TYPE_ARTISTS, "26").shouldGetItemWithSameId()
        MediaId(TYPE_ARTISTS, "26", 65).shouldGetItemWithSameId()
        MediaId(TYPE_PLAYLISTS, "1").shouldGetItemWithSameId()
        MediaId(TYPE_PLAYLISTS, "1", 865).shouldGetItemWithSameId()
    }

    private suspend fun MediaId.shouldGetItemWithSameId() {
        val requestedItem = browserTree.getItem(this)
            ?: failAssumption("Expected an item with id $this")
        requestedItem.id shouldBe this
    }

    @Test
    fun `When requesting any item, then that item should be in its parents children`() = runTest {
        MediaId(TYPE_ROOT) shouldHaveChild MediaId(TYPE_TRACKS)
        MediaId(TYPE_TRACKS) shouldHaveChild MediaId(TYPE_TRACKS, CATEGORY_ALL)
        MediaId(TYPE_TRACKS, CATEGORY_ALL) shouldHaveChild MediaId(TYPE_TRACKS, CATEGORY_ALL, 65)
        MediaId(TYPE_ALBUMS) shouldHaveChild MediaId(TYPE_ALBUMS, "40")
        MediaId(TYPE_ALBUMS, "40") shouldHaveChild MediaId(TYPE_ALBUMS, "40", 65)
        MediaId(TYPE_ARTISTS) shouldHaveChild MediaId(TYPE_ARTISTS, "13")
        MediaId(TYPE_ARTISTS, "26") shouldHaveChild MediaId(TYPE_ARTISTS, "26", 65)
        MediaId(TYPE_PLAYLISTS) shouldHaveChild MediaId(TYPE_PLAYLISTS, "1")
        MediaId(TYPE_PLAYLISTS, "1") shouldHaveChild MediaId(TYPE_PLAYLISTS, "1", 865)
    }

    private suspend infix fun MediaId.shouldHaveChild(itemId: MediaId) {
        val item = browserTree.getItem(itemId)
            ?: failAssumption("Expected $itemId to be an existing item")
        val parentChildren = browserTree.getChildren(this).first()

        parentChildren.shouldContain(item)
    }

    private suspend fun assertHasNoChildren(parentId: MediaId) {
        shouldThrow<NoSuchElementException> {
            browserTree.getChildren(parentId).first()
        }
    }

    private fun List<MediaContent>.shouldAllBeBrowsable() {
        val nonBrowsableItems = filterNot { it.browsable }

        if (nonBrowsableItems.isNotEmpty()) {
            fail(buildString {
                append("Expected all items to be browsable, but ")
                nonBrowsableItems.joinTo(this, ", ", "[", "]", 10) { it.id.encoded }
                append(" were not.")
            })
        }
    }

    private fun List<MediaContent>.shouldAllBePlayable() {
        val nonPlayableItems = filterNot { it.playable }

        if (nonPlayableItems.isNotEmpty()) {
            fail(buildString {
                append("Expected all items to be playable, but ")
                nonPlayableItems.joinTo(this, ", ", "[", "]", 10) { it.id.encoded }
                append(" weren't.")
            })
        }
    }

    private fun List<MediaContent>.noneShouldBePlayable() {
        val playableItems = filter { it.playable }

        if (playableItems.isNotEmpty()) {
            fail(buildString {
                append("Expected all items not to be playable, but ")
                playableItems.joinTo(this, ", ", "[", "]", 10) { it.id.encoded }
                append(" were.")
            })
        }
    }
}

/**
 * Browse the media tree recursively from the specified root node.
 *
 * @param parentId The root node from which the media tree should be explored.
 * @param action Any action to be performed on each node.
 */
private suspend fun BrowserTree.walk(
    parentId: MediaId,
    action: suspend (child: MediaContent, parentId: MediaId) -> Unit
) {
    try {
        val children = getChildren(parentId).first()
        for (child in children) {
            action(child, parentId)
            walk(child.id, action)
        }
    } catch (nonBrowsableParent: NoSuchElementException) {
        // Stop walking down the media tree when an item is not browsable.
    }
}

private fun AudioTrack.withId(type: String = id.type, category: String? = id.category): AudioTrack =
    copy(
        id = id.copy(
            type = type,
            category = category,
        )
    )
