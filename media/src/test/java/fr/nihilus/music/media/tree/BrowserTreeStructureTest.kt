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

package fr.nihilus.music.media.tree

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.truth.os.BundleSubject
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.TruthJUnit.assume
import fr.nihilus.music.media.*
import fr.nihilus.music.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.media.MediaId.Builder.CATEGORY_DISPOSABLE
import fr.nihilus.music.media.MediaId.Builder.CATEGORY_MOST_RATED
import fr.nihilus.music.media.MediaId.Builder.CATEGORY_RECENTLY_ADDED
import fr.nihilus.music.media.MediaId.Builder.TYPE_ALBUMS
import fr.nihilus.music.media.MediaId.Builder.TYPE_ARTISTS
import fr.nihilus.music.media.MediaId.Builder.TYPE_PLAYLISTS
import fr.nihilus.music.media.MediaId.Builder.TYPE_ROOT
import fr.nihilus.music.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.media.MediaId.Builder.encode
import fr.nihilus.music.media.provider.generateRandomTrackSequence
import fr.nihilus.music.media.repo.*
import fr.nihilus.music.media.repo.StubUsageManager
import fr.nihilus.music.media.repo.TestMediaRepository
import fr.nihilus.music.media.repo.TestUsageManager
import fr.nihilus.music.media.usage.DisposableTrack
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.reactivex.processors.PublishProcessor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.truth.os.BundleSubject.assertThat as assertThatBundle

/**
 * Browse the media tree recursively from the specified root node.
 *
 * @param parentId The root node from which the media tree should be explored.
 * @param action Any action to be performed on each node.
 */
private suspend fun BrowserTree.walk(parentId: MediaId, action: suspend (child: MediaItem, parentId: MediaId) -> Unit) {
    getChildren(parentId, null)?.forEach { child ->
        action(child, parentId)
        walk(child.mediaId.toMediaId(), action)
    }
}

@RunWith(AndroidJUnit4::class)
class BrowserTreeStructureTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun whenLoadingChildrenOfRoot_thenReturnAllAvailableTypes(): Unit = runBlocking {
        val rootChildren = loadChildrenOf(MediaId(TYPE_ROOT))

        assertThat(rootChildren).comparingElementsUsing(THEIR_MEDIA_ID)
            .containsExactly(TYPE_TRACKS, TYPE_ARTISTS, TYPE_ALBUMS, TYPE_PLAYLISTS)

