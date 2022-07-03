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

import android.content.Context
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
import fr.nihilus.music.service.generateRandomTrackSequence
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.extracting
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Validate the structure of the [BrowserTree]:
 * - tree can be browsed from the root to the topmost leafs,
 * - children of those nodes are correctly fetched and mapped to [MediaContent]s.
 */
@RunWith(AndroidJUnit4::class)
internal class BrowserTreeTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `When loading children of Root, then return all available types`() = runTest {
        val rootChildren = loadChildrenOf(MediaId(TYPE_ROOT))

        extracting(rootChildren) { id }.shouldContainExactlyInAnyOrder(
            MediaId(TYPE_TRACKS),
            MediaId(TYPE_ARTISTS),
            MediaId(TYPE_ALBUMS),
            MediaId(TYPE_PLAYLISTS),
        )

        assertThatAllAreBrowsableAmong(rootChildren)
        assertThatNoneArePlayableAmong(rootChildren)
    }

    @Test
    fun `When loading children of Track type, then return track categories`() = runTest {
        val trackTypeChildren = loadChildrenOf(MediaId(TYPE_TRACKS))

        extracting(trackTypeChildren) { id }.shouldContainExactlyInAnyOrder(
            MediaId(TYPE_TRACKS, CATEGORY_ALL),
            MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED),
            MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED),
            MediaId(TYPE_TRACKS, CATEGORY_POPULAR)
        )

        assertThatAllAreBrowsableAmong(trackTypeChildren)
        assertThatAllArePlayableAmong(trackTypeChildren)
    }

    @Test
    fun `When loading children of Album type, then return all albums from repository`() = runTest {
        val albumTypeChildren = loadChildrenOf(MediaId(TYPE_ALBUMS))

        extracting(albumTypeChildren) { id }.shouldContainExactly(
            MediaId(TYPE_ALBUMS, "40"),
            MediaId(TYPE_ALBUMS, "38"),
            MediaId(TYPE_ALBUMS, "102"),
            MediaId(TYPE_ALBUMS, "95"),
            MediaId(TYPE_ALBUMS, "7"),
            MediaId(TYPE_ALBUMS, "6"),
            MediaId(TYPE_ALBUMS, "65"),
            MediaId(TYPE_ALBUMS, "26")
        )

        assertThatAllAreBrowsableAmong(albumTypeChildren)
        assertThatAllArePlayableAmong(albumTypeChildren)
    }

    @Test
    fun `When loading children of album type, then return items from album metadata`() = runTest {
        val allAlbums = loadChildrenOf(MediaId(TYPE_ALBUMS))
        val anAlbum = allAlbums.requireItemWith(MediaId(TYPE_ALBUMS, "40"))

        anAlbum.shouldBeTypeOf<MediaCategory>()
        assertSoftly(anAlbum) {
            title shouldBe "The 2nd Law"
            subtitle shouldBe "Muse"
            count shouldBe 1
        }
    }

    @Test
    fun `When loading children of Artist type, then return all artists from repository`() =
        runTest {
            val allArtists = loadChildrenOf(MediaId(TYPE_ARTISTS))

            extracting(allArtists) { id }.shouldContainExactly(
                MediaId(TYPE_ARTISTS, "5"),
                MediaId(TYPE_ARTISTS, "26"),
                MediaId(TYPE_ARTISTS, "4"),
                MediaId(TYPE_ARTISTS, "13"),
                MediaId(TYPE_ARTISTS, "18")
            )

            assertThatAllAreBrowsableAmong(allArtists)
            assertThatNoneArePlayableAmong(allArtists)
        }

    @Test
    fun `When loading children of Artist type, then return items from artist metadata`() = runTest {
        val allArtists = loadChildrenOf(MediaId(TYPE_ARTISTS))
        val anArtist = allArtists.requireItemWith(MediaId(TYPE_ARTISTS, "5"))

        anArtist.shouldBeTypeOf<MediaCategory>()
        assertSoftly(anArtist) {
            title shouldBe "AC/DC"
            // TODO Use a plural string resource instead.
            subtitle shouldBe "1 albums, 2 tracks"
            count shouldBe 2
        }
    }

    @Test
    fun `When loading children of Playlist type, then return all playlists`() = runTest {
        val allPlaylists = loadChildrenOf(MediaId(TYPE_PLAYLISTS))

        extracting(allPlaylists) { id }.shouldContainExactly(
            MediaId(TYPE_PLAYLISTS, "1"),
            MediaId(TYPE_PLAYLISTS, "2"),
            MediaId(TYPE_PLAYLISTS, "3")
        )

        assertThatAllAreBrowsableAmong(allPlaylists)
        assertThatAllArePlayableAmong(allPlaylists)
    }

    @Test
    fun `When loading children of Playlist type, then return items from playlist metadata`() =
        runTest {
            val allPlaylists = loadChildrenOf(MediaId(TYPE_PLAYLISTS))

            val aPlaylist = allPlaylists.requireItemWith(MediaId(TYPE_PLAYLISTS, "1"))
            aPlaylist.title shouldBe "Zen"
        }

    @Test
    fun `Given any browsable parent, when loading its children then never throw`() = runTest {
        val browserTree = BrowserTreeImpl(
            context,
            TestMediaDao(),
            TestPlaylistDao(),
            TestUsageManager(),
        )

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
            val browserTree = BrowserTreeImpl(
                context,
                TestMediaDao(),
                TestPlaylistDao(),
                TestUsageManager(),
            )

            browserTree.walk(MediaId(TYPE_ROOT)) { child, _ ->
                if (child !is MediaCategory) {
                    shouldThrow<NoSuchElementException> {
                        browserTree.getChildren(child.id).first()
                    }
                }
            }
        }

    @Test
    fun `When loading children of All Tracks, then return all tracks from repository`() = runTest {
        val allTracks = loadChildrenOf(MediaId(TYPE_TRACKS, CATEGORY_ALL))

        extracting(allTracks) { id }.shouldContainExactly(
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

        assertThatAllArePlayableAmong(allTracks)
        assertThatNoneAreBrowsableAmong(allTracks)
    }

    @Test
    fun `When loading children of All Tracks, then return items from track metadata`() = runTest {
        val allTracks = loadChildrenOf(MediaId(TYPE_TRACKS, CATEGORY_ALL))
        val aTrack = allTracks.requireItemWith(MediaId(TYPE_TRACKS, CATEGORY_ALL, 125L))

        aTrack.shouldBeTypeOf<AudioTrack>()
        assertSoftly(aTrack) {
            title shouldBe "Jailbreak"
            artist shouldBe "AC/DC"
            album shouldBe "Greatest Hits 30 Anniversary Edition"
            duration shouldBe 276668L
            disc shouldBe 2
            number shouldBe 14
        }
    }

    @Test
    fun `When loading children of Most Rated, then return most rated tracks from usage manager`() =
        runTest {
            val mostRatedTracks = loadChildrenOf(MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED))

            extracting(mostRatedTracks) { id }.shouldContainExactly(
                MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED, 75),
                MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED, 464),
                MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED, 48),
                MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED, 477),
                MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED, 294)
            )

            assertThatAllArePlayableAmong(mostRatedTracks)
            assertThatNoneAreBrowsableAmong(mostRatedTracks)
        }

    @Test
    fun `When loading children of Most Rated, then return items from track metadata`() = runTest {
        val mostRecentTracks = loadChildrenOf(MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED))
        val aTrack =
            mostRecentTracks.requireItemWith(MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED, 75L))

        aTrack.shouldBeTypeOf<AudioTrack>()
        assertSoftly(aTrack) {
            title shouldBe "Nightmare"
            artist shouldBe "Avenged Sevenfold"
            album shouldBe "Nightmare"
            duration shouldBe 374648L
            disc shouldBe 1
            number shouldBe 1
        }
    }

    @Test
    fun `When loading children of Recently Added, then return tracks sorted by descending availability date`() =
        runTest {
            val mostRecentTracks = loadChildrenOf(MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED))

            extracting(mostRecentTracks) { id }.shouldContainExactly(
                MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED, 481), // September 25th, 2019 (21:22)
                MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED, 477), // September 25th, 2019 (21:22)
                MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED, 161), // June 18th, 2016
                MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED, 125), // February 12th, 2016 (21:49)
                MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED, 48),  // February 12th, 2016 (21:48)
                MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED, 309), // August 15th, 2015 (15:49)
                MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED, 464), // August 15th, 2015 (15:49)
                MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED, 75),  // August 14th, 2015
                MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED, 294), // November 1st, 2014
                MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED, 219)  // February 12th, 2013
            )
        }

    @Test
    fun `When loading children of Recently Added, then return only playable items`() = runTest {
        val mostRecentTracks = loadChildrenOf(MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED))

        assertThatAllArePlayableAmong(mostRecentTracks)
        assertThatNoneAreBrowsableAmong(mostRecentTracks)
    }

    @Test
    fun `When loading children of Recently Added, then return no more than 25 tracks`() = runTest {
        val testTracks = generateRandomTrackSequence().take(50).toList()
        val media = TestMediaDao(tracks = testTracks)
        val browserTree = BrowserTreeImpl(context, media, TestPlaylistDao(), StubUsageManager)

        val mostRecentTracks = browserTree.getChildren(
            MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED)
        ).first()

        mostRecentTracks shouldHaveAtMostSize 25
    }

    @Test
    fun `When loading children of Recently Added, then return items from track metadata`() =
        runTest {
            val mostRecentTracks = loadChildrenOf(MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED))
            val aTrack = mostRecentTracks.requireItemWith(
                MediaId(
                    TYPE_TRACKS,
                    CATEGORY_RECENTLY_ADDED,
                    481L
                )
            )

            aTrack.shouldBeTypeOf<AudioTrack>()
            assertSoftly(aTrack) {
                title shouldBe "Dirty Water"
                artist shouldBe "Foo Fighters"
                album shouldBe "Concrete and Gold"
                duration shouldBe 320914L
                disc shouldBe 1
                number shouldBe 6
            }
        }

    @Test
    fun `When loading children of an album, then return tracks from that album`() = runTest {
        assertAlbumHasTracksChildren(
            65L, listOf(
                MediaId(TYPE_ALBUMS, "65", 161)
            )
        )
        assertAlbumHasTracksChildren(
            102L, listOf(
                MediaId(TYPE_ALBUMS, "102", 477),
                MediaId(TYPE_ALBUMS, "102", 481)
            )
        )
        assertAlbumHasTracksChildren(
            7L, listOf(
                MediaId(TYPE_ALBUMS, "7", 48),
                MediaId(TYPE_ALBUMS, "7", 125)
            )
        )
    }

    private suspend fun assertAlbumHasTracksChildren(
        albumId: Long,
        expectedMediaIds: List<MediaId>
    ) {
        val children = loadChildrenOf(MediaId(TYPE_ALBUMS, albumId.toString()))
        assertThatAllArePlayableAmong(children)
        assertThatNoneAreBrowsableAmong(children)

        extracting(children) { id }.shouldContainExactly(expectedMediaIds)
    }

    @Test
    fun `When loading children of an artist, then return its albums followed by its tracks`() =
        runTest {
            val artistChildren = loadChildrenOf(MediaId(TYPE_ARTISTS, "18"))
            val indexOfFirstTrack = artistChildren.indexOfFirst { it.id.track != null }
            val childrenAfterAlbums = artistChildren.subList(indexOfFirstTrack, artistChildren.size)

            val nonTracksAfterAlbums = childrenAfterAlbums.filter { it.id.track == null }
            nonTracksAfterAlbums.shouldBeEmpty()
        }

    @Test
    fun `When loading children of an artist, then return albums from that artist sorted by desc release date`() =
        runTest {
            assertArtistHasAlbumsChildren(26L, listOf(MediaId(TYPE_ALBUMS, "65")))
            assertArtistHasAlbumsChildren(
                18L,
                listOf(MediaId(TYPE_ALBUMS, "40"), MediaId(TYPE_ALBUMS, "38"))
            )
            assertArtistHasAlbumsChildren(
                13L,
                listOf(
                    MediaId(TYPE_ALBUMS, "102"),
                    MediaId(TYPE_ALBUMS, "26"),
                    MediaId(TYPE_ALBUMS, "95")
                )
            )
        }

    private suspend fun assertArtistHasAlbumsChildren(
        artistId: Long,
        expectedAlbumIds: List<MediaId>
    ) {
        val artistChildren = loadChildrenOf(MediaId(TYPE_ARTISTS, artistId.toString()))
        val artistAlbums = artistChildren.filter { it.id.track == null }

        assertThatAllAreBrowsableAmong(artistAlbums)
        assertThatAllArePlayableAmong(artistAlbums)
        extracting(artistAlbums) { id }.shouldContainExactly(expectedAlbumIds)
    }

    @Test
    fun `When loading children of an artist, then return tracks from that artist sorted alphabetically`() =
        runTest {
            assertArtistHasTracksChildren(
                26L, listOf(
                    MediaId(TYPE_ARTISTS, "26", 161)
                )
            )
            assertArtistHasTracksChildren(
                18L, listOf(
                    MediaId(TYPE_ARTISTS, "18", 309),
                    MediaId(TYPE_ARTISTS, "18", 294)
                )
            )
            assertArtistHasTracksChildren(
                13L, listOf(
                    MediaId(TYPE_ARTISTS, "13", 481),
                    MediaId(TYPE_ARTISTS, "13", 219),
                    MediaId(TYPE_ARTISTS, "13", 464),
                    MediaId(TYPE_ARTISTS, "13", 477)
                )
            )
        }

    @Test
    fun `When loading children of an artist, then return artist albums from metadata`() = runTest {
        val artistChildren = loadChildrenOf(MediaId(TYPE_ARTISTS, "26"))
        val anAlbum = artistChildren.requireItemWith(MediaId(TYPE_ALBUMS, "65"))

        anAlbum.shouldBeTypeOf<MediaCategory>()
        assertSoftly(anAlbum) {
            title shouldBe "Sunset on the Golden Age"
            count shouldBe 1
        }
    }

    @Test
    fun `When loading children of an artist, then return artist tracks from metadata`() = runTest {
        val artistChildren = loadChildrenOf(MediaId(TYPE_ARTISTS, "26"))
        val aTrack = artistChildren.requireItemWith(MediaId(TYPE_ARTISTS, "26", 161L))

        aTrack.shouldBeTypeOf<AudioTrack>()
        assertSoftly(aTrack) {
            title shouldBe "1741 (The Battle of Cartagena)"
            duration shouldBe 437603L
        }
    }

    @Test
    fun `When loading children of a playlist, then return tracks from that playlist`() = runTest {
        assertPlaylistHasTracks(
            1L, listOf(
                MediaId(TYPE_PLAYLISTS, "1", 309)
            )
        )
        assertPlaylistHasTracks(
            2L, listOf(
                MediaId(TYPE_PLAYLISTS, "2", 477),
                MediaId(TYPE_PLAYLISTS, "2", 48),
                MediaId(TYPE_PLAYLISTS, "2", 125)
            )
        )
    }

    @Test
    fun `When loading children of a playlist, then return items from track metadata`() = runTest {
        val playlistChildren = loadChildrenOf(MediaId(TYPE_PLAYLISTS, "1"))
        val aPlaylistTrack = playlistChildren.requireItemWith(MediaId(TYPE_PLAYLISTS, "1", 309L))

        aPlaylistTrack.shouldBeTypeOf<AudioTrack>()
        assertSoftly(aPlaylistTrack) {
            title shouldBe "The 2nd Law: Isolated System"
            duration shouldBe 300042L
        }
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
        assertLoadedItemHasSameMediaId(MediaId(TYPE_ROOT))
        assertLoadedItemHasSameMediaId(MediaId(TYPE_TRACKS, CATEGORY_ALL))
        assertLoadedItemHasSameMediaId(MediaId(TYPE_TRACKS, CATEGORY_ALL, 477L))
        assertLoadedItemHasSameMediaId(MediaId(TYPE_ALBUMS, "102"))
        assertLoadedItemHasSameMediaId(MediaId(TYPE_TRACKS))
        assertLoadedItemHasSameMediaId(MediaId(TYPE_ALBUMS, "102", 477L))
        assertLoadedItemHasSameMediaId(MediaId(TYPE_ARTISTS, "13"))
        assertLoadedItemHasSameMediaId(MediaId(TYPE_ARTISTS, "13", 477L))
        assertLoadedItemHasSameMediaId(MediaId(TYPE_PLAYLISTS, "2"))
        assertLoadedItemHasSameMediaId(MediaId(TYPE_PLAYLISTS, "2", 477L))
    }

    private suspend fun assertLoadedItemHasSameMediaId(itemId: MediaId) {
        val browserTree = BrowserTreeImpl(
            context,
            TestMediaDao(),
            TestPlaylistDao(),
            StubUsageManager,
        )

        val requestedItem = browserTree.getItem(itemId)
            ?: failAssumption("Expected an item with id $itemId")
        requestedItem.id shouldBe itemId
    }

    @Test
    fun `When requesting any item, then that item should be in its parents children`() = runTest {
        assertItemIsPartOfItsParentsChildren(MediaId(TYPE_ROOT), MediaId(TYPE_TRACKS))
        assertItemIsPartOfItsParentsChildren(
            MediaId(TYPE_TRACKS),
            MediaId(TYPE_TRACKS, CATEGORY_ALL)
        )
        assertItemIsPartOfItsParentsChildren(
            MediaId(TYPE_TRACKS, CATEGORY_ALL),
            MediaId(TYPE_TRACKS, CATEGORY_ALL, 477L)
        )
        assertItemIsPartOfItsParentsChildren(MediaId(TYPE_ALBUMS), MediaId(TYPE_ALBUMS, "102"))
        assertItemIsPartOfItsParentsChildren(
            MediaId(TYPE_ALBUMS, "102"),
            MediaId(TYPE_ALBUMS, "102", 477L)
        )
        assertItemIsPartOfItsParentsChildren(MediaId(TYPE_ARTISTS), MediaId(TYPE_ARTISTS, "13"))
        assertItemIsPartOfItsParentsChildren(
            MediaId(TYPE_ARTISTS, "13"),
            MediaId(TYPE_ARTISTS, "13", 477L)
        )
        assertItemIsPartOfItsParentsChildren(MediaId(TYPE_PLAYLISTS), MediaId(TYPE_PLAYLISTS, "2"))
        assertItemIsPartOfItsParentsChildren(
            MediaId(TYPE_PLAYLISTS, "2"),
            MediaId(TYPE_PLAYLISTS, "2", 477L)
        )
    }

    private suspend fun assertItemIsPartOfItsParentsChildren(parentId: MediaId, itemId: MediaId) {
        val browserTree = BrowserTreeImpl(
            context,
            TestMediaDao(),
            TestPlaylistDao(),
            StubUsageManager,
        )

        val item = browserTree.getItem(itemId)
            ?: failAssumption("Expected $itemId to be an existing item")
        val parentChildren = browserTree.getChildren(parentId).first()

        parentChildren.shouldContain(item)
    }

    private suspend fun assertHasNoChildren(parentId: MediaId) {
        val browserTree = BrowserTreeImpl(
            context,
            TestMediaDao(),
            TestPlaylistDao(),
            StubUsageManager,
        )

        shouldThrow<NoSuchElementException> {
            browserTree.getChildren(parentId).first()
        }
    }

    private suspend fun assertPlaylistHasTracks(playlistId: Long, expectedTrackIds: List<MediaId>) {
        val playlistChildren = loadChildrenOf(MediaId(TYPE_PLAYLISTS, playlistId.toString()))

        assertThatAllArePlayableAmong(playlistChildren)
        assertThatNoneAreBrowsableAmong(playlistChildren)

        extracting(playlistChildren) { id }.shouldContainExactly(expectedTrackIds)
    }

    /**
     * Assume that the given collection of media items contains a media with the specified [media id][itemId],
     * and if it does, return it ; otherwise the test execution is stopped due to assumption failure.
     */
    private fun List<MediaContent>.requireItemWith(itemId: MediaId): MediaContent {
        return find { it.id == itemId }
            ?: failAssumption(buildString {
                append("Missing an item with id = $itemId in ")
                joinTo(this, ", ", "[", "]", 10) { it.id.encoded }
            })
    }

    private suspend fun assertArtistHasTracksChildren(
        artistId: Long,
        expectedTrackIds: List<MediaId>
    ) {
        val artistChildren = loadChildrenOf(MediaId(TYPE_ARTISTS, artistId.toString()))
        val artistTracks = artistChildren.filter { it.id.track != null }

        assertThatAllArePlayableAmong(artistTracks)
        assertThatNoneAreBrowsableAmong(artistTracks)

        extracting(artistTracks) { id }.shouldContainExactly(expectedTrackIds)
    }

    private suspend fun loadChildrenOf(parentId: MediaId): List<MediaContent> {
        val browserTree = BrowserTreeImpl(
            context,
            TestMediaDao(),
            TestPlaylistDao(),
            TestUsageManager(),
        )
        return browserTree.getChildren(parentId).first()
    }

    private fun assertThatAllAreBrowsableAmong(children: List<MediaContent>) {
        val nonBrowsableItems = children.filterNot { it.browsable }

        if (nonBrowsableItems.isNotEmpty()) {
            fail(buildString {
                append("Expected all items to be browsable, but ")
                nonBrowsableItems.joinTo(this, ", ", "[", "]", 10) { it.id.encoded }
                append(" were not.")
            })
        }
    }

    private fun assertThatAllArePlayableAmong(children: List<MediaContent>) {
        val nonPlayableItems = children.filterNot { it.playable }

        if (nonPlayableItems.isNotEmpty()) {
            fail(buildString {
                append("Expected all items to be playable, but ")
                nonPlayableItems.joinTo(this, ", ", "[", "]", 10) { it.id.encoded }
                append(" weren't.")
            })
        }
    }

    private fun assertThatNoneArePlayableAmong(children: List<MediaContent>) {
        val playableItems = children.filter { it.playable }

        if (playableItems.isNotEmpty()) {
            fail(buildString {
                append("Expected all items not to be playable, but ")
                playableItems.joinTo(this, ", ", "[", "]", 10) { it.id.encoded }
                append(" were.")
            })
        }
    }

    private fun assertThatNoneAreBrowsableAmong(children: List<MediaContent>) {
        val browsableItems = children.filter { it.browsable }

        if (browsableItems.isNotEmpty()) {
            fail(buildString {
                append("Expected all items not to be browsable, but ")
                browsableItems.joinTo(this, ", ", "[", "]", 10) { it.id.encoded }
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
