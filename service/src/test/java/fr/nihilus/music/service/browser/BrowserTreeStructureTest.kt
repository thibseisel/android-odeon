/*
 * Copyright 2019 Thibault Seisel
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
import android.support.v4.media.MediaBrowserCompat.MediaItem
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_DISPOSABLE
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_MOST_RATED
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_POPULAR
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_RECENTLY_ADDED
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ALBUMS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ARTISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_PLAYLISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ROOT
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.core.media.MediaId.Builder.encode
import fr.nihilus.music.core.media.MediaItems
import fr.nihilus.music.core.media.toMediaId
import fr.nihilus.music.core.test.coroutines.test
import fr.nihilus.music.core.test.fail
import fr.nihilus.music.core.test.failAssumption
import fr.nihilus.music.media.repo.ChangeNotification
import fr.nihilus.music.media.usage.DisposableTrack
import fr.nihilus.music.service.assertOn
import fr.nihilus.music.service.generateRandomTrackSequence
import io.kotlintest.inspectors.forOne
import io.kotlintest.matchers.collections.*
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.matchers.withClue
import io.kotlintest.should
import io.kotlintest.shouldBe
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.truth.os.BundleSubject.assertThat as assertThatBundle

/**
 * Validate the structure of the [BrowserTree]:
 * - tree can be browsed from the root to the topmost leafs,
 * - children of those nodes are correctly fetched and mapped to [MediaItem]s.
 */
@RunWith(AndroidJUnit4::class)
internal class BrowserTreeStructureTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `When loading children of Root, then return all available types`() = runBlockingTest {
        val rootChildren = loadChildrenOf(MediaId(TYPE_ROOT))

        rootChildren.ids.shouldContainExactlyInAnyOrder(
            TYPE_TRACKS,
            TYPE_ARTISTS,
            TYPE_ALBUMS,
            TYPE_PLAYLISTS
        )

