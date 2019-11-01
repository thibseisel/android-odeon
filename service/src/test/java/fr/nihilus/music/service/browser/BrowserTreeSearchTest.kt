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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ALBUMS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ARTISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.core.media.MediaId.Builder.encode
import fr.nihilus.music.media.provider.Artist
import fr.nihilus.music.media.provider.Track
import fr.nihilus.music.media.repo.MediaRepository
import fr.nihilus.music.media.usage.UsageManager
import fr.nihilus.music.service.THEIR_MEDIA_ID
import io.kotlintest.matchers.collections.shouldContainAll
import io.kotlintest.matchers.collections.shouldContainExactly
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.test.runBlockingTest as test

@RunWith(AndroidJUnit4::class)
class BrowserTreeSearchTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `When searching with an empty query then return no results`() = test {
        val tree = givenRealisticBrowserTree()

        val results = tree.search(SearchQuery.Empty)
        assertThat(results).isEmpty()
    }

    @Test
    fun `Given artist focus, when searching an artist then return that artist`() = test {
        val browserTree = givenRealisticBrowserTree()

        val results = browserTree.search(SearchQuery.Artist("Foo Fighters"))
        assertThat(results).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            encode(TYPE_ARTISTS, "13")
        )
    }

    @Test
    fun `Given album focus, when searching an album then return that album`() = test {
        val tree = givenRealisticBrowserTree()

        val results = tree.search(SearchQuery.Album("Foo Fighters", "Wasting Light"))
        assertThat(results).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            encode(TYPE_ALBUMS, "26")
        )
    }

    @Test
    fun `Given track focused query, when searching songs then return that song`() = test {
        val tree = givenRealisticBrowserTree()

        val results = tree.search(SearchQuery.Song(
            "Foo Fighters",
            "Concrete and Gold",
            "Dirty Water"
        ))

        assertThat(results).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            encode(TYPE_TRACKS, CATEGORY_ALL, 481)
        )
    }

    @Test
    fun `Given exact artist name, when searching unfocused then return that artist`() = test {
        val tree = givenRealisticBrowserTree()

            val results = tree.search(SearchQuery.Unspecified("foo fighters"))
        assertThat(results).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            encode(TYPE_ARTISTS, "13")
        )
    }

    @Test
    fun `Given exact album title, when searching unfocused then return that album`() = test {
        val tree = givenRealisticBrowserTree()

            val results = tree.search(SearchQuery.Unspecified("concrete and gold"))
        assertThat(results).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            encode(TYPE_ALBUMS, "102")
        )
    }

    @Test
    fun `Given exact song title, when searching unfocused then return that song`() = test {
        val tree = givenRealisticBrowserTree()

            val results = tree.search(SearchQuery.Unspecified("dirty water"))
        assertThat(results).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            encode(TYPE_TRACKS, CATEGORY_ALL, 481)
        )
    }

    @Test
    fun `Given query matching both album and song, when searching albums then return only that album`() = test {
        val tree = givenRealisticBrowserTree()

            val results = tree.search(SearchQuery.Album("Avenged Sevenfold", "Nightmare"))
        assertThat(results).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            encode(TYPE_ALBUMS, "6")
        )
    }

    @Test
    fun `Given query matching both album and song, when searching unfocused then return both`() = test {
        val tree = givenRealisticBrowserTree()

        // Both the album "Nightmare" and its eponymous track are listed in search results.
        // Note that the album should be listed first.
        val results = tree.search(SearchQuery.Unspecified("nightmare"))

        assertThat(results).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            encode(TYPE_ALBUMS, "6"),
            encode(TYPE_TRACKS, CATEGORY_ALL, 75)
        ).inOrder()
    }

    @Test
    fun `Given uppercase query, when searching unfocused then return results`() = test {
        val tree = givenRealisticBrowserTree()
        val results = tree.search(SearchQuery.Unspecified("Nightmare"))

        results.map { it.mediaId }.shouldContainAll(
            encode(TYPE_ALBUMS, "6"),
            encode(TYPE_TRACKS, CATEGORY_ALL, 75)
        )
    }

    @Test
    fun `Given pattern query, when searching then return items containing that pattern`() = test {
        val tracks = listOf(
            Track(23, "Another Brick In The Wall", "Pink Floyd", "The Wall", 0, 1, 5, "", null, 0, 2, 2, 0),
            Track(34, "Another One Bites the Dust", "Queen", "The Game", 0, 1, 3, "", null, 0, 3, 3, 0),
            Track(56, "Nothing Else Matters", "Metallica", "Metallica", 0L, 1, 8, "", null, 0, 4, 4, 0),
            Track(12, "Otherside", "Red Hot Chili Peppers", "Californication", 0, 1, 6, "", null, 0, 1, 1, 0),
            Track(98, "You've Got Another Thing Comin", "Judas Priest", "Screaming for Vengeance", 0, 1, 8, "", null, 0, 7, 7, 0)
        )

        val tree = BrowserTree(TestMediaRepository(tracks, emptyList(), emptyList()))

        // "OTHERside" is listed first (it starts with the pattern),
        // then "AnOTHER Brick In the Wall" (same pattern at same position),
        // then "AnOTHER One Bites the Dust" (one word contains the pattern but slightly longer),
        // then "You've Got AnOTHER Thing Comin" (pattern matches farther)
        val results = tree.search(SearchQuery.Unspecified("other"))

        assertThat(results).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            encode(TYPE_TRACKS, CATEGORY_ALL, 12),
            encode(TYPE_TRACKS, CATEGORY_ALL, 23),
            encode(TYPE_TRACKS, CATEGORY_ALL, 34),
            encode(TYPE_TRACKS, CATEGORY_ALL, 98)
        ).inOrder()
    }

    @Test
    fun `Given pattern query that matches multiple items equally, when searching then return shortest first`() = test {
        val tracks = listOf(
            Track(10, "Are You Ready", "AC/DC", "The Razor's Edge", 0, 1, 7, "", null, 0, 32, 18, 0),
            Track(42, "Are You Gonna Be My Girl", "Jet", "Get Born", 0, 1, 2, "", null, 0, 78, 90, 0),
            Track(63, "Are You Gonna Go My Way", "Lenny Kravitz", "Are You Gonna Go My Way", 0, 1, 1, "", null, 0, 57, 23, 0)
        )

        val tree = BrowserTree(repository = TestMediaRepository(tracks, emptyList(), emptyList()))

        // When the pattern matches multiple items equally,
        // shorter items should be displayed first.
        val results = tree.search(SearchQuery.Unspecified("are"))

        assertThat(results).comparingElementsUsing(THEIR_MEDIA_ID).containsExactly(
            encode(TYPE_TRACKS, CATEGORY_ALL, 10),
            encode(TYPE_TRACKS, CATEGORY_ALL, 63),
            encode(TYPE_TRACKS, CATEGORY_ALL, 42)
        ).inOrder()
    }

    @Test
    fun `When search pattern matches multiple items, then first return results that matches the start of a word`() = test {
        val tracks = listOf(
            Track(90, "Avalanche", "Ghost", "Prequelle", 0, 1, 12, "", null, 0, 56, 97, 0),
            Track(91, "No Grave But The Sea", "Alestorm", "No Grave But The Sea", 0, 1, 1, "", null, 0, 456, 856, 0),
            Track(356, "Gravity", "Bullet For My Valentine", "Gravity", 0, 1, 8, "", null, 0, 45, 99, 0)
        )

        val artist = listOf(
            Artist(65, "Avatar", 0, 0, null),
            Artist(98, "Avenged Sevenfold", 0, 0, null)
        )

        val tree = BrowserTree(repository = TestMediaRepository(tracks, emptyList(), artist))
        val results = tree.search(SearchQuery.Unspecified("av")).map { it.mediaId }

        results.shouldContainExactly(
            encode(TYPE_ARTISTS, "65"), // AVatar
            encode(TYPE_TRACKS, CATEGORY_ALL, 90), // AValanche
            encode(TYPE_ARTISTS, "98"), // AVenged Sevenfold
            encode(TYPE_TRACKS, CATEGORY_ALL, 356), // GrAVity
            encode(TYPE_TRACKS, CATEGORY_ALL, 91) // No GrAVe But the Sea
        )
    }

    private fun BrowserTree(
        repository: MediaRepository,
        usageManager: UsageManager = StubUsageManager
    ): BrowserTree = BrowserTreeImpl(context, repository, usageManager)

    private fun givenRealisticBrowserTree(): BrowserTreeImpl =
        BrowserTreeImpl(context, TestMediaRepository(), TestUsageManager())
}