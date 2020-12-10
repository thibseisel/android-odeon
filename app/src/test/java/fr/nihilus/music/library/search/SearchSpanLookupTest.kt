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

package fr.nihilus.music.library.search

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ALBUMS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ARTISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_PLAYLISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import io.kotest.assertions.forEachAsClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.data.row
import io.kotest.inspectors.forAll
import io.kotest.matchers.ints.shouldBeExactly
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
internal class SearchSpanLookupTest {

    private val builder = MediaDescriptionCompat.Builder()

    @Test
    fun `It should throw when created with non positive rowSpanCount`() {
        shouldThrow<IllegalArgumentException> { SearchSpanLookup(-1) }
        shouldThrow<IllegalArgumentException> { SearchSpanLookup(0) }
    }

    @Test
    fun `Each track should span a whole row`() {
        ::track.shouldSpan(2)
    }

    @Test
    fun `Each album should span one grid slot`() {
        ::album.shouldSpan(1)
    }

    @Test
    fun `Each artist should span one grid slot`() {
        ::artist.shouldSpan(1)
    }

    @Test
    fun `Each playlist should span a whole row`() {
        ::playlist.shouldSpan(2)
    }

    @Test
    fun `There should be one track per row`() {
        val items = 3.tracks
        val lookup = givenLookup(spanCount = 4, items = items)

        items.indices.toList().forAll { position ->
            lookup.getSpanIndex(position, 4) shouldBeExactly 0
            lookup.getSpanGroupIndex(position, 4) shouldBeExactly position
        }
    }

    @Test
    fun `There should be one playlist per row`() {
        val items = 3.playlists
        val lookup = givenLookup(spanCount = 4, items)

        items.indices.toList().forAll { position ->
            lookup.shouldHaveItemAt(row = position, col = 0, forPosition = position)
        }
    }

    @Test
    fun `There should be one album per grid slot`() {
        val lookup = givenLookup(spanCount = 2, items = 3.albums)

        lookup.shouldHaveItemAt(row = 0, col = 0, forPosition = 0)
        lookup.shouldHaveItemAt(row = 0, col = 1, forPosition = 1)
        lookup.shouldHaveItemAt(row = 1, col = 0, forPosition = 2)
    }

    @Test
    fun `There should be one artist per grid slot`() {
        val lookup = givenLookup(spanCount = 2, items = 3.artists)

        lookup.shouldHaveItemAt(row = 0, col = 0, forPosition = 0)
        lookup.shouldHaveItemAt(row = 0, col = 1, forPosition = 1)
        lookup.shouldHaveItemAt(row = 1, col = 0, forPosition = 2)
    }

    @Test
    fun `Mixed items should spawn a new line when changing type`() {
        val lookup = givenLookup(
            spanCount = 2,
            items = listOf(
                track(1),
                artist(1), artist(2), artist(3),
                album(1), album(2), album(3),
                playlist(1)
            )
        )

        lookup.shouldHaveItemAt(row = 0, col = 0, forPosition = 0)

        lookup.shouldHaveItemAt(row = 1, col = 0, forPosition = 1)
        lookup.shouldHaveItemAt(row = 1, col = 1, forPosition = 2)
        lookup.shouldHaveItemAt(row = 2, col = 0, forPosition = 3)

        lookup.shouldHaveItemAt(row = 3, col = 0, forPosition = 4)
        lookup.shouldHaveItemAt(row = 3, col = 1, forPosition = 5)
        lookup.shouldHaveItemAt(row = 4, col = 0, forPosition = 6)

        lookup.shouldHaveItemAt(row = 5, col = 0, forPosition = 7)
    }

    private fun SearchSpanLookup.shouldHaveItemAt(row: Int, col: Int, forPosition: Int) {
        val spanIndex = getSpanIndex(forPosition, spanCount)
        val groupIndex = getSpanGroupIndex(forPosition, spanCount)

        withClue("Item at position $forPosition should be at [$row, $col], but was at [$groupIndex, $spanIndex]") {
            getSpanIndex(forPosition, spanCount) shouldBeExactly col
            getSpanGroupIndex(forPosition, spanCount) shouldBeExactly row
        }
    }

    private fun givenLookup(
        spanCount: Int,
        items: List<MediaBrowserCompat.MediaItem>
    ) = SearchSpanLookup(spanCount).apply { update(items) }

    private fun ((Long) -> MediaBrowserCompat.MediaItem).shouldSpan(spanSize: Int) {
        val lookup = SearchSpanLookup(spanCount = 2)
        val results = List(3) { this(it.toLong()) }
        lookup.update(results)

        results.indices.forEachAsClue {
            lookup.getSpanSize(it) shouldBeExactly spanSize
        }
    }

    private fun track(id: Long) = mediaItem(
        mediaId = MediaId.encode(TYPE_TRACKS, CATEGORY_ALL, id),
        flags = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
    )

    private fun album(id: Long) = mediaItem(
        mediaId = MediaId.encode(TYPE_ALBUMS, id.toString()),
        flags = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
    )

    private fun artist(id: Long) = mediaItem(
        mediaId = MediaId.encode(TYPE_ARTISTS, id.toString()),
        flags = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
    )

    private fun playlist(id: Long) = mediaItem(
        mediaId = MediaId.encode(TYPE_PLAYLISTS, id.toString()),
        flags = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
    )

    private fun mediaItem(mediaId: String, flags: Int): MediaBrowserCompat.MediaItem {
        val description = builder.setMediaId(mediaId).build()
        return MediaBrowserCompat.MediaItem(description, flags)
    }

    private val Int.tracks get() = List(this) { track(it.toLong()) }

    private val Int.albums get() = List(this) { album(it.toLong()) }

    private val Int.artists get() = List(this) { artist(it.toLong()) }

    private val Int.playlists get() = List(this) { playlist(it.toLong()) }
}