        assertThatAllAreBrowsableAmong(rootChildren)
        assertThatNoneArePlayableAmong(rootChildren)
    }

    @Test
    fun `When loading children of Track type, then return track categories`() = runBlockingTest {
        val trackTypeChildren = loadChildrenOf(MediaId(TYPE_TRACKS))

        trackTypeChildren.ids.shouldContainExactlyInAnyOrder(
            "$TYPE_TRACKS/$CATEGORY_ALL",
            "$TYPE_TRACKS/$CATEGORY_RECENTLY_ADDED",
            "$TYPE_TRACKS/$CATEGORY_MOST_RATED",
            "$TYPE_TRACKS/$CATEGORY_DISPOSABLE",
            "$TYPE_TRACKS/$CATEGORY_POPULAR"
        )

        assertThatAllAreBrowsableAmong(trackTypeChildren)
        assertThatNoneArePlayableAmong(trackTypeChildren)
    }

    @Test
    fun `When loading children of Album type, then return all albums from repository`() = runBlockingTest {
        val albumTypeChildren = loadChildrenOf(MediaId(TYPE_ALBUMS))

        albumTypeChildren.ids.shouldContainExactly(
            "$TYPE_ALBUMS/40",
            "$TYPE_ALBUMS/38",
            "$TYPE_ALBUMS/102",
            "$TYPE_ALBUMS/95",
            "$TYPE_ALBUMS/7",
            "$TYPE_ALBUMS/6",
            "$TYPE_ALBUMS/65",
            "$TYPE_ALBUMS/26"
        )

        assertThatAllAreBrowsableAmong(albumTypeChildren)
    }

    @Test
    fun `When loading children of album type, then return items from album metadata`() = runBlockingTest {
        val allAlbums = loadChildrenOf(MediaId(TYPE_ALBUMS))
        val anAlbum = allAlbums.requireItemWith(MediaId(TYPE_ALBUMS, "40"))

        with(anAlbum.description) {
            title shouldBe "The 2nd Law"
            subtitle shouldBe "Muse"
        }

        assertThatBundle(anAlbum.description.extras).integer(MediaItems.EXTRA_NUMBER_OF_TRACKS).isEqualTo(1)
    }

    @Test
    fun `When loading children of Artist type, then return all artists from repository`() = runBlockingTest {
        val allArtists = loadChildrenOf(MediaId(TYPE_ARTISTS))

        allArtists.ids.shouldContainExactly(
            "$TYPE_ARTISTS/5",
            "$TYPE_ARTISTS/26",
            "$TYPE_ARTISTS/4",
            "$TYPE_ARTISTS/13",
            "$TYPE_ARTISTS/18"
        )

        assertThatAllAreBrowsableAmong(allArtists)
    }

    @Test
    fun `When loading children of Artist type, then return items from artist metadata`() = runBlockingTest {
        val allArtists = loadChildrenOf(MediaId(TYPE_ARTISTS))
        val anArtist = allArtists.requireItemWith(MediaId(TYPE_ARTISTS, "5"))

        with(anArtist.description) {
            title shouldBe "AC/DC"
            // TODO Use a plural string resource instead.
            subtitle shouldBe "1 albums, 2 tracks"

            assertOn(extras) {
                integer(MediaItems.EXTRA_NUMBER_OF_TRACKS).isEqualTo(2)
            }
        }
    }

    @Test
    fun `When loading children of Playlist type, then return all playlists`() = runBlockingTest {
        val allPlaylists = loadChildrenOf(MediaId(TYPE_PLAYLISTS))

        allPlaylists.ids.shouldContainExactly(
            "$TYPE_PLAYLISTS/1",
            "$TYPE_PLAYLISTS/2",
            "$TYPE_PLAYLISTS/3"
        )

        assertThatAllAreBrowsableAmong(allPlaylists)
    }

    @Test
    fun `When loading children of Playlist type, then return items from playlist metadata`() = runBlockingTest {
        val allPlaylists = loadChildrenOf(MediaId(TYPE_PLAYLISTS))

        val aPlaylist = allPlaylists.requireItemWith(MediaId(TYPE_PLAYLISTS, "1"))
        aPlaylist.description.title shouldBe "Zen"
    }

    @Test
    fun `Given any browsable parent, when loading its children then never return null`() = runBlockingTest {
        val repository = TestMediaRepository()
        val browserTree = BrowserTreeImpl(context, repository, TestUsageManager())

        browserTree.walk(MediaId(TYPE_ROOT)) { child, parentId ->
            if(child.isBrowsable) {
                val childId = child.mediaId.toMediaId()
                val children = browserTree.getChildren(childId, null)

                withClue("Browsable item $childId (child of $parentId) should have children.") {
                    children.shouldNotBeNull()
                }
            }
        }
    }

    @Test
    fun `Given any non browsable item, when loading its children then return null`() = runBlockingTest {
        val repository = TestMediaRepository()
        val browserTree = BrowserTreeImpl(context, repository, TestUsageManager())

        browserTree.walk(MediaId(TYPE_ROOT)) { child, parentId ->
            if (!child.isBrowsable) {
                val childId = child.mediaId.toMediaId()
                val children = browserTree.getChildren(childId, null)

                withClue("Non-browsable item $childId (child of $parentId) should not have children.") {
                    children.shouldBeNull()
                }
            }
        }
    }

    @Test
    fun `When loading children of All Tracks, then return all tracks from repository`() = runBlockingTest {
        val allTracks = loadChildrenOf(MediaId(TYPE_TRACKS, CATEGORY_ALL))

        allTracks.ids.shouldContainExactly(
            "$TYPE_TRACKS/$CATEGORY_ALL|161",
            "$TYPE_TRACKS/$CATEGORY_ALL|309",
            "$TYPE_TRACKS/$CATEGORY_ALL|481",
            "$TYPE_TRACKS/$CATEGORY_ALL|48",
            "$TYPE_TRACKS/$CATEGORY_ALL|125",
            "$TYPE_TRACKS/$CATEGORY_ALL|294",
            "$TYPE_TRACKS/$CATEGORY_ALL|219",
            "$TYPE_TRACKS/$CATEGORY_ALL|75",
            "$TYPE_TRACKS/$CATEGORY_ALL|464",
            "$TYPE_TRACKS/$CATEGORY_ALL|477"
        )

        assertThatAllArePlayableAmong(allTracks)
        assertThatNoneAreBrowsableAmong(allTracks)
    }

    @Test
    fun `When loading children of All Tracks, then return items from track metadata`() = runBlockingTest {
        val allTracks = loadChildrenOf(MediaId(TYPE_TRACKS, CATEGORY_ALL))
        val aTrack = allTracks.requireItemWith(MediaId(TYPE_TRACKS, CATEGORY_ALL, 125L))

        with(aTrack.description) {
            title shouldBe "Jailbreak"
            subtitle shouldBe "AC/DC"

            assertOn(extras) {
                containsKey(MediaItems.EXTRA_DURATION)
                integer(MediaItems.EXTRA_DISC_NUMBER).isEqualTo(2)
                integer(MediaItems.EXTRA_TRACK_NUMBER).isEqualTo(14)
            }
        }
    }

    @Test
    fun `When loading children of Most Rated, then return most rated tracks from usage manager`() = runBlockingTest {
        val mostRatedTracks = loadChildrenOf(MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED))

        mostRatedTracks.ids.shouldContainExactly(
            "$TYPE_TRACKS/$CATEGORY_MOST_RATED|75",
            "$TYPE_TRACKS/$CATEGORY_MOST_RATED|464",
            "$TYPE_TRACKS/$CATEGORY_MOST_RATED|48",
            "$TYPE_TRACKS/$CATEGORY_MOST_RATED|477",
            "$TYPE_TRACKS/$CATEGORY_MOST_RATED|294"
        )

        assertThatAllArePlayableAmong(mostRatedTracks)
        assertThatNoneAreBrowsableAmong(mostRatedTracks)
    }

    @Test
    fun `When loading children of Most Rated, then return items from track metadata`() = runBlockingTest {
        val mostRecentTracks = loadChildrenOf(MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED))
        val aTrack = mostRecentTracks.requireItemWith(MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED, 75L))

        with(aTrack.description) {
            title shouldBe "Nightmare"
            subtitle shouldBe "Avenged Sevenfold"

            assertOn(extras) {
                containsKey(MediaItems.EXTRA_DURATION)
                integer(MediaItems.EXTRA_DISC_NUMBER).isEqualTo(1)
                integer(MediaItems.EXTRA_TRACK_NUMBER).isEqualTo(1)
            }
        }
    }

    @Test
    fun `When loading children of Recently Added, then return tracks sorted by descending availability date`() = runBlockingTest {
        val mostRecentTracks = loadChildrenOf(MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED))

        mostRecentTracks.ids.shouldContainExactly(
            "$TYPE_TRACKS/$CATEGORY_RECENTLY_ADDED|481", // September 25th, 2019 (21:22)
            "$TYPE_TRACKS/$CATEGORY_RECENTLY_ADDED|477", // September 25th, 2019 (21:22)
            "$TYPE_TRACKS/$CATEGORY_RECENTLY_ADDED|161", // June 18th, 2016
            "$TYPE_TRACKS/$CATEGORY_RECENTLY_ADDED|125", // February 12th, 2016 (21:49)
            "$TYPE_TRACKS/$CATEGORY_RECENTLY_ADDED|48",  // February 12th, 2016 (21:48)
            "$TYPE_TRACKS/$CATEGORY_RECENTLY_ADDED|309", // August 15th, 2015 (15:49)
            "$TYPE_TRACKS/$CATEGORY_RECENTLY_ADDED|464", // August 15th, 2015 (15:49)
            "$TYPE_TRACKS/$CATEGORY_RECENTLY_ADDED|75",  // August 14th, 2015
            "$TYPE_TRACKS/$CATEGORY_RECENTLY_ADDED|294", // November 1st, 2014
            "$TYPE_TRACKS/$CATEGORY_RECENTLY_ADDED|219"  // February 12th, 2013
        )
    }

    @Test
    fun `When loading children of Recently Added, then return only playable items`() = runBlockingTest {
        val mostRecentTracks = loadChildrenOf(MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED))

        assertThatAllArePlayableAmong(mostRecentTracks)
        assertThatNoneAreBrowsableAmong(mostRecentTracks)
    }

    @Test
    fun `When loading children of Recently Added, then return no more than 25 tracks`() = runBlockingTest {
        val testTracks = generateRandomTrackSequence().take(50).toList()
        val repository = TestMediaRepository(tracks = testTracks)
        val browserTree = BrowserTreeImpl(context, repository, StubUsageManager)

        val mostRecentTracks = browserTree.getChildren(
            MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED),
            options = null
        ) ?: failAssumption("Expected $TYPE_TRACKS/$CATEGORY_RECENTLY_ADDED to have children")

        mostRecentTracks shouldHaveAtMostSize 25
    }

    @Test
    fun `When loading children of Recently Added, then return items from track metadata`() = runBlockingTest {
        val mostRecentTracks = loadChildrenOf(MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED))
        val aTrack = mostRecentTracks.requireItemWith(MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED, 481L))

        with(aTrack.description) {
            title shouldBe "Dirty Water"
            subtitle shouldBe "Foo Fighters"

            assertOn(extras) {
                containsKey(MediaItems.EXTRA_DURATION)
                integer(MediaItems.EXTRA_DISC_NUMBER).isEqualTo(1)
                integer(MediaItems.EXTRA_TRACK_NUMBER).isEqualTo(6)
            }
        }
    }

    @Test
    fun `When loading children of Disposable, then return disposable items from usage manager`() = runBlockingTest {
        val usageManager = TestUsageManager(emptyList(), disposableTracks = listOf(
            DisposableTrack(48L, "Give It Up", 5_716_578, null),
            DisposableTrack(161L, "1741 (The Battle of Cartagena)", 17_506_481, 1565272800)
        ))

        val browserTree = BrowserTreeImpl(context, StubMediaRepository(), usageManager)
        val children = browserTree.getChildren(MediaId(TYPE_TRACKS, CATEGORY_DISPOSABLE), null)

        children.shouldNotBeNull()
        children.shouldHaveSize(2)

        children[0] should {
            it.mediaId shouldBe "$TYPE_TRACKS/$CATEGORY_DISPOSABLE|48"
            it.description.title shouldBe "Give It Up"

            assertOn(it.description.extras) {
                longInt(MediaItems.EXTRA_FILE_SIZE).isEqualTo(5_716_578)
                doesNotContainKey(MediaItems.EXTRA_LAST_PLAYED_TIME)
            }
        }

        children[1] should {
            it.mediaId shouldBe "$TYPE_TRACKS/$CATEGORY_DISPOSABLE|161"
            it.description.title shouldBe "1741 (The Battle of Cartagena)"

            assertOn(it.description.extras) {
                longInt(MediaItems.EXTRA_FILE_SIZE).isEqualTo(17_506_481)
                longInt(MediaItems.EXTRA_LAST_PLAYED_TIME).isEqualTo(1565272800)
            }
        }
    }

    @Test
    fun `When loading children of an album, then return tracks from that album`() = runBlockingTest {
        assertAlbumHasTracksChildren(65L, listOf("$TYPE_ALBUMS/65|161"))
        assertAlbumHasTracksChildren(102L, listOf(
            "$TYPE_ALBUMS/102|477",
            "$TYPE_ALBUMS/102|481"
        ))
        assertAlbumHasTracksChildren(7L, listOf(
            "$TYPE_ALBUMS/7|48",
            "$TYPE_ALBUMS/7|125"
        ))
    }

    private suspend fun assertAlbumHasTracksChildren(albumId: Long, expectedMediaIds: List<String>) {
        val children = loadChildrenOf(MediaId(TYPE_ALBUMS, albumId.toString()))
        assertThatAllArePlayableAmong(children)
        assertThatNoneAreBrowsableAmong(children)

        children.ids.shouldContainExactly(expectedMediaIds)
    }

    @Test
    fun `When loading children of an artist, then return its albums followed by its tracks`() = runBlockingTest {
        val artistChildren = loadChildrenOf(MediaId(TYPE_ARTISTS, "18"))
        val indexOfFirstTrack = artistChildren.indexOfFirst { it.mediaId.toMediaId().track != null }
        val childrenAfterAlbums = artistChildren.subList(indexOfFirstTrack, artistChildren.size)

        val nonTracksAfterAlbums = childrenAfterAlbums.filter { it.mediaId.toMediaId().track == null }
        nonTracksAfterAlbums.shouldBeEmpty()
    }

    @Test
    fun `When loading children of an artist, then return albums from that artist sorted by desc release date`() = runBlockingTest {
        assertArtistHasAlbumsChildren(26L, listOf("$TYPE_ALBUMS/65"))
        assertArtistHasAlbumsChildren(18L, listOf("$TYPE_ALBUMS/40", "$TYPE_ALBUMS/38"))
        assertArtistHasAlbumsChildren(13L, listOf("$TYPE_ALBUMS/102", "$TYPE_ALBUMS/26", "$TYPE_ALBUMS/95"))
    }

    private suspend fun assertArtistHasAlbumsChildren(artistId: Long, expectedAlbumIds: List<String>) {
        val artistChildren = loadChildrenOf(MediaId(TYPE_ARTISTS, artistId.toString()))
        val artistAlbums = artistChildren.filter { it.mediaId.toMediaId().track == null }

        assertThatAllAreBrowsableAmong(artistAlbums)
        artistAlbums.ids.shouldContainExactly(expectedAlbumIds)
    }

    @Test
    fun `When loading children of an artist, then return tracks from that artist sorted alphabetically`() = runBlockingTest {
        assertArtistHasTracksChildren(26L, listOf("$TYPE_ARTISTS/26|161"))
        assertArtistHasTracksChildren(18L, listOf(
            "$TYPE_ARTISTS/18|309",
            "$TYPE_ARTISTS/18|294"
        ))
        assertArtistHasTracksChildren(13L, listOf(
            "$TYPE_ARTISTS/13|481",
            "$TYPE_ARTISTS/13|219",
            "$TYPE_ARTISTS/13|464",
            "$TYPE_ARTISTS/13|477"
        ))
    }

    @Test
    fun `When loading children of an artist, then return artist albums from metadata`() = runBlockingTest {
        val artistChildren = loadChildrenOf(MediaId(TYPE_ARTISTS, "26"))
        val anAlbum = artistChildren.requireItemWith(MediaId(TYPE_ALBUMS, "65"))

        with(anAlbum.description) {
            title shouldBe "Sunset on the Golden Age"
            assertOn(extras) {
                integer(MediaItems.EXTRA_NUMBER_OF_TRACKS).isEqualTo(1)
            }
        }
    }

    @Test
    fun `When loading children of an artist, then return artist tracks from metadata`() = runBlockingTest {
        val artistChildren = loadChildrenOf(MediaId(TYPE_ARTISTS, "26"))
        val aTrack = artistChildren.requireItemWith(MediaId(TYPE_ARTISTS, "26", 161L))

        with(aTrack.description) {
            title shouldBe "1741 (The Battle of Cartagena)"

            assertOn(extras) {
                containsKey(MediaItems.EXTRA_DURATION)
            }
        }
    }

    @Test
    fun `When loading children of a playlist, then return tracks from that playlist`() = runBlockingTest {
        assertPlaylistHasTracks(1L, listOf("$TYPE_PLAYLISTS/1|309"))
        assertPlaylistHasTracks(2L, listOf(
            "$TYPE_PLAYLISTS/2|477",
            "$TYPE_PLAYLISTS/2|48",
            "$TYPE_PLAYLISTS/2|125"
        ))
    }

    @Test
    fun `When loading children of a playlist, then return items from track metadata`() = runBlockingTest {
        val playlistChildren = loadChildrenOf(MediaId(TYPE_PLAYLISTS, "1"))
        val aPlaylistTrack = playlistChildren.requireItemWith(MediaId(TYPE_PLAYLISTS, "1", 309L))

        with(aPlaylistTrack.description) {
            title shouldBe "The 2nd Law: Isolated System"
            assertOn(extras) {
                containsKey(MediaItems.EXTRA_DURATION)
            }
        }
    }

    @Test
    fun `Given an unknown category, when loading its children then return null`() = runBlockingTest {
        assertHasNoChildren(MediaId("unknown"))
        assertHasNoChildren(MediaId(TYPE_TRACKS, "undefined"))
        assertHasNoChildren(MediaId(TYPE_ALBUMS, "1234"))
        assertHasNoChildren(MediaId(TYPE_ARTISTS, "1234"))
        assertHasNoChildren(MediaId(TYPE_PLAYLISTS, "1234"))
    }

    @Test
    fun `When requesting any item, then return an item with the same id as requested`() = runBlockingTest {
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
        val repository = TestMediaRepository()
        val browserTree = BrowserTreeImpl(context, repository, StubUsageManager)

        val requestedItem = browserTree.getItem(itemId)
            ?: failAssumption("Expected an item with id $itemId")
        requestedItem.mediaId shouldBe itemId.encoded
    }

    @Test
    fun `When requesting any item, then that item should be in its parents children`() = runBlockingTest {
        assertItemIsPartOfItsParentsChildren(MediaId(TYPE_ROOT), MediaId(TYPE_TRACKS))
        assertItemIsPartOfItsParentsChildren(MediaId(TYPE_TRACKS), MediaId(TYPE_TRACKS, CATEGORY_ALL))
        assertItemIsPartOfItsParentsChildren(MediaId(TYPE_TRACKS, CATEGORY_ALL), MediaId(TYPE_TRACKS, CATEGORY_ALL, 477L))
        assertItemIsPartOfItsParentsChildren(MediaId(TYPE_ALBUMS), MediaId(TYPE_ALBUMS, "102"))
        assertItemIsPartOfItsParentsChildren(MediaId(TYPE_ALBUMS, "102"), MediaId(TYPE_ALBUMS, "102", 477L))
        assertItemIsPartOfItsParentsChildren(MediaId(TYPE_ARTISTS), MediaId(TYPE_ARTISTS, "13"))
        assertItemIsPartOfItsParentsChildren(MediaId(TYPE_ARTISTS, "13"), MediaId(TYPE_ARTISTS, "13", 477L))
        assertItemIsPartOfItsParentsChildren(MediaId(TYPE_PLAYLISTS), MediaId(TYPE_PLAYLISTS, "2"))
        assertItemIsPartOfItsParentsChildren(MediaId(TYPE_PLAYLISTS, "2"), MediaId(TYPE_PLAYLISTS, "2", 477L))
    }

    @Test
    fun `Given pages of size N, when loading children then return the N first items`() = runBlockingTest {
        val repository = TestMediaRepository()
        val browserTree = BrowserTreeImpl(context, repository, StubUsageManager)

        val paginatedChildren = browserTree.getChildren(
            MediaId(TYPE_TRACKS, CATEGORY_ALL),
            PaginationOptions(0, 3)
        ) ?: failAssumption("Assumed item with id $TYPE_TRACKS/$CATEGORY_ALL to have children.")

        paginatedChildren.ids.shouldContainExactly(
            encode(TYPE_TRACKS, CATEGORY_ALL, 161),
            encode(TYPE_TRACKS, CATEGORY_ALL, 309),
            encode(TYPE_TRACKS, CATEGORY_ALL, 481)
        )
    }

    @Test
    fun `Given the page X of size N, when loading children then return N items from position NX`() = runBlockingTest {
        val repository = TestMediaRepository()
        val browserTree = BrowserTreeImpl(context, repository, StubUsageManager)

        val paginatedChildren = browserTree.getChildren(
            MediaId(TYPE_TRACKS, CATEGORY_ALL),
            PaginationOptions(3, 2)
        ) ?: failAssumption("Assumed item with id $TYPE_TRACKS/$CATEGORY_ALL to have children.")

        paginatedChildren.ids.shouldContainExactly(
            encode(TYPE_TRACKS, CATEGORY_ALL, 219),
            encode(TYPE_TRACKS, CATEGORY_ALL, 75)
        )
    }

    @Test
    fun `Given a page after the last page, when loading children then return no children`() = runBlockingTest {
        val repository = TestMediaRepository()
        val browserTree = BrowserTreeImpl(context, repository, StubUsageManager)

        val pagePastChildren = browserTree.getChildren(
            MediaId(TYPE_TRACKS, CATEGORY_ALL),
            PaginationOptions(2, 5)
        ) ?: failAssumption("Assumed item with id $TYPE_TRACKS/$CATEGORY_ALL to have children.")

        pagePastChildren.shouldBeEmpty()
    }

    @Test
    fun `When receiving change notification, then map to the corresponding media id`() {
        assertNotifyParentChanged(ChangeNotification.AllTracks, MediaId(TYPE_TRACKS, CATEGORY_ALL))
        assertNotifyParentChanged(ChangeNotification.AllTracks, MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED))
        assertNotifyParentChanged(ChangeNotification.AllTracks, MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED))
        assertNotifyParentChanged(ChangeNotification.AllTracks, MediaId(TYPE_TRACKS, CATEGORY_DISPOSABLE))

        assertNotifyParentChanged(ChangeNotification.AllAlbums, MediaId(TYPE_ALBUMS))
        assertNotifyParentChanged(ChangeNotification.Album(40L), MediaId(TYPE_ALBUMS, "40"))

        assertNotifyParentChanged(ChangeNotification.AllArtists, MediaId(TYPE_ARTISTS))
        assertNotifyParentChanged(ChangeNotification.Artist(5L), MediaId(TYPE_ARTISTS, "5"))

        assertNotifyParentChanged(ChangeNotification.AllPlaylists, MediaId(TYPE_PLAYLISTS))
        assertNotifyParentChanged(ChangeNotification.Playlist(1L), MediaId(TYPE_PLAYLISTS, "1"))
    }

    private fun assertNotifyParentChanged(notification: ChangeNotification, changedParentId: MediaId) {
        val changeNotifier = PublishProcessor.create<ChangeNotification>()
        val repository = TestMediaRepository(changeNotifications = changeNotifier)
        val browserTree = BrowserTreeImpl(context, repository, StubUsageManager)

        val subscriber = browserTree.updatedParentIds.test()
        changeNotifier.onNext(notification)

        assertThat(subscriber.values()).contains(changedParentId)
    }

    private suspend fun assertItemIsPartOfItsParentsChildren(parentId: MediaId, itemId: MediaId) {
        val repository = TestMediaRepository()
        val browserTree = BrowserTreeImpl(context, repository, StubUsageManager)

        val item = browserTree.getItem(itemId)
            ?: failAssumption("Expected $itemId to be an existing item")
        val parentChildren = browserTree.getChildren(parentId, null)
            ?: failAssumption("Expected $parentId to have children")

        parentChildren.forOne {
            it.mediaId shouldBe item.mediaId
            it.isBrowsable shouldBe item.isBrowsable
            it.isPlayable shouldBe item.isPlayable

            with (it.description) {
                title shouldBe item.description.title
                subtitle shouldBe item.description.subtitle
                iconUri shouldBe item.description.iconUri
                mediaUri shouldBe item.description.mediaUri
            }
        }
    }

    private suspend fun assertHasNoChildren(parentId: MediaId) {
        val browserTree = BrowserTreeImpl(context, TestMediaRepository(), StubUsageManager)
        val children = browserTree.getChildren(parentId, null)
        children.shouldBeNull()
    }

    private suspend fun assertPlaylistHasTracks(playlistId: Long, expectedTrackIds: List<String>) {
        val playlistChildren = loadChildrenOf(MediaId(TYPE_PLAYLISTS, playlistId.toString()))

        assertThatAllArePlayableAmong(playlistChildren)
        assertThatNoneAreBrowsableAmong(playlistChildren)

        playlistChildren.ids.shouldContainExactly(expectedTrackIds)
    }

    /**
     * Assume that the given collection of media items contains a media with the specified [media id][itemId],
     * and if it does, return it ; otherwise the test execution is stopped due to assumption failure.
     */
    private fun List<MediaItem>.requireItemWith(itemId: MediaId): MediaItem {
        return find { it.mediaId == itemId.encoded } ?: failAssumption(buildString {
            append("Missing an item with id = $itemId in ")
            this@requireItemWith.joinTo(this, ", ", "[", "]", 10) {
                it.mediaId.orEmpty()
            }
        })
    }

    private suspend fun assertArtistHasTracksChildren(artistId: Long, expectedTrackIds: List<String>) {
        val artistChildren = loadChildrenOf(MediaId(TYPE_ARTISTS, artistId.toString()))
        val artistTracks = artistChildren.filter { it.mediaId.toMediaId().track != null }

        assertThatAllArePlayableAmong(artistTracks)
        assertThatNoneAreBrowsableAmong(artistTracks)

        artistTracks.ids.shouldContainExactly(expectedTrackIds)
    }

    private suspend fun loadChildrenOf(parentId: MediaId): List<MediaItem> {
        val repository = TestMediaRepository()
        val usageManager = TestUsageManager()
        val browserTree = BrowserTreeImpl(context, repository, usageManager)

        val children = browserTree.getChildren(parentId, null)
        return children ?: fail("Expected $parentId to have children.")
    }

    private fun assertThatAllAreBrowsableAmong(children: List<MediaItem>) {
        val nonBrowsableItems = children.filterNot { it.isBrowsable }

        if (nonBrowsableItems.isNotEmpty()) {
            fail(buildString {
                append("Expected all items to be browsable, but ")
                nonBrowsableItems.joinTo(this, ", ", "[", "]", 10) { it.mediaId.orEmpty() }
                append(" were not.")
            })
        }
    }

    private fun assertThatAllArePlayableAmong(children: List<MediaItem>) {
        val nonPlayableItems = children.filterNot(MediaItem::isPlayable)

        if (nonPlayableItems.isNotEmpty()) {
            fail(buildString {
                append("Expected all items to be playable, but ")
                nonPlayableItems.joinTo(this, ", ", "[", "]", 10) { it.mediaId.orEmpty() }
                append(" weren't.")
            })
        }
    }

    private fun assertThatNoneArePlayableAmong(children: List<MediaItem>) {
        val playableItems = children.filter { it.isPlayable }

        if (playableItems.isNotEmpty()) {
            fail(buildString {
                append("Expected all items not to be playable, but ")
                playableItems.joinTo(this, ", ", "[", "]", 10) { it.mediaId.orEmpty() }
                append(" were.")
            })
        }
    }

    private fun assertThatNoneAreBrowsableAmong(children: List<MediaItem>) {
        val browsableItems = children.filter(MediaItem::isBrowsable)

        if (browsableItems.isNotEmpty()) {
            fail(buildString {
                append("Expected all items not to be browsable, but ")
                browsableItems.joinTo(this, ", ", "[", "]", 10) { it.mediaId.orEmpty() }
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
    action: suspend (child: MediaItem, parentId: MediaId) -> Unit
) {
    getChildren(parentId, null)?.forEach { child ->
        action(child, parentId)
        walk(child.mediaId.toMediaId(), action)
    }
}

private val List<MediaItem>.ids: List<String?>
    get() = map { it.mediaId }