        assertThatAllAreBrowsableAmong(rootChildren)
        assertThatNoneArePlayableAmong(rootChildren)
    }

    @Test
    fun whenLoadingChildrenOfTrackType_thenReturnTrackCategories(): Unit = runBlocking {
        val trackTypeChildren = loadChildrenOf(MediaId(TYPE_TRACKS))

        assertThat(trackTypeChildren).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            "$TYPE_TRACKS/$CATEGORY_ALL",
            "$TYPE_TRACKS/$CATEGORY_RECENTLY_ADDED",
            "$TYPE_TRACKS/$CATEGORY_MOST_RATED"
        )

        assertThatAllAreBrowsableAmong(trackTypeChildren)
        assertThatNoneArePlayableAmong(trackTypeChildren)
    }

    @Test
    fun whenLoadingChildrenOfAlbumType_thenReturnAlbumsWithMediaIdBasedOnAlbumId(): Unit = runBlocking {
        val albumTypeChildren = loadChildrenOf(MediaId(TYPE_ALBUMS))

        assertThat(albumTypeChildren).comparingElementsUsing(THEIR_MEDIA_ID)
            .containsExactly(
                "$TYPE_ALBUMS/40",
                "$TYPE_ALBUMS/38",
                "$TYPE_ALBUMS/102",
                "$TYPE_ALBUMS/95",
                "$TYPE_ALBUMS/7",
                "$TYPE_ALBUMS/6",
                "$TYPE_ALBUMS/65",
                "$TYPE_ALBUMS/26"
            ).inOrder()

        assertThatAllAreBrowsableAmong(albumTypeChildren)
    }

    @Test
    fun whenLoadingChildrenOfAlbumType_thenReturnItemsFromAlbumMetadata(): Unit = runBlocking {
        val allAlbums = loadChildrenOf(MediaId(TYPE_ALBUMS))
        val anAlbum = allAlbums.requireItemWith(MediaId(TYPE_ALBUMS, "40"))

        with(anAlbum.description) {
            assertThat(title).isEqualTo("The 2nd Law")
            assertThat(subtitle).isEqualTo("Muse")
        }

        assertThatBundle(anAlbum.description.extras).integer(MediaItems.EXTRA_NUMBER_OF_TRACKS).isEqualTo(1)
    }

    @Test
    fun whenLoadingChildrenOfArtistType_thenReturnAllArtistWithMediaIdBasedOnTheArtistId(): Unit = runBlocking {
        val allArtists = loadChildrenOf(MediaId(TYPE_ARTISTS))

        assertThat(allArtists).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            "$TYPE_ARTISTS/5",
            "$TYPE_ARTISTS/26",
            "$TYPE_ARTISTS/4",
            "$TYPE_ARTISTS/13",
            "$TYPE_ARTISTS/18"
        ).inOrder()
        assertThatAllAreBrowsableAmong(allArtists)
    }

    @Test
    fun whenLoadingChildrenOfArtistType_thenReturnItemsFromArtistMetadata(): Unit = runBlocking {
        val allArtists = loadChildrenOf(MediaId(TYPE_ARTISTS))
        val anArtist = allArtists.requireItemWith(MediaId(TYPE_ARTISTS, "5"))

        with(anArtist.description) {
            assertThat(title).isEqualTo("AC/DC")
            // TODO Use a plural string resource instead.
            assertThat(subtitle).isEqualTo("1 albums, 2 tracks")
            assertOn(extras) {
                integer(MediaItems.EXTRA_NUMBER_OF_TRACKS).isEqualTo(2)
            }
        }
    }

    @Test
    fun whenLoadingChildrenOfPlaylistType_thenReturnAllPlaylists(): Unit = runBlocking {
        val allPlaylists = loadChildrenOf(MediaId(TYPE_PLAYLISTS))

        assertThat(allPlaylists).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            "$TYPE_PLAYLISTS/1",
            "$TYPE_PLAYLISTS/2",
            "$TYPE_PLAYLISTS/3"
        ).inOrder()

        assertThatAllAreBrowsableAmong(allPlaylists)
    }

    @Test
    fun whenLoadingChildrenOfPlaylistType_thenReturnItemsFromPlaylistMetadata() = runBlocking {
        val allPlaylists = loadChildrenOf(MediaId(TYPE_PLAYLISTS))

        val aPlaylist = allPlaylists.requireItemWith(MediaId(TYPE_PLAYLISTS, "1"))
        assertThat(aPlaylist.description.title).isEqualTo("Zen")
    }

    @Test
    fun givenAnyBrowsableParent_whenLoadingItsChildren_thenNeverReturnNull(): Unit = runBlocking {
        val repository = TestMediaRepository()
        val browserTree = BrowserTreeImpl(context, repository, TestUsageManager())

        browserTree.walk(MediaId(TYPE_ROOT)) { child, parentId ->
            if(child.isBrowsable) {
                val childId = child.mediaId.toMediaId()
                val children = browserTree.getChildren(childId, null)

                assertThat(children)
                    .named("Children of item %s (parent = %s)", childId, parentId)
                    .isNotNull()
            }
        }
    }

    @Test
    fun givenAnyNonBrowsableItem_whenLoadingItsChildren_thenReturnNull(): Unit = runBlocking {
        val repository = TestMediaRepository()
        val browserTree = BrowserTreeImpl(context, repository, TestUsageManager())

        browserTree.walk(MediaId(TYPE_ROOT)) { child, parentId ->
            if (!child.isBrowsable) {
                val childId = child.mediaId.toMediaId()
                val children = browserTree.getChildren(childId, null)

                assertThat(children)
                    .named("Children of %s (parent = %s)", childId, parentId)
                    .isNull()
            }
        }
    }

    @Test
    fun whenLoadingChildrenOfAllTracks_thenReturnTheListOfAllTracks(): Unit = runBlocking {
        val allTracks = loadChildrenOf(MediaId(TYPE_TRACKS, CATEGORY_ALL))

        assertThat(allTracks).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
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
        ).inOrder()

        assertThatAllArePlayableAmong(allTracks)
        assertThatNoneAreBrowsableAmong(allTracks)
    }

    @Test
    fun whenLoadingChildrenOfAllTracks_thenReturnItemsFromTrackMetadata(): Unit = runBlocking {
        val allTracks = loadChildrenOf(MediaId(TYPE_TRACKS, CATEGORY_ALL))
        val aTrack = allTracks.requireItemWith(MediaId(TYPE_TRACKS, CATEGORY_ALL, 125L))

        with(aTrack.description) {
            assertThat(title).isEqualTo("Jailbreak")
            assertThat(subtitle).isEqualTo("AC/DC")

            assertOn(extras) {
                containsKey(MediaItems.EXTRA_DURATION)
                integer(MediaItems.EXTRA_DISC_NUMBER).isEqualTo(2)
                integer(MediaItems.EXTRA_TRACK_NUMBER).isEqualTo(14)
            }
        }
    }

    @Test
    fun whenLoadingChildrenOfMostRated_thenReturnMostRatedTracksFromUsageManager(): Unit = runBlockingTest {
        val mostRatedTracks = loadChildrenOf(MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED))

        assertThat(mostRatedTracks).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            "$TYPE_TRACKS/$CATEGORY_MOST_RATED|75",
            "$TYPE_TRACKS/$CATEGORY_MOST_RATED|464",
            "$TYPE_TRACKS/$CATEGORY_MOST_RATED|48",
            "$TYPE_TRACKS/$CATEGORY_MOST_RATED|477",
            "$TYPE_TRACKS/$CATEGORY_MOST_RATED|294"
        ).inOrder()

        assertThatAllArePlayableAmong(mostRatedTracks)
        assertThatNoneAreBrowsableAmong(mostRatedTracks)
    }

    @Test
    fun whenLoadingChildrenOfMostRated_thenReturnItemsFromTrackMetadata(): Unit = runBlocking {
        val mostRecentTracks = loadChildrenOf(MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED))
        val aTrack = mostRecentTracks.requireItemWith(MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED, 75L))

        with(aTrack.description) {
            assertThat(title).isEqualTo("Nightmare")
            assertThat(subtitle).isEqualTo("Avenged Sevenfold")

            assertOn(extras) {
                containsKey(MediaItems.EXTRA_DURATION)
                integer(MediaItems.EXTRA_DISC_NUMBER).isEqualTo(1)
                integer(MediaItems.EXTRA_TRACK_NUMBER).isEqualTo(1)
            }
        }
    }

    @Test
    fun whenLoadingChildrenOfRecentlyAdded_thenItemsAreTracksSortedByDescendingAvailabilityDate(): Unit = runBlocking {
        val mostRecentTracks = loadChildrenOf(MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED))

        assertThat(mostRecentTracks).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
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
        ).inOrder()
    }

    @Test
    fun whenLoadingChildrenOfRecentlyAdded_thenItemsArePlayableOnly(): Unit = runBlocking {
        val mostRecentTracks = loadChildrenOf(MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED))

        assertThatAllArePlayableAmong(mostRecentTracks)
        assertThatNoneAreBrowsableAmong(mostRecentTracks)
    }

    @Test
    fun whenLoadingChildrenOfRecentlyAdded_thenReturnNoMoreThan25Tracks(): Unit = runBlocking {
        val testTracks = generateRandomTrackSequence().take(50).toList()
        val repository = TestMediaRepository(tracks = testTracks)
        val browserTree = BrowserTreeImpl(context, repository, StubUsageManager)

        val mostRecentTracks = browserTree.getChildren(
            MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED),
            options = null
        ) ?: failAssumption("Expected $TYPE_TRACKS/$CATEGORY_RECENTLY_ADDED to have children")

        assertThat(mostRecentTracks.size).named("Number of most recent tracks").isAtMost(25)
    }

    @Test
    fun whenLoadingChildrenOfRecentlyAdded_thenReturnItemsFromTrackMetadata(): Unit = runBlocking {
        val mostRecentTracks = loadChildrenOf(MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED))
        val aTrack = mostRecentTracks.requireItemWith(MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED, 481L))

        with(aTrack.description) {
            assertThat(title).isEqualTo("Dirty Water")
            assertThat(subtitle).isEqualTo("Foo Fighters")

            assertOn(extras) {
                containsKey(MediaItems.EXTRA_DURATION)
                integer(MediaItems.EXTRA_DISC_NUMBER).isEqualTo(1)
                integer(MediaItems.EXTRA_TRACK_NUMBER).isEqualTo(6)
            }
        }
    }

    @Test
    fun whenLoadingChildrenOfDisposableTracks_thenReturnItemsFromUsageManager() = runBlockingTest {
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
    fun whenLoadingChildrenOfAnAlbum_thenReturnTracksFromThatAlbum(): Unit = runBlocking {
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

        assertThat(children)
            .comparingElementsUsing(THEIR_MEDIA_ID)
            .containsExactlyElementsIn(expectedMediaIds)
            .inOrder()
    }

    @Test
    fun whenLoadingChildrenOfAnArtist_thenReturnItsAlbumsFollowedByItsTracks(): Unit = runBlocking {
        val artistChildren = loadChildrenOf(MediaId(TYPE_ARTISTS, "18"))
        val indexOfFirstTrack = artistChildren.indexOfFirst { it.mediaId.toMediaId().track != null }
        val childrenAfterAlbums = artistChildren.subList(indexOfFirstTrack, artistChildren.size)

        val nonTracksAfterAlbums = childrenAfterAlbums.filter { it.mediaId.toMediaId().track == null }
        assertThat(nonTracksAfterAlbums).isEmpty()
    }

    @Test
    fun whenLoadingChildrenOfAnArtist_thenReturnAlbumsFromThatArtistSortedByDescReleaseDate(): Unit = runBlocking {
        assertArtistHasAlbumsChildren(26L, listOf("$TYPE_ALBUMS/65"))
        assertArtistHasAlbumsChildren(18L, listOf("$TYPE_ALBUMS/40", "$TYPE_ALBUMS/38"))
        assertArtistHasAlbumsChildren(13L, listOf("$TYPE_ALBUMS/102", "$TYPE_ALBUMS/26", "$TYPE_ALBUMS/95"))
    }

    private suspend fun assertArtistHasAlbumsChildren(artistId: Long, expectedAlbumIds: List<String>) {
        val artistChildren = loadChildrenOf(MediaId(TYPE_ARTISTS, artistId.toString()))
        val artistAlbums = artistChildren.filter { it.mediaId.toMediaId().track == null }

        assertThatAllAreBrowsableAmong(artistAlbums)
        assertThat(artistAlbums)
            .comparingElementsUsing(THEIR_MEDIA_ID)
            .containsExactlyElementsIn(expectedAlbumIds)
            .inOrder()
    }

    @Test
    fun whenLoadingChildrenOfAnArtist_thenReturnTracksFromThatArtistSortedAlphabetically(): Unit = runBlocking {
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
    fun whenLoadingChildrenOfAnArtist_thenReturnArtistAlbumsFromMetadata(): Unit = runBlocking {
        val artistChildren = loadChildrenOf(MediaId(TYPE_ARTISTS, "26"))
        val anAlbum = artistChildren.requireItemWith(MediaId(TYPE_ALBUMS, "65"))

        with(anAlbum.description) {
            assertThat(title).isEqualTo("Sunset on the Golden Age")
            assertOn(extras) {
                integer(MediaItems.EXTRA_NUMBER_OF_TRACKS).isEqualTo(1)
            }
        }
    }

    @Test
    fun whenLoadingChildrenOfAnArtist_thenReturnArtistTracksFromMetadata(): Unit = runBlocking {
        val artistChildren = loadChildrenOf(MediaId(TYPE_ARTISTS, "26"))
        val aTrack = artistChildren.requireItemWith(MediaId(TYPE_ARTISTS, "26", 161L))

        with(aTrack.description) {
            assertThat(title).isEqualTo("1741 (The Battle of Cartagena)")

            assertOn(extras) {
                containsKey(MediaItems.EXTRA_DURATION)
            }
        }
    }

    @Test
    fun whenLoadingChildrenOfOnePlaylist_thenReturnTracksFromThatPlaylist(): Unit = runBlocking {
        assertPlaylistHasTracks(1L, listOf("$TYPE_PLAYLISTS/1|309"))
        assertPlaylistHasTracks(2L, listOf(
            "$TYPE_PLAYLISTS/2|477",
            "$TYPE_PLAYLISTS/2|48",
            "$TYPE_PLAYLISTS/2|125"
        ))
    }

    @Test
    fun whenLoadingChildrenOfOnePlaylist_thenReturnItemsFromTrackMetadata(): Unit = runBlocking {
        val playlistChildren = loadChildrenOf(MediaId(TYPE_PLAYLISTS, "1"))
        val aPlaylistTrack = playlistChildren.requireItemWith(MediaId(TYPE_PLAYLISTS, "1", 309L))

        with(aPlaylistTrack.description) {
            assertThat(title).isEqualTo("The 2nd Law: Isolated System")
            assertOn(extras) {
                containsKey(MediaItems.EXTRA_DURATION)
            }
        }
    }

    @Test
    fun givenNonExistingCategory_whenLoadingItsChildren_thenReturnNull(): Unit = runBlocking {
        assertHasNoChildren(MediaId("unknown"))
        assertHasNoChildren(MediaId(TYPE_TRACKS, "undefined"))
        assertHasNoChildren(MediaId(TYPE_ALBUMS, "1234"))
        assertHasNoChildren(MediaId(TYPE_ARTISTS, "1234"))
        assertHasNoChildren(MediaId(TYPE_PLAYLISTS, "1234"))
    }

    @Test
    fun whenRequestingAnyItem_thenReturnAnItemWithSameMediaIdAsRequested(): Unit = runBlocking {
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
        assume().that(requestedItem).isNotNull()
        assertThat(requestedItem!!.mediaId).isEqualTo(itemId.encoded)
    }

    @Test
    fun whenRequestingAnyItem_thenReturnAnItemThatIsInTheResultOfGetChildren(): Unit = runBlocking {
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
    fun givenPagesOfSizeN_whenLoadingChildren_thenReturnNFirstItems(): Unit = runBlocking {
        val repository = TestMediaRepository()
        val browserTree = BrowserTreeImpl(context, repository, StubUsageManager)

        val paginatedChildren = browserTree.getChildren(
            MediaId(TYPE_TRACKS, CATEGORY_ALL),
            givenPaginationExtras(0, 3)
        )

        assertThat(paginatedChildren).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            encode(TYPE_TRACKS, CATEGORY_ALL, 161),
            encode(TYPE_TRACKS, CATEGORY_ALL, 309),
            encode(TYPE_TRACKS, CATEGORY_ALL, 481)
        ).inOrder()
    }

    @Test
    fun givenPagesOfSizeNAndPageX_whenLoadingChildren_thenReturnNItemsFromPositionXN(): Unit = runBlocking {
        val repository = TestMediaRepository()
        val browserTree = BrowserTreeImpl(context, repository, StubUsageManager)

        val paginatedChildren = browserTree.getChildren(
            MediaId(TYPE_TRACKS, CATEGORY_ALL),
            givenPaginationExtras(3, 2)
        )

        assertThat(paginatedChildren).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            encode(TYPE_TRACKS, CATEGORY_ALL, 219),
            encode(TYPE_TRACKS, CATEGORY_ALL, 75)
        ).inOrder()
    }

    @Test
    fun givenPageAfterItems_whenLoadingChildren_thenReturnNoChildren(): Unit = runBlocking {
        val repository = TestMediaRepository()
        val browserTree = BrowserTreeImpl(context, repository, StubUsageManager)

        val pagePastChildren = browserTree.getChildren(
            MediaId(TYPE_TRACKS, CATEGORY_ALL),
            givenPaginationExtras(2, 5)
        )

        assertThat(pagePastChildren).isEmpty()
    }

    private fun givenPaginationExtras(pageNumber: Int, pageSize: Int): Bundle = Bundle(2).apply {
        putInt(MediaBrowserCompat.EXTRA_PAGE, pageNumber)
        putInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, pageSize)
    }

    @Test
    fun whenReceivingChangeNotification_thenMapToTheCorrespondingMediaId() {
        assertNotifyParentChanged(ChangeNotification.AllTracks, MediaId(TYPE_TRACKS, CATEGORY_ALL))
        assertNotifyParentChanged(ChangeNotification.AllTracks, MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED))
        assertNotifyParentChanged(ChangeNotification.AllTracks, MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED))
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

        assertThat(parentChildren)
            .comparingElementsUsing(THEIR_MEDIA_ID)
            .contains(item.mediaId)
    }

    private suspend fun assertHasNoChildren(parentId: MediaId) {
        val browserTree = BrowserTreeImpl(context, TestMediaRepository(), StubUsageManager)
        val children = browserTree.getChildren(parentId, null)
        assertThat(children).isNull()
    }

    private suspend fun assertPlaylistHasTracks(playlistId: Long, expectedtrackIds: List<String>) {
        val playlistChildren = loadChildrenOf(MediaId(TYPE_PLAYLISTS, playlistId.toString()))

        assertThatAllArePlayableAmong(playlistChildren)
        assertThatNoneAreBrowsableAmong(playlistChildren)

        assertThat(playlistChildren)
            .comparingElementsUsing(THEIR_MEDIA_ID)
            .containsExactlyElementsIn(expectedtrackIds)
            .inOrder()
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

        assertThat(artistTracks)
            .comparingElementsUsing(THEIR_MEDIA_ID)
            .containsExactlyElementsIn(expectedTrackIds)
            .inOrder()
    }

    private suspend fun loadChildrenOf(parentId: MediaId): List<MediaItem> {
        val repository = TestMediaRepository()
        val usageManager = TestUsageManager()
        val browserTree = BrowserTreeImpl(context, repository, usageManager)

        val children = browserTree.getChildren(parentId, null)
        return children ?: fail("Expected $parentId to have children.")
    }

    private inline fun assertOn(extras: Bundle?, assertions: BundleSubject.() -> Unit) {
        assertThat(extras).named("extras").isNotNull()
        assertThatBundle(extras).run(assertions)
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