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
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_SMART
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.core.test.fail
import fr.nihilus.music.core.test.failAssumption
import fr.nihilus.music.service.AudioTrack
import fr.nihilus.music.service.MediaCategory
import fr.nihilus.music.service.MediaContent
import fr.nihilus.music.service.generateRandomTrackSequence
import io.kotlintest.inspectors.forAll
import io.kotlintest.inspectors.forNone
import io.kotlintest.inspectors.forOne
import io.kotlintest.matchers.beOfType
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotlintest.matchers.collections.shouldHaveAtMostSize
import io.kotlintest.matchers.types.shouldBeTypeOf
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotThrowAny
import io.kotlintest.shouldThrow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Validate the structure of the [BrowserTree]:
 * - tree can be browsed from the root to the topmost leafs,
 * - children of those nodes are correctly fetched and mapped to [MediaContent]s.
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
            TYPE_PLAYLISTS,
            TYPE_SMART
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

        anAlbum.shouldBeTypeOf<MediaCategory> {
            it.title shouldBe "The 2nd Law"
            it.subtitle shouldBe "Muse"
            it.trackCount shouldBe 1
        }
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

        anArtist.shouldBeTypeOf<MediaCategory> {
            it.title shouldBe "AC/DC"
            // TODO Use a plural string resource instead.
            it.subtitle shouldBe "1 albums, 2 tracks"
            it.trackCount shouldBe 2
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
        aPlaylist.shouldBeTypeOf<MediaCategory> {
            it.title shouldBe "Zen"
        }
    }

    @Test
    fun `Given any browsable parent, when loading its children then never throw`() = runBlockingTest {
        val browserTree = BrowserTreeImpl(context, TestMediaDao(), TestPlaylistDao(), TestUsageManager(), TestSpotifyManager())

        browserTree.walk(MediaId(TYPE_ROOT)) { child, _ ->
            if (child is MediaCategory) {
                println(child.id)
                shouldNotThrowAny {
                    browserTree.getChildren(child.id).first()
                }
            }
        }
    }

    @Test
    fun `Given any non browsable item, when loading its children then throw NoSuchElementException`() = runBlockingTest {
        val browserTree = BrowserTreeImpl(context, TestMediaDao(), TestPlaylistDao(), TestUsageManager(), TestSpotifyManager())

        browserTree.walk(MediaId(TYPE_ROOT)) { child, _ ->
            if (child is AudioTrack) {
                shouldThrow<NoSuchElementException> {
                    browserTree.getChildren(child.id).first()
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

        aTrack.shouldBeTypeOf<AudioTrack> {
            it.title shouldBe "Jailbreak"
            it.subtitle shouldBe "AC/DC"
            it.album shouldBe "Greatest Hits 30 Anniversary Edition"
            it.artist shouldBe "AC/DC"
            it.duration shouldBe 276668L
            it.discNumber shouldBe 2
            it.trackNumber shouldBe 14
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

        aTrack.shouldBeTypeOf<AudioTrack> {
            it.title shouldBe "Nightmare"
            it.subtitle shouldBe "Avenged Sevenfold"
            it.album shouldBe "Nightmare"
            it.artist shouldBe "Avenged Sevenfold"
            it.duration shouldBe 374648L
            it.discNumber shouldBe 1
            it.trackNumber shouldBe 1
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
        val media = TestMediaDao(tracks = testTracks)
        val browserTree = BrowserTreeImpl(context, media, TestPlaylistDao(), StubUsageManager, StubSpotifyManager)

        val mostRecentTracks = browserTree.getChildren(
            MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED)
        ).first()

        mostRecentTracks shouldHaveAtMostSize 25
    }

    @Test
    fun `When loading children of Recently Added, then return items from track metadata`() = runBlockingTest {
        val mostRecentTracks = loadChildrenOf(MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED))
        val aTrack = mostRecentTracks.requireItemWith(MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED, 481L))

        aTrack.shouldBeTypeOf<AudioTrack> {
            it.title shouldBe "Dirty Water"
            it.subtitle shouldBe "Foo Fighters"
            it.album shouldBe "Concrete and Gold"
            it.artist shouldBe "Foo Fighters"
            it.duration shouldBe 320914L
            it.discNumber shouldBe 1
            it.trackNumber shouldBe 6
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
        val indexOfFirstTrack = artistChildren.indexOfFirst { it.id.track != null }
        val childrenAfterAlbums = artistChildren.subList(indexOfFirstTrack, artistChildren.size)

        val nonTracksAfterAlbums = childrenAfterAlbums.filter { it.id.track == null }
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
        val artistAlbums = artistChildren.filter { it.id.track == null }

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

        anAlbum.shouldBeTypeOf<MediaCategory> {
            it.title shouldBe "Sunset on the Golden Age"
            it.trackCount shouldBe 1
        }
    }

    @Test
    fun `When loading children of an artist, then return artist tracks from metadata`() = runBlockingTest {
        val artistChildren = loadChildrenOf(MediaId(TYPE_ARTISTS, "26"))
        val aTrack = artistChildren.requireItemWith(MediaId(TYPE_ARTISTS, "26", 161L))

        aTrack.shouldBeTypeOf<AudioTrack> {
            it.title shouldBe "1741 (The Battle of Cartagena)"
            it.duration shouldBe 437603L
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

        aPlaylistTrack.shouldBeTypeOf<AudioTrack> {
            it.title shouldBe "The 2nd Law: Isolated System"
            it.duration shouldBe 300042L
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
        val browserTree = BrowserTreeImpl(context, TestMediaDao(), TestPlaylistDao(), StubUsageManager, StubSpotifyManager)

        val requestedItem = browserTree.getItem(itemId)
            ?: failAssumption("Expected an item with id $itemId")
        requestedItem.id shouldBe itemId
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

    private suspend fun assertItemIsPartOfItsParentsChildren(parentId: MediaId, itemId: MediaId) {
        val browserTree = BrowserTreeImpl(context, TestMediaDao(), TestPlaylistDao(), StubUsageManager, StubSpotifyManager)

        val item = browserTree.getItem(itemId)
            ?: failAssumption("Expected $itemId to be an existing item")
        val parentChildren = browserTree.getChildren(parentId).first()

        parentChildren.forOne {
            it.id shouldBe item.id
            it.title shouldBe item.title
            it.subtitle shouldBe item.subtitle
            it.iconUri shouldBe item.iconUri

            if (item is AudioTrack) {
                it as? AudioTrack ?: fail("Expected $it to be an AudioTrack")
                it.album shouldBe item.album
                it.artist shouldBe item.artist
                it.duration shouldBe item.duration
                it.discNumber shouldBe item.discNumber
                it.trackNumber shouldBe item.trackNumber
                it.mediaUri shouldBe item.mediaUri
            }

            if (item is MediaCategory) {
                it as? MediaCategory ?: fail("Expected $it to be a MediaCategory")
                it.trackCount shouldBe item.trackCount
                it.isPlayable shouldBe item.isPlayable
            }
        }
    }

    private suspend fun assertHasNoChildren(parentId: MediaId) {
        val browserTree = BrowserTreeImpl(context, TestMediaDao(), TestPlaylistDao(), StubUsageManager, StubSpotifyManager)

        shouldThrow<NoSuchElementException> {
            browserTree.getChildren(parentId).first()
        }
    }

    private suspend fun assertPlaylistHasTracks(playlistId: Long, expectedTrackIds: List<String>) {
        val playlistChildren = loadChildrenOf(MediaId(TYPE_PLAYLISTS, playlistId.toString()))

        assertThatAllArePlayableAmong(playlistChildren)
        assertThatNoneAreBrowsableAmong(playlistChildren)

        playlistChildren.ids.shouldContainExactly(expectedTrackIds)
    }

    /**
     * Assume that the given collection of media contains a media with the specified [media id][itemId],
     * and if it does, return it ; otherwise the test execution is stopped due to assumption failure.
     */
    private fun List<MediaContent>.requireItemWith(itemId: MediaId): MediaContent {
        return find { it.id == itemId } ?: failAssumption(buildString {
            append("Missing an item with id = $itemId in ")
            this@requireItemWith.joinTo(this, ", ", "[", "]", 10) {
                it.id.encoded
            }
        })
    }

    private suspend fun assertArtistHasTracksChildren(artistId: Long, expectedTrackIds: List<String>) {
        val artistChildren = loadChildrenOf(MediaId(TYPE_ARTISTS, artistId.toString()))
        val artistTracks = artistChildren.filter { it.id.track != null }

        assertThatAllArePlayableAmong(artistTracks)
        assertThatNoneAreBrowsableAmong(artistTracks)

        artistTracks.ids.shouldContainExactly(expectedTrackIds)
    }

    private suspend fun loadChildrenOf(parentId: MediaId): List<MediaContent> {
        val browserTree = BrowserTreeImpl(context, TestMediaDao(), TestPlaylistDao(), TestUsageManager(), StubSpotifyManager)
        return browserTree.getChildren(parentId).first()
    }

    private fun assertThatAllAreBrowsableAmong(children: List<MediaContent>) {
        children.forAll { it should beOfType<MediaCategory>() }
    }

    private fun assertThatAllArePlayableAmong(children: List<MediaContent>) {
        children.forAll { it should beOfType<AudioTrack>() }
    }

    private fun assertThatNoneArePlayableAmong(children: List<MediaContent>) {
        children.forNone { it should beOfType<AudioTrack>() }
    }

    private fun assertThatNoneAreBrowsableAmong(children: List<MediaContent>) {
        children.forNone { it should beOfType<MediaCategory>() }
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

private val List<MediaContent>.ids: List<String>
    get() = map { it.id.encoded }