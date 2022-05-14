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
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ALBUMS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ARTISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.media.artists.Artist
import fr.nihilus.music.media.provider.MediaDao
import fr.nihilus.music.media.tracks.Track
import fr.nihilus.music.media.usage.UsageManager
import fr.nihilus.music.service.MediaContent
import io.kotest.assertions.extracting
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
internal class BrowserTreeSearchTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `When searching with an empty query then return no results`() = runTest {
        val tree = givenRealisticBrowserTree()

        val results = tree.search(SearchQuery.Empty)
        results.shouldBeEmpty()
    }

    @Test
    fun `Given artist focus, when searching an artist then return that artist`() = runTest {
        val browserTree = givenRealisticBrowserTree()

        val results = browserTree.search(SearchQuery.Artist("Foo Fighters"))
        extracting(results, MediaContent::id).shouldContainExactly(
            MediaId(TYPE_ARTISTS, "13")
        )
    }

    @Test
    fun `Given album focus, when searching an album then return that album`() = runTest {
        val tree = givenRealisticBrowserTree()

        val results = tree.search(SearchQuery.Album("Foo Fighters", "Wasting Light"))
        extracting(results, MediaContent::id).shouldContainExactly(
            MediaId(TYPE_ALBUMS, "26")
        )
    }

    @Test
    fun `Given track focused query, when searching songs then return that song`() = runTest {
        val tree = givenRealisticBrowserTree()

        val results = tree.search(
            SearchQuery.Song(
                artist = "Foo Fighters",
                album = "Concrete and Gold",
                title = "Dirty Water"
            )
        )

        extracting(results, MediaContent::id).shouldContainExactly(
            MediaId(TYPE_TRACKS, CATEGORY_ALL, 481)
        )
    }

    @Test
    fun `Given exact artist name, when searching unfocused then return that artist`() = runTest {
        val tree = givenRealisticBrowserTree()

        val results = tree.search(SearchQuery.Unspecified("foo fighters"))
        extracting(results, MediaContent::id).shouldContainExactly(
            MediaId(TYPE_ARTISTS, "13")
        )
    }

    @Test
    fun `Given exact album title, when searching unfocused then return that album`() = runTest {
        val tree = givenRealisticBrowserTree()

        val results = tree.search(SearchQuery.Unspecified("concrete and gold"))
        extracting(results, MediaContent::id).shouldContainExactly(
            MediaId(TYPE_ALBUMS, "102")
        )
    }

    @Test
    fun `Given exact song title, when searching unfocused then return that song`() = runTest {
        val tree = givenRealisticBrowserTree()

        val results = tree.search(SearchQuery.Unspecified("dirty water"))
        extracting(results, MediaContent::id).shouldContainExactly(
            MediaId(TYPE_TRACKS, CATEGORY_ALL, 481)
        )
    }

    @Test
    fun `Given query matching both album and song, when searching albums then return only that album`() =
        runTest {
            val tree = givenRealisticBrowserTree()

            val results = tree.search(SearchQuery.Album("Avenged Sevenfold", "Nightmare"))
            extracting(results, MediaContent::id).shouldContainExactly(
                MediaId(TYPE_ALBUMS, "6")
            )
        }

    @Test
    fun `Given query matching both album and song, when searching unfocused then return both`() =
        runTest {
            val tree = givenRealisticBrowserTree()

            // Both the album "Nightmare" and its eponymous track are listed in search results.
            // Note that the album should be listed first.
            val results = tree.search(SearchQuery.Unspecified("nightmare"))

            extracting(results, MediaContent::id).shouldContainExactly(
                MediaId(TYPE_ALBUMS, "6"),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 75)
            )
        }

    @Test
    fun `Given uppercase query, when searching unfocused then return results`() = runTest {
        val tree = givenRealisticBrowserTree()
        val results = tree.search(SearchQuery.Unspecified("Nightmare"))

        extracting(results, MediaContent::id).shouldContainAll(
            MediaId(TYPE_ALBUMS, "6"),
            MediaId(TYPE_TRACKS, CATEGORY_ALL, 75)
        )
    }

    @Test
    fun `Given pattern query, when searching then return items containing that pattern`() =
        runTest {
            val tracks = listOf(
                Track(
                    id = 23,
                    title = "Another Brick In The Wall",
                    artistId = 2,
                    artist = "Pink Floyd",
                    albumId = 2,
                    album = "The Wall",
                    duration = 0,
                    discNumber = 1,
                    trackNumber = 5,
                    mediaUri = "",
                    albumArtUri = null,
                    availabilityDate = 0,
                    fileSize = 0,
                    exclusionTime = null
                ),
                Track(
                    id = 34,
                    title = "Another One Bites the Dust",
                    artistId = 3,
                    artist = "Queen",
                    albumId = 3,
                    album = "The Game",
                    duration = 0,
                    discNumber = 1,
                    trackNumber = 3,
                    mediaUri = "",
                    albumArtUri = null,
                    availabilityDate = 0,
                    fileSize = 0,
                    exclusionTime = null,
                ),
                Track(
                    id = 56,
                    title = "Nothing Else Matters",
                    artistId = 4,
                    artist = "Metallica",
                    albumId = 4,
                    album = "Metallica",
                    duration = 0L,
                    discNumber = 1,
                    trackNumber = 8,
                    mediaUri = "",
                    albumArtUri = null,
                    availabilityDate = 0,
                    fileSize = 0,
                    exclusionTime = null
                ),
                Track(
                    id = 12,
                    title = "Otherside",
                    artistId = 1,
                    artist = "Red Hot Chili Peppers",
                    albumId = 1,
                    album = "Californication",
                    duration = 0,
                    discNumber = 1,
                    trackNumber = 6,
                    mediaUri = "",
                    albumArtUri = null,
                    availabilityDate = 0,
                    fileSize = 0,
                    exclusionTime = null,
                ),
                Track(
                    id = 98,
                    title = "You've Got Another Thing Comin",
                    artistId = 7,
                    artist = "Judas Priest",
                    albumId = 7,
                    album = "Screaming for Vengeance",
                    duration = 0,
                    discNumber = 1,
                    trackNumber = 8,
                    mediaUri = "",
                    albumArtUri = null,
                    availabilityDate = 0,
                    fileSize = 0,
                    exclusionTime = null
                )
            )

            val tree = BrowserTree(TestMediaDao(emptyList(), emptyList(), tracks))

            // "OTHERside" is listed first (it starts with the pattern),
            // then "AnOTHER Brick In the Wall" (same pattern at same position),
            // then "AnOTHER One Bites the Dust" (one word contains the pattern but slightly longer),
            // then "You've Got AnOTHER Thing Comin" (pattern matches farther)
            val results = tree.search(SearchQuery.Unspecified("other"))

            extracting(results, MediaContent::id).shouldContainExactly(
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 12),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 23),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 34),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 98)
            )
        }

    @Test
    fun `Given pattern query that matches multiple items equally, when searching then return shortest first`() =
        runTest {
            val tracks = listOf(
                Track(
                    id = 10,
                    title = "Are You Ready",
                    artistId = 32,
                    artist = "AC/DC",
                    albumId = 18,
                    album = "The Razor's Edge",
                    duration = 0,
                    discNumber = 1,
                    trackNumber = 7,
                    mediaUri = "",
                    albumArtUri = null,
                    availabilityDate = 0,
                    fileSize = 0,
                    exclusionTime = null
                ),
                Track(
                    id = 42,
                    title = "Are You Gonna Be My Girl",
                    artistId = 78,
                    artist = "Jet",
                    albumId = 90,
                    album = "Get Born",
                    duration = 0,
                    discNumber = 1,
                    trackNumber = 2,
                    mediaUri = "",
                    albumArtUri = null,
                    availabilityDate = 0,
                    fileSize = 0,
                    exclusionTime = null,
                ),
                Track(
                    id = 63,
                    title = "Are You Gonna Go My Way",
                    artistId = 57,
                    artist = "Lenny Kravitz",
                    albumId = 23,
                    album = "Are You Gonna Go My Way",
                    duration = 0,
                    discNumber = 1,
                    trackNumber = 1,
                    mediaUri = "",
                    albumArtUri = null,
                    availabilityDate = 0,
                    fileSize = 0,
                    exclusionTime = null
                )
            )

            val tree = BrowserTree(TestMediaDao(emptyList(), emptyList(), tracks))

            // When the pattern matches multiple items equally,
            // shorter items should be displayed first.
            val results = tree.search(SearchQuery.Unspecified("are"))

            extracting(results, MediaContent::id).shouldContainExactly(
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 10),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 63),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 42)
            )
        }

    @Test
    fun `When search pattern matches multiple items, then first return results that matches the start of a word`() =
        runTest {
            val tracks = listOf(
                Track(
                    id = 90,
                    title = "Avalanche",
                    artistId = 56,
                    artist = "Ghost",
                    albumId = 97,
                    album = "Prequelle",
                    duration = 0,
                    discNumber = 1,
                    trackNumber = 12,
                    mediaUri = "",
                    albumArtUri = null,
                    availabilityDate = 0,
                    fileSize = 0,
                    exclusionTime = null
                ),
                Track(
                    id = 91,
                    title = "No Grave But The Sea",
                    artistId = 456,
                    artist = "Alestorm",
                    albumId = 856,
                    album = "No Grave But The Sea",
                    duration = 0,
                    discNumber = 1,
                    trackNumber = 1,
                    mediaUri = "",
                    albumArtUri = null,
                    availabilityDate = 0,
                    fileSize = 0,
                    exclusionTime = null
                ),
                Track(
                    id = 356,
                    title = "Gravity",
                    artistId = 45,
                    artist = "Bullet For My Valentine",
                    albumId = 99,
                    album = "Gravity",
                    duration = 0,
                    discNumber = 1,
                    trackNumber = 8,
                    mediaUri = "",
                    albumArtUri = null,
                    availabilityDate = 0,
                    fileSize = 0,
                    exclusionTime = null
                )
            )

            val artist = listOf(
                Artist(65, "Avatar", 0, 0, null),
                Artist(98, "Avenged Sevenfold", 0, 0, null)
            )

            val tree = BrowserTree(TestMediaDao(artist, emptyList(), tracks))
            val results = tree.search(SearchQuery.Unspecified("av"))

            extracting(results, MediaContent::id).shouldContainExactly(
                MediaId(TYPE_ARTISTS, "65"), // AVatar
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 90), // AValanche
                MediaId(TYPE_ARTISTS, "98"), // AVenged Sevenfold
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 356), // GrAVity
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 91) // No GrAVe But the Sea
            )
        }

    private fun BrowserTree(
        mediaDao: MediaDao,
        usageManager: UsageManager = StubUsageManager
    ): BrowserTree =
        BrowserTreeImpl(context, mediaDao, StubPlaylistDao, usageManager, StubSpotifyManager)

    private fun givenRealisticBrowserTree(): BrowserTreeImpl =
        BrowserTreeImpl(
            context,
            TestMediaDao(),
            TestPlaylistDao(),
            TestUsageManager(),
            StubSpotifyManager
        )
}
