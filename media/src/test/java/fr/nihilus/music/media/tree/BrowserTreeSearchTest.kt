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
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import fr.nihilus.music.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.media.MediaId.Builder.TYPE_ALBUMS
import fr.nihilus.music.media.MediaId.Builder.TYPE_ARTISTS
import fr.nihilus.music.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.media.MediaId.Builder.encode
import fr.nihilus.music.media.THEIR_MEDIA_ID
import fr.nihilus.music.media.provider.Track
import fr.nihilus.music.media.repo.TestMediaRepository
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowserTreeSearchTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun whenSearchingWithEmptyQuery_thenReturnNoResults() = runBlockingTest {
        val tree = givenRealisticBrowserTree()

        val results = tree.search("", null)
        assertThat(results).isEmpty()
    }

    @Test
    fun givenExactArtistName_whenSearchingAnArtist_thenReturnThatArtist() = runBlockingTest {
        val options = givenArtistSearchOptions("foo fighters")
        val browserTree = givenRealisticBrowserTree()

        val results = browserTree.search("foo fighters", options)
        assertThat(results).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            encode(TYPE_ARTISTS, "13")
        )
    }

    @Test
    fun givenExactAlbumTitle_whenSearchingAnAlbum_thenReturnThatAlbum() = runBlockingTest {
        val options = givenAlbumSearchOptions("wasting light")
        val tree = givenRealisticBrowserTree()

        val results = tree.search("wasting light", options)
        assertThat(results).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            encode(TYPE_ALBUMS, "26")
        )
    }

    @Test
    fun givenExactSongTitle_whenSearchingSongs_thenReturnThatSong() = runBlockingTest {
        val options = givenSongSearchOptions("dirty water")
        val tree = givenRealisticBrowserTree()

        val results = tree.search("dirty water", options)
        assertThat(results).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            encode(TYPE_TRACKS, CATEGORY_ALL, 481)
        )
    }

    @Test
    fun givenExactArtistName_whenSearchingUnfocused_thenReturnThatArtist() = runBlockingTest {
        val tree = givenRealisticBrowserTree()

        val results = tree.search("foo fighters", null)
        assertThat(results).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            encode(TYPE_ARTISTS, "13")
        )
    }

    @Test
    fun givenExactAlbumName_whenSearchingUnfocused_thenReturnThatAlbum() = runBlockingTest {
        val tree = givenRealisticBrowserTree()

        val results = tree.search("concrete and gold", null)
        assertThat(results).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            encode(TYPE_ALBUMS, "102")
        )
    }

    @Test
    fun givenExactSongTitle_whenSearchingUnfocused_thenReturnThatSong() = runBlockingTest {
        val tree = givenRealisticBrowserTree()

        val results = tree.search("dirty water", null)
        assertThat(results).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            encode(TYPE_TRACKS, CATEGORY_ALL, 481)
        )
    }

    @Test
    fun givenQueryMatchingBothAlbumAndSong_whenSearchingAlbums_thenReturnOnlyTheAlbum() = runBlockingTest {
        val options = givenAlbumSearchOptions("nightmare")
        val tree = givenRealisticBrowserTree()

        val results = tree.search("nightmare", options)
        assertThat(results).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            encode(TYPE_ALBUMS, "6")
        )
    }

    @Test
    fun givenQueryMatchingBothAlbumAndSong_whenSearchingUnfocused_thenReturnBoth() = runBlockingTest {
        val tree = givenRealisticBrowserTree()

        // Both the album "Nightmare" and its eponymous track are listed in search results.
        // Note that the album should be listed first.
        val results = tree.search("nightmare", null)

        assertThat(results).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            encode(TYPE_ALBUMS, "6"),
            encode(TYPE_TRACKS, CATEGORY_ALL, 75)
        ).inOrder()
    }

    @Test
    fun givenPattern_whenSearching_thenReturnItemsContainingThatPattern() = runBlockingTest {
        val tracks = listOf(
            Track(23, "Another Brick In The Wall", "Pink Floyd", "The Wall", 0, 1, 5, "", null, 0, 2, 2),
            Track(34, "Another One Bites the Dust", "Queen", "The Game", 0, 1, 3, "", null, 0, 3, 3),
            Track(56, "Nothing Else Matters", "Metallica", "Metallica", 0L, 1, 8, "", null, 0, 4, 4),
            Track(12, "Otherside", "Red Hot Chili Peppers", "Californication", 0, 1, 6, "", null, 0, 1, 1),
            Track(98, "You've Got Another Thing Comin", "Judas Priest", "Screaming for Vengeance", 0, 1, 8, "", null, 0, 7, 7)
        )

        val tree = BrowserTreeImpl(context, TestMediaRepository(tracks, emptyList(), emptyList()))

        // "OTHERside" is listed first (it starts with the pattern),
        // then "AnOTHER Brick In the Wall" (same pattern at same position),
        // then "AnOTHER One Bites the Dust" (one word contains the pattern but slightly longer),
        // then "You've Got AnOTHER Thing Comin" (pattern matches farther)
        val results = tree.search("other", null)

        assertThat(results).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            encode(TYPE_TRACKS, CATEGORY_ALL, 12),
            encode(TYPE_TRACKS, CATEGORY_ALL, 23),
            encode(TYPE_TRACKS, CATEGORY_ALL, 34),
            encode(TYPE_TRACKS, CATEGORY_ALL, 98)
        ).inOrder()
    }

    @Test
    fun givenPatternThatMatchesMultipleItemsEqually_whenSearching_thenReturnShorterFirst() = runBlockingTest {
        val tracks = listOf(
            Track(10, "Are You Ready", "AC/DC", "The Razor's Edge", 0, 1, 7, "", null, 0, 32, 18),
            Track(42, "Are You Gonna Be My Girl", "Jet", "Get Born", 0, 1, 2, "", null, 0, 78, 90),
            Track(63, "Are You Gonna Go My Way", "Lenny Kravitz", "Are You Gonna Go My Way", 0, 1, 1, "", null, 0, 57, 23)
        )

        val tree = BrowserTreeImpl(context, TestMediaRepository(tracks, emptyList(), emptyList()))

        // When the pattern matches multiple items equally,
        // shorter items should be displayed first.
        val results = tree.search("are", null)

        assertThat(results).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            encode(TYPE_TRACKS, CATEGORY_ALL, 10),
            encode(TYPE_TRACKS, CATEGORY_ALL, 63),
            encode(TYPE_TRACKS, CATEGORY_ALL, 42)
        ).inOrder()
    }

    /*@Test
    fun givenSearchWhoseFirstResultIsBrowsable_whenSearchingPlayableOnly_thenReturnItsChildren() = runBlockingTest {
        val albums = listOf(
            Album(7, "The Final Countdown", "Europe", 1, 0, null, 4),
            Album(2, "Nightmare", "Avenged Sevenfold", 2, 0, null, 1)
        )

        val tracks = listOf(
            Track(36, "Buried Alive", "Avenged Sevenfold", "Nightmare", 0, 1, 4, "", null, 0, 1, 2),
            Track(87, "Rock the Night", "Europe", "The Final Countdown", 0, 1, 2, "", null, 0, 4, 7),
            Track(35, "So Far Away", "Avenged Sevenfold", "Nightmare", 0, 1, 1, "", null, 0, 1, 2)
        )

        val tree = BrowserTreeImpl(context, TestMediaRepository(tracks, albums, emptyList()))

        // When performing in normal mode, the first result would have been the "Nightmare" album.
        // Since it performs in playable-only, assume that the user wanted to play the whole album,
        // so the search results contains only tracks from that album.
        val playableOnlyResults = tree.search("night", givenPlayableOnlyOption())

        assertThat(playableOnlyResults).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            encode(TYPE_ALBUMS, "2", 36),
            encode(TYPE_ALBUMS, "2", 35)
        )
    }

    @Test
    fun givenSearchWhoseResultsAreAllPlayable_whenSearchingPlayableOnly_thenReturnThemAll() = runBlockingTest {
        val tracks = listOf(
            Track(23, "Another Brick In The Wall", "Pink Floyd", "The Wall", 0, 1, 5, "", null, 0, 2, 2),
            Track(56, "Nothing Else Matters", "Metallica", "Metallica", 0L, 1, 8, "", null, 0, 4, 4),
            Track(12, "Otherside", "Red Hot Chili Peppers", "Californication", 0, 1, 6, "", null, 0, 1, 1),
            Track(98, "You've Got Another Thing Comin", "Judas Priest", "Screaming for Vengeance", 0, 1, 8, "", null, 0, 7, 7)
        )

        val tree = BrowserTreeImpl(context, TestMediaRepository(tracks, emptyList(), emptyList()))

        // When a normal search would have returned no browsable item,
        // then return the same results in playable-only mode.
        val playableOnlyResults = tree.search("other", givenPlayableOnlyOption())

        assertThat(playableOnlyResults).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            encode(TYPE_TRACKS, CATEGORY_ALL, 12),
            encode(TYPE_TRACKS, CATEGORY_ALL, 23),
            encode(TYPE_TRACKS, CATEGORY_ALL, 98)
        ).inOrder()
    }

    @Test
    fun givenSearchWhoseFirstResultIsNotBrowsable_whenSearchingPlayableOnly_thenOmitBrowsableItems() = runBlockingTest {
        val albums = listOf(
            Album(37, "Back In Black", "AC/DC", 1, 0, null, 54),
            Album(99, "Going to Hell", "The Pretty Reckless", 1, 0, null, 21),
            Album(3, "Highway to Hell", "AC/DC", 1, 0, null, 21)
        )

        val tracks = listOf(
            Track(33, "Going to Hell", "The Pretty Reckless", "Going to Hell", 0, 1, 2, "", null, 0, 21, 99),
            Track(22, "Hell's Bells", "AC/DC", "Back in Black", 0, 1, 1, "", null, 0, 54, 37),
            Track(77, "Highway to Hell", "AC/DC", "Highway to Hell", 0, 1, 1, "", null, 0, 21, 3)
        )

        val tree = BrowserTreeImpl(context, TestMediaRepository(tracks, albums, emptyList()))

        // "hell" matches "Highway to Hell" (both an album and a song),
        // but since "Hell's Bells" is a better match, assume that users wanted that song
        // and return only songs having "hell" in their title (including "Highway to Hell"),
        // ignoring albums and artists.
        val playableOnlyResults = tree.search("hell", givenPlayableOnlyOption())

        assertThat(playableOnlyResults).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            encode(TYPE_TRACKS, CATEGORY_ALL, 22), // HELL's bells
            encode(TYPE_TRACKS, CATEGORY_ALL, 33), // going to HELL
            encode(TYPE_TRACKS, CATEGORY_ALL, 77) //
        ).inOrder()
    }*/

    private fun givenRealisticBrowserTree(): BrowserTreeImpl {
        val repository = TestMediaRepository()
        return BrowserTreeImpl(context, repository)
    }

    private fun givenArtistSearchOptions(artistName: String) = Bundle(2).apply {
        putString(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE)
        putString(MediaStore.EXTRA_MEDIA_ARTIST, artistName)
    }

    private fun givenAlbumSearchOptions(albumTitle: String) = Bundle(2).apply {
        putString(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE)
        putString(MediaStore.EXTRA_MEDIA_ALBUM, albumTitle)
    }

    private fun givenSongSearchOptions(title: String) = Bundle(2).apply {
        putString(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Media.ENTRY_CONTENT_TYPE)
        putString(MediaStore.EXTRA_MEDIA_TITLE, title)
    }

    private fun givenPlayableOnlyOption() = Bundle(1).apply {
        putBoolean(BrowserTree.EXTRA_PLAYABLE_ONLY, true)
    }
}